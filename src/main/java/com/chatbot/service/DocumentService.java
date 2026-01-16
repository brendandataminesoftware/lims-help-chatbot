package com.chatbot.service;

import com.chatbot.config.RagConfig;
import com.chatbot.model.DocumentInfo;
import com.chatbot.model.LoadResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chroma.ChromaApi;
import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

@Service
public class DocumentService {

    private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

    private final VectorStore vectorStore;
    private final ChromaApi api;
    private final HtmlParserService htmlParserService;
    private final RagConfig ragConfig;
    private final Map<String, DocumentInfo> loadedDocuments = new ConcurrentHashMap<>();

    public DocumentService(VectorStore vectorStore, HtmlParserService htmlParserService, RagConfig ragConfig, ChromaApi api) {
        this.vectorStore = vectorStore;
        this.htmlParserService = htmlParserService;
        this.ragConfig = ragConfig;
        this.api = api;
    }

    public void wipeChromaCollection() {
        try {
            api.deleteCollection("documents");
        } catch (Exception e) {
            // Chroma server often wraps this as a 500 with that message
            if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                // already wiped
                return;
            }
            throw e;
        }
    }

    public LoadResult loadDocumentsFromDirectory(String directoryPath) {
        File directory = new File(directoryPath);
        wipeChromaCollection();
        if (!directory.exists() || !directory.isDirectory()) {
            return LoadResult.builder()
                    .filesProcessed(0)
                    .chunksCreated(0)
                    .errors(1)
                    .message("Directory does not exist: " + directoryPath)
                    .build();
        }

        int filesProcessed = 0;
        int totalChunks = 0;
        int errors = 0;


        try (Stream<Path> paths = Files.walk(directory.toPath())) {
            List<Path> htmlFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".html") || name.endsWith(".htm");
                    })
                    .toList();

            log.info("Found {} HTML files in {}", htmlFiles.size(), directoryPath);

            // Batch all documents for a single vectorStore.add() call
            List<Document> allDocuments = new ArrayList<>();
            List<DocumentInfo> pendingDocInfos = new ArrayList<>();

            for (Path htmlFile : htmlFiles) {
                try {
                    HtmlParserService.ParsedDocument parsed = htmlParserService.parseHtmlFile(htmlFile.toFile());
                    List<String> chunks = chunkText(parsed.content());

                    if (chunks.isEmpty()) {
                        log.warn("No content chunks created for document: {}", parsed.filename());
                        continue;
                    }

                    // Calculate relative path from base directory for URL construction
                    String relativePath = directory.toPath().relativize(htmlFile).toString().replace("\\", "/");

                    String docId = UUID.randomUUID().toString();

                    for (int i = 0; i < chunks.size(); i++) {
                        Map<String, Object> metadata = new HashMap<>();
                        metadata.put("source", relativePath);
                        metadata.put("filePath", parsed.filePath());
                        metadata.put("title", parsed.title());
                        metadata.put("chunk", i + 1);
                        metadata.put("totalChunks", chunks.size());
                        metadata.put("docId", docId);

                        allDocuments.add(new Document(chunks.get(i), metadata));
                    }

                    pendingDocInfos.add(DocumentInfo.builder()
                            .id(docId)
                            .filename(parsed.filename())
                            .filePath(parsed.filePath())
                            .title(parsed.title())
                            .chunkCount(chunks.size())
                            .loadedAt(LocalDateTime.now())
                            .build());

                    filesProcessed++;
                    totalChunks += chunks.size();
                    log.info("Parsed {} -> {} ({} chunks)", htmlFile.getFileName(), relativePath, chunks.size());

                } catch (Exception e) {
                    log.error("Error parsing {}: {}", htmlFile, e.getMessage());
                    errors++;
                }
            }

            // Send documents to vector store in batches of 100
            if (!allDocuments.isEmpty()) {
                final int BATCH_SIZE = 100;

                log.info("Sending {} chunks to vector store in batches of {}...", allDocuments.size(), BATCH_SIZE);

                // Safety: ensure pendingDocInfos aligns with allDocuments
                int docInfoIndex = 0;

                for (int start = 0; start < allDocuments.size(); start += BATCH_SIZE) {
                    int end = Math.min(start + BATCH_SIZE, allDocuments.size());

                    // Sub-list view; copy if your vectorStore implementation mutates the list
                    List<Document> batch = allDocuments.subList(start, end);

                    log.info("Sending batch {}-{} ({} chunks)...", start, end - 1, batch.size());
                    vectorStore.add(batch);

                    // Register only the documents that correspond to this batch after successful add
                    int batchDocInfoEnd = Math.min(docInfoIndex + (end - start), pendingDocInfos.size());
                    for (int i = docInfoIndex; i < batchDocInfoEnd; i++) {
                        DocumentInfo docInfo = pendingDocInfos.get(i);
                        loadedDocuments.put(docInfo.getId(), docInfo);
                    }
                    docInfoIndex = batchDocInfoEnd;
                }

                log.info("Successfully added {} chunks to vector store (registered {} docs)",
                        allDocuments.size(), Math.min(pendingDocInfos.size(), docInfoIndex));
            }

        } catch (IOException e) {
            log.error("Error walking directory: {}", e.getMessage());
            return LoadResult.builder()
                    .filesProcessed(filesProcessed)
                    .chunksCreated(totalChunks)
                    .errors(errors + 1)
                    .message("Error walking directory: " + e.getMessage())
                    .build();
        }

        String message = String.format("Loaded %d files with %d chunks (%d errors)",
                filesProcessed, totalChunks, errors);

        return LoadResult.builder()
                .filesProcessed(filesProcessed)
                .chunksCreated(totalChunks)
                .errors(errors)
                .message(message)
                .build();
    }

    public int loadDocument(File file) throws IOException {
        HtmlParserService.ParsedDocument parsed = htmlParserService.parseHtmlFile(file);
        return loadParsedDocument(parsed);
    }

    public int loadDocumentFromString(String html, String filename) {
        HtmlParserService.ParsedDocument parsed = htmlParserService.parseHtmlString(html, filename);
        return loadParsedDocument(parsed);
    }

    private int loadParsedDocument(HtmlParserService.ParsedDocument parsed) {
        List<String> chunks = chunkText(parsed.content());

        if (chunks.isEmpty()) {
            log.warn("No content chunks created for document: {}", parsed.filename());
            return 0;
        }

        String docId = UUID.randomUUID().toString();

        List<Document> documents = new ArrayList<>();
        for (int i = 0; i < chunks.size(); i++) {
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("source", parsed.filename());
            metadata.put("filePath", parsed.filePath());
            metadata.put("title", parsed.title());
            metadata.put("chunk", i + 1);
            metadata.put("totalChunks", chunks.size());
            metadata.put("docId", docId);

            documents.add(new Document(chunks.get(i), metadata));
        }

        vectorStore.add(documents);

        loadedDocuments.put(docId, DocumentInfo.builder()
                .id(docId)
                .filename(parsed.filename())
                .filePath(parsed.filePath())
                .title(parsed.title())
                .chunkCount(chunks.size())
                .loadedAt(LocalDateTime.now())
                .build());

        return chunks.size();
    }

    private List<String> chunkText(String text) {
        List<String> chunks = new ArrayList<>();

        if (text == null || text.isBlank()) {
            return chunks;
        }

        int chunkSize = ragConfig.getChunkSize();
        int overlap = ragConfig.getChunkOverlap();

        int start = 0;
        while (start < text.length()) {
            int end = Math.min(start + chunkSize, text.length());

            // Try to break at a sentence or word boundary
            if (end < text.length()) {
                int lastPeriod = text.lastIndexOf(". ", end);
                int lastSpace = text.lastIndexOf(" ", end);

                if (lastPeriod > start + chunkSize / 2) {
                    end = lastPeriod + 1;
                } else if (lastSpace > start + chunkSize / 2) {
                    end = lastSpace;
                }
            }

            String chunk = text.substring(start, end).trim();
            if (!chunk.isEmpty()) {
                chunks.add(chunk);
            }

            start = end - overlap;
            if (start >= text.length() - overlap) {
                break;
            }
        }

        return chunks;
    }

    public List<DocumentInfo> getLoadedDocuments() {
        return new ArrayList<>(loadedDocuments.values());
    }

    public DocumentInfo getDocumentByFilename(String filename) {
        return loadedDocuments.values().stream()
                .filter(doc -> doc.getFilename().equals(filename))
                .findFirst()
                .orElse(null);
    }

    public void clearDocuments() {
        loadedDocuments.clear();
        log.info("Cleared document registry. Note: Vector store data persists until ChromaDB is reset.");
    }
}
