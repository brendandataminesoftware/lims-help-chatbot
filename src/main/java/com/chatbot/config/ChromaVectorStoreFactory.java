package com.chatbot.config;

import org.springframework.ai.chroma.ChromaApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChromaVectorStoreFactory {

    private final ChromaApi chromaApi;
    private final EmbeddingModel embeddingModel;
    private final Map<String, ChromaVectorStore> vectorStores = new ConcurrentHashMap<>();

    public ChromaVectorStoreFactory(ChromaApi chromaApi, EmbeddingModel embeddingModel) {
        this.chromaApi = chromaApi;
        this.embeddingModel = embeddingModel;
    }

    public ChromaVectorStore getVectorStore(String collectionName) {
        return vectorStores.computeIfAbsent(collectionName, this::createVectorStore);
    }

    private ChromaVectorStore createVectorStore(String collectionName) {
        ChromaVectorStore store = new ChromaVectorStore(embeddingModel, chromaApi, collectionName, true);
        return store;
    }

    public void deleteCollection(String collectionName) {
        try {
            chromaApi.deleteCollection(collectionName);
            vectorStores.remove(collectionName);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                vectorStores.remove(collectionName);
                return;
            }
            throw e;
        }
    }
}
