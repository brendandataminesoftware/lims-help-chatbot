package com.chatbot.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class CollectionMetadataService {

    private static final Logger log = LoggerFactory.getLogger(CollectionMetadataService.class);
    private static final String METADATA_FILE = "collection-metadata.json";

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CollectionMetadata> metadata = new ConcurrentHashMap<>();

    public static class CollectionMetadata {
        private String title;

        public CollectionMetadata() {}

        public CollectionMetadata(String title) {
            this.title = title;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }
    }

    @PostConstruct
    public void init() {
        loadMetadata();
    }

    public void setTitle(String collectionName, String title) {
        metadata.computeIfAbsent(collectionName, k -> new CollectionMetadata()).setTitle(title);
        saveMetadata();
        log.info("Set title for collection '{}': {}", collectionName, title);
    }

    public String getTitle(String collectionName) {
        CollectionMetadata meta = metadata.get(collectionName);
        return meta != null ? meta.getTitle() : null;
    }

    public CollectionMetadata getMetadata(String collectionName) {
        return metadata.get(collectionName);
    }

    private void loadMetadata() {
        Path path = Paths.get(METADATA_FILE);
        if (Files.exists(path)) {
            try {
                Map<String, CollectionMetadata> loaded = objectMapper.readValue(
                        path.toFile(),
                        new TypeReference<Map<String, CollectionMetadata>>() {}
                );
                metadata.putAll(loaded);
                log.info("Loaded metadata for {} collections", metadata.size());
            } catch (IOException e) {
                log.warn("Failed to load collection metadata: {}", e.getMessage());
            }
        }
    }

    private void saveMetadata() {
        try {
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(Paths.get(METADATA_FILE).toFile(), metadata);
        } catch (IOException e) {
            log.error("Failed to save collection metadata: {}", e.getMessage());
        }
    }
}
