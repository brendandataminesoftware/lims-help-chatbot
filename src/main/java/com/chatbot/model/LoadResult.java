package com.chatbot.model;

public class LoadResult {

    private int filesProcessed;
    private int chunksCreated;
    private int errors;
    private String message;

    public LoadResult() {}

    public LoadResult(int filesProcessed, int chunksCreated, int errors, String message) {
        this.filesProcessed = filesProcessed;
        this.chunksCreated = chunksCreated;
        this.errors = errors;
        this.message = message;
    }

    public int getFilesProcessed() {
        return filesProcessed;
    }

    public void setFilesProcessed(int filesProcessed) {
        this.filesProcessed = filesProcessed;
    }

    public int getChunksCreated() {
        return chunksCreated;
    }

    public void setChunksCreated(int chunksCreated) {
        this.chunksCreated = chunksCreated;
    }

    public int getErrors() {
        return errors;
    }

    public void setErrors(int errors) {
        this.errors = errors;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private int filesProcessed;
        private int chunksCreated;
        private int errors;
        private String message;

        public Builder filesProcessed(int filesProcessed) {
            this.filesProcessed = filesProcessed;
            return this;
        }

        public Builder chunksCreated(int chunksCreated) {
            this.chunksCreated = chunksCreated;
            return this;
        }

        public Builder errors(int errors) {
            this.errors = errors;
            return this;
        }

        public Builder message(String message) {
            this.message = message;
            return this;
        }

        public LoadResult build() {
            return new LoadResult(filesProcessed, chunksCreated, errors, message);
        }
    }
}
