package com.chatbot.controller;

import com.chatbot.service.CollectionMetadataService;
import com.chatbot.service.DocumentService;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Controller for serving collection documents at /{collectionName}/path/to/file.html
 */
@RestController
public class CollectionDocsController {

    private static final Logger log = LoggerFactory.getLogger(CollectionDocsController.class);

    private final DocumentService documentService;
    private final CollectionMetadataService collectionMetadataService;

    public CollectionDocsController(DocumentService documentService, CollectionMetadataService collectionMetadataService) {
        this.documentService = documentService;
        this.collectionMetadataService = collectionMetadataService;
    }

    /**
     * Serve documents from a collection's docs directory.
     * URL pattern: /docs/{collectionName}/**
     */
    @GetMapping("/docs/{collectionName}/**")
    public ResponseEntity<Resource> serveCollectionDocument(
            @PathVariable String collectionName,
            HttpServletRequest request) {

        // Extract the file path from the request URI
        String requestUri = request.getRequestURI();
        String prefix = "/docs/" + collectionName + "/";
        int prefixIndex = requestUri.indexOf(prefix);

        if (prefixIndex == -1) {
            return ResponseEntity.notFound().build();
        }

        String filePath = requestUri.substring(prefixIndex + prefix.length());
        filePath = URLDecoder.decode(filePath, StandardCharsets.UTF_8);

        // Resolve collection alias if applicable
        String resolvedCollection = collectionMetadataService.resolveCollection(collectionName);

        // Get the collection docs path
        Path collectionDocsPath = documentService.getCollectionDocsPath(resolvedCollection);
        Path targetFile = collectionDocsPath.resolve(filePath).normalize();

        // Security check: ensure the resolved path is within the collection directory
        if (!targetFile.startsWith(collectionDocsPath)) {
            log.warn("Attempted path traversal attack: {}", filePath);
            return ResponseEntity.badRequest().build();
        }

        if (!Files.exists(targetFile) || !Files.isRegularFile(targetFile)) {
            log.debug("File not found: {}", targetFile);
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(targetFile);

        // Determine content type
        String contentType = MediaType.TEXT_HTML_VALUE;
        String fileName = targetFile.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".css")) {
            contentType = "text/css";
        } else if (fileName.endsWith(".js")) {
            contentType = "application/javascript";
        } else if (fileName.endsWith(".png")) {
            contentType = MediaType.IMAGE_PNG_VALUE;
        } else if (fileName.endsWith(".jpg") || fileName.endsWith(".jpeg")) {
            contentType = MediaType.IMAGE_JPEG_VALUE;
        } else if (fileName.endsWith(".gif")) {
            contentType = MediaType.IMAGE_GIF_VALUE;
        } else if (fileName.endsWith(".svg")) {
            contentType = "image/svg+xml";
        } else if (fileName.endsWith(".xml")) {
            contentType = MediaType.APPLICATION_XML_VALUE;
        }

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, contentType)
                .body(resource);
    }
}
