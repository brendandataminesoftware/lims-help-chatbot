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

        // Create ChromaVectorStore with initializeSchema=true to let it handle collection creation
        ChromaVectorStore store = new ChromaVectorStore(embeddingModel, chromaApi, collectionName, true);

        // Call afterPropertiesSet to trigger collection initialization
        try {
            store.afterPropertiesSet();
            log.info("Initialized vector store for collection: {}", collectionName);
        } catch (Exception e) {
            log.warn("Error during afterPropertiesSet for collection {}: {}", collectionName, e.getMessage());
        }

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
        // Always remove from cache first
        vectorStores.remove(collectionName);

        try {
            chromaApi.deleteCollection(collectionName);
            log.info("Deleted collection: {}", collectionName);
        } catch (Exception e) {
            if (e.getMessage() != null && e.getMessage().contains("does not exist")) {
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

        // Remove from cache and delete from ChromaDB
        deleteCollection(collectionName);

        // Create a fresh vector store instance which will create the collection
        ChromaVectorStore store = createVectorStore(collectionName);
        vectorStores.put(collectionName, store);

        log.info("Collection {} recreated successfully", collectionName);
    }
}
