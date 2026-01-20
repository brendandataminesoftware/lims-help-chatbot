package com.chatbot.model;

import java.util.List;

public class ChatResponse {

    private String message;
    private List<Source> sources;
    private long processingTimeMs;

    public ChatResponse() {}

    public ChatResponse(String message, List<Source> sources, long processingTimeMs) {
        this.message = message;
        this.sources = sources;
        this.processingTimeMs = processingTimeMs;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public List<Source> getSources() {
        return sources;
    }

    public void setSources(List<Source> sources) {
        this.sources = sources;
    }

    public long getProcessingTimeMs() {
        return processingTimeMs;
    }

    public void setProcessingTimeMs(long processingTimeMs) {
        this.processingTimeMs = processingTimeMs;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String message;
        private List<Source> sources;
        private long processingTimeMs;

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public Builder sources(List<Source> sources) {
            this.sources = sources;
            return this;
        }

        public Builder processingTimeMs(long processingTimeMs) {
            this.processingTimeMs = processingTimeMs;
            return this;
        }

        public ChatResponse build() {
            return new ChatResponse(message, sources, processingTimeMs);
        }
    }
}
