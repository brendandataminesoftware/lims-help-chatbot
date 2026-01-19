package com.chatbot.model;

import java.util.ArrayList;
import java.util.List;

public class ChatRequest {

    private String message;
    private List<ChatMessage> history = new ArrayList<>();
    private String systemPrompt;
    private String collectionName;

    public ChatRequest() {}

    public ChatRequest(String message, List<ChatMessage> history) {
        this.message = message;
        this.history = history;
    }

    public ChatRequest(String message, List<ChatMessage> history, String systemPrompt) {
        this.message = message;
        this.history = history;
        this.systemPrompt = systemPrompt;
    }

    public ChatRequest(String message, List<ChatMessage> history, String systemPrompt, String collectionName) {
        this.message = message;
        this.history = history;
        this.systemPrompt = systemPrompt;
        this.collectionName = collectionName;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<ChatMessage> getHistory() {
        return history;
    }

    public void setHistory(List<ChatMessage> history) {
        this.history = history;
    }

    public String getSystemPrompt() {
        return systemPrompt;
    }

    public void setSystemPrompt(String systemPrompt) {
        this.systemPrompt = systemPrompt;
    }

    public String getCollectionName() {
        return collectionName;
    }

    public void setCollectionName(String collectionName) {
        this.collectionName = collectionName;
    }

    public static class ChatMessage {
        private String role;
        private String content;

        public ChatMessage() {}

        public ChatMessage(String role, String content) {
            this.role = role;
            this.content = content;
        }

        public String getRole() {
            return role;
        }

        public void setRole(String role) {
            this.role = role;
        }

        public String getContent() {
            return content;
        }

        public void setContent(String content) {
            this.content = content;
        }
    }
}
