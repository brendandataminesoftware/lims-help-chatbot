package com.chatbot.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chroma.ChromaApi;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.ChromaVectorStore;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class ChromaVectorStoreFactory {

    private static final Logger log = LoggerFactory.getLogger(ChromaVectorStoreFactory.class);

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
        log.info("Creating vector store for collection: {}", collectionName);
        ChromaVectorStore store = new ChromaVectorStore(embeddingModel, chromaApi, collectionName, true);
        return store;
    }

    /**
     * Ensure a collection exists, creating it if necessary.
     * This is called before loading documents to guarantee the collection is ready.
     */
    public void ensureCollectionExists(String collectionName) {
        log.info("Ensuring collection exists: {}", collectionName);
        // Getting the vector store will create it if it doesn't exist (initializeSchema=true)
        getVectorStore(collectionName);
    }

    /**
     * Delete a collection if it exists, ignoring errors if it doesn't.
     */
    public void deleteCollection(String collectionName) {
        try {
            chromaApi.deleteCollection(collectionName);
            vectorStores.remove(collectionName);
            log.info("Deleted collection: {}", collectionName);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
                vectorStores.remove(collectionName);
                log.debug("Collection {} does not exist, nothing to delete", collectionName);
                return;
            }
            throw e;
        }
    }

    /**
     * Delete and recreate a collection, ensuring it exists and is empty.
     */
    public void recreateCollection(String collectionName) {
        log.info("Recreating collection: {}", collectionName);
        deleteCollection(collectionName);
        ensureCollectionExists(collectionName);
    }
}
