package com.chatbot.controller;

import com.chatbot.service.CollectionMetadataService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/collections")
public class CollectionController {

    private final CollectionMetadataService collectionMetadataService;

    public CollectionController(CollectionMetadataService collectionMetadataService) {
        this.collectionMetadataService = collectionMetadataService;
    }

    @GetMapping("/{collectionName}/metadata")
    public ResponseEntity<Map<String, Object>> getCollectionMetadata(@PathVariable String collectionName) {
        Map<String, Object> response = new HashMap<>();
        response.put("collectionName", collectionName);

        String title = collectionMetadataService.getTitle(collectionName);
        response.put("title", title);

        String logo = collectionMetadataService.getLogo(collectionName);
        response.put("logo", logo);

        return ResponseEntity.ok(response);
    }
}
