package com.chatbot.service;

import com.chatbot.config.ChromaVectorStoreFactory;
import com.chatbot.config.RagConfig;
import com.chatbot.model.ChatRequest;
import com.chatbot.model.ChatResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private static final String DEFAULT_COLLECTION = "documents";

    private final ChatClient.Builder chatClientBuilder;
    private final VectorStore vectorStore;
    private final ChromaVectorStoreFactory vectorStoreFactory;
    private final RagConfig ragConfig;

    private static final String DEFAULT_SYSTEM_PROMPT = """
            You are an expert support assistant for the CCLAS EL system. You have deep knowledge of CCLAS EL functionality, configuration, troubleshooting, and best practices.

            Instructions:
            - Answer questions based primarily on the provided context documents from the CCLAS EL documentation
            - If the context doesn't contain relevant information, say so clearly and suggest where the user might find help
            - Provide complete and detailed answers with step-by-step instructions when appropriate
            - When describing forms, screens, or processes, list ALL fields that need to be filled out with explanations of what each field is for and any valid values or formats
            - Use CCLAS EL terminology accurately and consistently
            - If you're unsure about something, acknowledge the uncertainty
            - Format your responses using markdown when appropriate (use tables for field listings when helpful)
            - Provide useful suggestions for what the user should do next or related features they might find helpful
            """;

    private static final String CONTEXT_TEMPLATE = """

            Context from documents:
            %s
            """;

    public ChatService(ChatClient.Builder chatClientBuilder, VectorStore vectorStore,
                       ChromaVectorStoreFactory vectorStoreFactory, RagConfig ragConfig) {
        this.chatClientBuilder = chatClientBuilder;
        this.vectorStore = vectorStore;
        this.vectorStoreFactory = vectorStoreFactory;
        this.ragConfig = ragConfig;
    }

    private VectorStore getVectorStore(String collectionName) {
        if (collectionName == null || collectionName.isBlank()) {
            return vectorStore; // Use default configured vector store
        }
        return vectorStoreFactory.getVectorStore(collectionName);
    }

    public String getDefaultSystemPrompt() {
        return DEFAULT_SYSTEM_PROMPT;
    }

    private String buildFullSystemPrompt(String customPrompt, String context) {
        String basePrompt = (customPrompt != null && !customPrompt.isBlank()) ? customPrompt : DEFAULT_SYSTEM_PROMPT;
        return basePrompt + String.format(CONTEXT_TEMPLATE, context);
    }

    public ChatResponse chat(ChatRequest request) {
        long startTime = System.currentTimeMillis();

        // Retrieve relevant documents from the specified collection
        List<Document> relevantDocs = retrieveRelevantDocuments(request.getMessage(), request.getCollectionName());

        // Build context from retrieved documents
        String context = buildContext(relevantDocs);

        // Extract source references and build URLs pointing to locally served docs
        String collectionName = request.getCollectionName();
        String effectiveCollection = (collectionName != null && !collectionName.isBlank())
                ? collectionName : DEFAULT_COLLECTION;

        List<String> sources = relevantDocs.stream()
                .map(doc -> doc.getMetadata().get("source"))
                .filter(source -> source != null)
                .map(Object::toString)
                .distinct()
                .map(source -> "/docs/" + effectiveCollection + "/" + source)
                .collect(Collectors.toList());

        // Build messages list with history
        List<Message> messages = new ArrayList<>();
        String fullSystemPrompt = buildFullSystemPrompt(request.getSystemPrompt(), context);
        messages.add(new SystemMessage(fullSystemPrompt));

        // Add conversation history
        if (request.getHistory() != null) {
            for (ChatRequest.ChatMessage historyMsg : request.getHistory()) {
                if ("user".equalsIgnoreCase(historyMsg.getRole())) {
                    messages.add(new UserMessage(historyMsg.getContent()));
                } else if ("assistant".equalsIgnoreCase(historyMsg.getRole())) {
                    messages.add(new AssistantMessage(historyMsg.getContent()));
                }
            }
        }

        // Add current user message
        messages.add(new UserMessage(request.getMessage()));

        // Generate response
        ChatClient chatClient = chatClientBuilder.build();
        Prompt prompt = new Prompt(messages);

        String response = chatClient.prompt(prompt)
                .call()
                .content();

        long processingTime = System.currentTimeMillis() - startTime;

        log.debug("Chat response generated in {}ms using {} sources", processingTime, sources.size());

        return ChatResponse.builder()
                .message(response)
                .sources(sources)
                .processingTimeMs(processingTime)
                .build();
    }

    public Flux<String> chatStream(ChatRequest request) {
        // Retrieve relevant documents from the specified collection
        List<Document> relevantDocs = retrieveRelevantDocuments(request.getMessage(), request.getCollectionName());

        // Build context from retrieved documents
        String context = buildContext(relevantDocs);

        // Build messages list with history
        List<Message> messages = new ArrayList<>();
        String fullSystemPrompt = buildFullSystemPrompt(request.getSystemPrompt(), context);
        messages.add(new SystemMessage(fullSystemPrompt));

        // Add conversation history
        if (request.getHistory() != null) {
            for (ChatRequest.ChatMessage historyMsg : request.getHistory()) {
                if ("user".equalsIgnoreCase(historyMsg.getRole())) {
                    messages.add(new UserMessage(historyMsg.getContent()));
                } else if ("assistant".equalsIgnoreCase(historyMsg.getRole())) {
                    messages.add(new AssistantMessage(historyMsg.getContent()));
                }
            }
        }

        // Add current user message
        messages.add(new UserMessage(request.getMessage()));

        // Generate streaming response
        ChatClient chatClient = chatClientBuilder.build();
        Prompt prompt = new Prompt(messages);

        return chatClient.prompt(prompt)
                .stream()
                .content();
    }

    private List<Document> retrieveRelevantDocuments(String query, String collectionName) {
        try {
            VectorStore store = getVectorStore(collectionName);
            SearchRequest searchRequest = SearchRequest.query(query).withTopK(ragConfig.getMaxResults());

            List<Document> results = store.similaritySearch(searchRequest);
            log.debug("Retrieved {} relevant documents for query from collection '{}'",
                    results.size(), collectionName != null ? collectionName : "default");
            return results;
        } catch (Exception e) {
            log.warn("Error retrieving documents from collection '{}': {}. Proceeding without context.",
                    collectionName, e.getMessage());
            return List.of();
        }
    }

    private String buildContext(List<Document> documents) {
        if (documents.isEmpty()) {
            return "No relevant documents found.";
        }

        StringBuilder context = new StringBuilder();
        for (int i = 0; i < documents.size(); i++) {
            Document doc = documents.get(i);
            String source = doc.getMetadata().getOrDefault("source", "unknown").toString();
            String title = doc.getMetadata().getOrDefault("title", "").toString();

            context.append(String.format("\n--- Document %d (Source: %s", i + 1, source));
            if (!title.isEmpty()) {
                context.append(String.format(", Title: %s", title));
            }
            context.append(") ---\n");
            context.append(doc.getContent());
            context.append("\n");
        }

        return context.toString();
    }
}
