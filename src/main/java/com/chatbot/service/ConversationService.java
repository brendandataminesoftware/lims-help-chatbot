package com.chatbot.service;

import com.chatbot.model.Conversation;
import com.chatbot.repository.ConversationRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class ConversationService {

    private static final String SESSION_COOKIE_NAME = "chat_session";
    private static final int SESSION_COOKIE_MAX_AGE = 60 * 60 * 24 * 365; // 1 year

    private final ConversationRepository conversationRepository;
    private final ObjectMapper objectMapper;

    public ConversationService(ConversationRepository conversationRepository) {
        this.conversationRepository = conversationRepository;
        this.objectMapper = new ObjectMapper();
    }

    public String getOrCreateSessionId(HttpServletRequest request, HttpServletResponse response) {
        // Check for existing session cookie
        if (request.getCookies() != null) {
            for (Cookie cookie : request.getCookies()) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }

        // Create new session ID
        String sessionId = UUID.randomUUID().toString();
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        cookie.setPath("/");
        cookie.setMaxAge(SESSION_COOKIE_MAX_AGE);
        cookie.setHttpOnly(true);
        response.addCookie(cookie);

        return sessionId;
    }

    public List<Conversation> getConversations(String sessionId) {
        return conversationRepository.findBySessionIdOrderByUpdatedAtDesc(sessionId);
    }

    public Conversation getConversation(String sessionId, String conversationId) {
        return conversationRepository.findByIdAndSessionId(conversationId, sessionId);
    }

    public Conversation createConversation(String sessionId) {
        Conversation conversation = new Conversation(sessionId);
        return conversationRepository.save(conversation);
    }

    @Transactional
    public Conversation updateConversation(String sessionId, String conversationId, String title, String messagesJson) {
        Conversation conversation = conversationRepository.findByIdAndSessionId(conversationId, sessionId);
        if (conversation == null) {
            return null;
        }

        if (title != null) {
            conversation.setTitle(title);
        }

        if (messagesJson != null) {
            conversation.setMessagesJson(messagesJson);

            // Auto-generate title from first user message if title is still default
            if ("New Chat".equals(conversation.getTitle())) {
                String autoTitle = generateTitleFromMessages(messagesJson);
                if (autoTitle != null) {
                    conversation.setTitle(autoTitle);
                }
            }
        }

        return conversationRepository.save(conversation);
    }

    @Transactional
    public boolean deleteConversation(String sessionId, String conversationId) {
        Conversation conversation = conversationRepository.findByIdAndSessionId(conversationId, sessionId);
        if (conversation == null) {
            return false;
        }
        conversationRepository.delete(conversation);
        return true;
    }

    private String generateTitleFromMessages(String messagesJson) {
        try {
            JsonNode messages = objectMapper.readTree(messagesJson);
            for (JsonNode message : messages) {
                if ("user".equals(message.get("role").asText())) {
                    String content = message.get("content").asText();
                    if (content.length() > 40) {
                        return content.substring(0, 40) + "...";
                    }
                    return content;
                }
            }
        } catch (JsonProcessingException e) {
            // Ignore parsing errors
        }
        return null;
    }
}
