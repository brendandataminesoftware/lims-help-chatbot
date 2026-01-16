package com.chatbot.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "rag")
public class RagConfig {

    private int chunkSize = 1000;
    private int chunkOverlap = 200;
    private int maxResults = 5;
    private String dataDir = "./data";
    private String docsBaseUrl = "https://docs.dataminesoftware.com/CCLAS-EL/Latest/";

    public int getChunkSize() {
        return chunkSize;
    }

    public void setChunkSize(int chunkSize) {
        this.chunkSize = chunkSize;
    }

    public int getChunkOverlap() {
        return chunkOverlap;
    }

    public void setChunkOverlap(int chunkOverlap) {
        this.chunkOverlap = chunkOverlap;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public void setMaxResults(int maxResults) {
        this.maxResults = maxResults;
    }

    public String getDataDir() {
        return dataDir;
    }

    public void setDataDir(String dataDir) {
        this.dataDir = dataDir;
    }

    public String getDocsBaseUrl() {
        return docsBaseUrl;
    }

    public void setDocsBaseUrl(String docsBaseUrl) {
        this.docsBaseUrl = docsBaseUrl;
    }
}
