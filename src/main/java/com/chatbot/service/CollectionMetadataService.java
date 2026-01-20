package com.chatbot.service;

import com.chatbot.config.RagConfig;
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

    private final RagConfig ragConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, CollectionMetadata> metadata = new ConcurrentHashMap<>();

    public CollectionMetadataService(RagConfig ragConfig) {
        this.ragConfig = ragConfig;
    }

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

    private Path getMetadataPath() {
        return Paths.get(ragConfig.getMetadataFile());
    }

    private Map<String, CollectionMetadata> loadMetadataFromFile() {
        Path path = getMetadataPath();
        if (Files.exists(path)) {
            try {
                return objectMapper.readValue(
                        path.toFile(),
                        new TypeReference<Map<String, CollectionMetadata>>() {}
                );
            } catch (IOException e) {
                log.warn("Failed to load collection metadata from {}: {}", path, e.getMessage());
            }
        }
        return new ConcurrentHashMap<>();
    }

    private void loadMetadata() {
        Path path = getMetadataPath();
        if (Files.exists(path)) {
            try {
                Map<String, CollectionMetadata> loaded = objectMapper.readValue(
                        path.toFile(),
                        new TypeReference<Map<String, CollectionMetadata>>() {}
                );
                metadata.putAll(loaded);
                log.info("Loaded metadata for {} collections from {}", metadata.size(), path);
            } catch (IOException e) {
                log.warn("Failed to load collection metadata from {}: {}", path, e.getMessage());
            }
        }
    }

    private void saveMetadata() {
        Path path = getMetadataPath();
        try {
            // Ensure parent directory exists
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(path.toFile(), metadata);
        } catch (IOException e) {
            log.error("Failed to save collection metadata to {}: {}", path, e.getMessage());
        }
    }
}
