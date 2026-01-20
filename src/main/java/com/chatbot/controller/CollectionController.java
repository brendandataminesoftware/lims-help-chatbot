package com.chatbot.controller;

import com.chatbot.service.CollectionMetadataService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private static final Logger log = LoggerFactory.getLogger(CollectionController.class);

    private final CollectionMetadataService collectionMetadataService;

    public CollectionController(CollectionMetadataService collectionMetadataService) {
        this.collectionMetadataService = collectionMetadataService;
    }

    @GetMapping("/{collectionName}/metadata")
    public ResponseEntity<Map<String, Object>> getCollectionMetadata(@PathVariable String collectionName) {
        log.info("Getting metadata for collection: {}", collectionName);

        Map<String, Object> response = new HashMap<>();
        response.put("collectionName", collectionName);

        // Check if this is an alias and resolve it
        String resolvedCollection = collectionMetadataService.resolveCollection(collectionName);
        log.info("Resolved collection '{}' -> '{}'", collectionName, resolvedCollection);

        response.put("resolvedCollection", resolvedCollection);
        response.put("isAlias", !collectionName.equals(resolvedCollection));

        String title = collectionMetadataService.getTitle(collectionName);
        response.put("title", title);

        String logo = collectionMetadataService.getLogo(collectionName);
        response.put("logo", logo);

        return ResponseEntity.ok(response);
    }
}
