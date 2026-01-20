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
        private String logo;

        public CollectionMetadata() {}

        public CollectionMetadata(String title, String logo) {
            this.title = title;
            this.logo = logo;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getLogo() {
            return logo;
        }

        public void setLogo(String logo) {
            this.logo = logo;
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

    public void setLogo(String collectionName, String logo) {
        metadata.computeIfAbsent(collectionName, k -> new CollectionMetadata()).setLogo(logo);
        saveMetadata();
        log.info("Set logo for collection '{}': {}", collectionName, logo);
    }

    public String getTitle(String collectionName) {
        CollectionMetadata meta = loadMetadataFromFile().get(collectionName);
        return meta != null ? meta.getTitle() : null;
    }

    public String getLogo(String collectionName) {
        CollectionMetadata meta = loadMetadataFromFile().get(collectionName);
        return meta != null ? meta.getLogo() : null;
    }

    public CollectionMetadata getMetadata(String collectionName) {
        return loadMetadataFromFile().get(collectionName);
    }

    private Map<String, CollectionMetadata> loadMetadataFromFile() {
        Path path = Paths.get(METADATA_FILE);
        if (Files.exists(path)) {
            try {
                return objectMapper.readValue(
                        path.toFile(),
                        new TypeReference<Map<String, CollectionMetadata>>() {}
                );
            } catch (IOException e) {
                log.warn("Failed to load collection metadata: {}", e.getMessage());
            }
        }
        return new ConcurrentHashMap<>();
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
