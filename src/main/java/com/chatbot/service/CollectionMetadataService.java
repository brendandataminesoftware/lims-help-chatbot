package com.chatbot.service;

import com.chatbot.config.RagConfig;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
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

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CollectionMetadata {
        private String title;
        private String logo;
        private String aliasOf;

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

        public String getAliasOf() {
            return aliasOf;
        }

        public void setAliasOf(String aliasOf) {
            this.aliasOf = aliasOf;
        }

        public boolean isAlias() {
            return aliasOf != null && !aliasOf.isBlank();
        }
    }

    @PostConstruct
    public void init() {
        loadMetadata();
    }

    public void setTitle(String collectionName, String title) {
        Map<String, CollectionMetadata> current = loadMetadataFromFile();
        current.computeIfAbsent(collectionName, k -> new CollectionMetadata()).setTitle(title);
        saveMetadataToFile(current);
        log.info("Set title for collection '{}': {}", collectionName, title);
    }

    public void setLogo(String collectionName, String logo) {
        Map<String, CollectionMetadata> current = loadMetadataFromFile();
        current.computeIfAbsent(collectionName, k -> new CollectionMetadata()).setLogo(logo);
        saveMetadataToFile(current);
        log.info("Set logo for collection '{}': {}", collectionName, logo);
    }

    public void setAlias(String aliasName, String targetCollection) {
        Map<String, CollectionMetadata> current = loadMetadataFromFile();
        CollectionMetadata aliasMeta = current.computeIfAbsent(aliasName, k -> new CollectionMetadata());
        aliasMeta.setAliasOf(targetCollection);
        saveMetadataToFile(current);
        log.info("Set alias '{}' -> '{}'", aliasName, targetCollection);
    }

    public void removeAlias(String aliasName) {
        Map<String, CollectionMetadata> current = loadMetadataFromFile();
        CollectionMetadata meta = current.get(aliasName);
        if (meta != null && meta.isAlias()) {
            current.remove(aliasName);
            saveMetadataToFile(current);
            log.info("Removed alias '{}'", aliasName);
        }
    }

    /**
     * Resolves a collection name, following aliases if present.
     * Returns the actual collection name to use.
     */
    public String resolveCollection(String collectionName) {
        Map<String, CollectionMetadata> allMetadata = loadMetadataFromFile();
        CollectionMetadata meta = allMetadata.get(collectionName);
        if (meta != null && meta.isAlias()) {
            String target = meta.getAliasOf();
            log.info("Resolved alias '{}' -> '{}'", collectionName, target);
            return target;
        }
        return collectionName;
    }

    /**
     * Gets the alias target if the collection is an alias, null otherwise.
     */
    public String getAliasOf(String collectionName) {
        Map<String, CollectionMetadata> allMetadata = loadMetadataFromFile();
        CollectionMetadata meta = allMetadata.get(collectionName);
        return meta != null ? meta.getAliasOf() : null;
    }

    public String getTitle(String collectionName) {
        Map<String, CollectionMetadata> allMetadata = loadMetadataFromFile();
        CollectionMetadata meta = allMetadata.get(collectionName);
        // If it's an alias, get the title from the target collection
        if (meta != null && meta.isAlias()) {
            CollectionMetadata targetMeta = allMetadata.get(meta.getAliasOf());
            return targetMeta != null ? targetMeta.getTitle() : null;
        }
        return meta != null ? meta.getTitle() : null;
    }

    public String getLogo(String collectionName) {
        Map<String, CollectionMetadata> allMetadata = loadMetadataFromFile();
        CollectionMetadata meta = allMetadata.get(collectionName);
        // If it's an alias, get the logo from the target collection
        if (meta != null && meta.isAlias()) {
            CollectionMetadata targetMeta = allMetadata.get(meta.getAliasOf());
            return targetMeta != null ? targetMeta.getLogo() : null;
        }
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
        log.info("Loading metadata from: {}", path.toAbsolutePath());
        if (Files.exists(path)) {
            try {
                Map<String, CollectionMetadata> loaded = objectMapper.readValue(
                        path.toFile(),
                        new TypeReference<Map<String, CollectionMetadata>>() {}
                );
                log.info("Loaded {} collections from file: {}", loaded.size(), loaded.keySet());
                return loaded;
            } catch (IOException e) {
                log.error("Failed to load collection metadata from {}: {}", path, e.getMessage(), e);
            }
        } else {
            log.info("Metadata file does not exist: {}", path.toAbsolutePath());
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

    private void saveMetadataToFile(Map<String, CollectionMetadata> data) {
        Path path = getMetadataPath();
        log.info("Saving {} collections to {}: {}", data.size(), path.toAbsolutePath(), data.keySet());
        try {
            // Ensure parent directory exists
            if (path.getParent() != null) {
                Files.createDirectories(path.getParent());
            }
            objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValue(path.toFile(), data);
            log.info("Successfully saved metadata to {}", path.toAbsolutePath());
        } catch (IOException e) {
            log.error("Failed to save collection metadata to {}: {}", path, e.getMessage(), e);
        }
    }
}
