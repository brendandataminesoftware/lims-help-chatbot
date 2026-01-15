package com.chatbot.model;

import java.time.LocalDateTime;

public class DocumentInfo {

    private String id;
    private String filename;
    private String filePath;
    private String title;
    private int chunkCount;
    private LocalDateTime loadedAt;

    public DocumentInfo() {}

    public DocumentInfo(String id, String filename, String filePath, String title, int chunkCount, LocalDateTime loadedAt) {
        this.id = id;
        this.filename = filename;
        this.filePath = filePath;
        this.title = title;
        this.chunkCount = chunkCount;
        this.loadedAt = loadedAt;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getFilename() {
        return filename;
    }

    public void setFilename(String filename) {
        this.filename = filename;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getChunkCount() {
        return chunkCount;
    }

    public void setChunkCount(int chunkCount) {
        this.chunkCount = chunkCount;
    }

    public LocalDateTime getLoadedAt() {
        return loadedAt;
    }

    public void setLoadedAt(LocalDateTime loadedAt) {
        this.loadedAt = loadedAt;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String id;
        private String filename;
        private String filePath;
        private String title;
        private int chunkCount;
        private LocalDateTime loadedAt;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder filename(String filename) {
            this.filename = filename;
            return this;
        }

        public Builder filePath(String filePath) {
            this.filePath = filePath;
            return this;
        }

        public Builder title(String title) {
            this.title = title;
            return this;
        }

        public Builder chunkCount(int chunkCount) {
            this.chunkCount = chunkCount;
            return this;
        }

        public Builder loadedAt(LocalDateTime loadedAt) {
            this.loadedAt = loadedAt;
            return this;
        }

        public DocumentInfo build() {
            return new DocumentInfo(id, filename, filePath, title, chunkCount, loadedAt);
        }
    }
}
