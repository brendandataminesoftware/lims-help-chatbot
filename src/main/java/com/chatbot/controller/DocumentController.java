package com.chatbot.controller;

import com.chatbot.model.DocumentInfo;
import com.chatbot.model.LoadResult;
import com.chatbot.service.DocumentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.File;
import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/documents")
public class DocumentController {

    private static final Logger log = LoggerFactory.getLogger(DocumentController.class);

    private final DocumentService documentService;

    public DocumentController(DocumentService documentService) {
        this.documentService = documentService;
    }

    @GetMapping
    public ResponseEntity<List<DocumentInfo>> getDocuments() {
        return ResponseEntity.ok(documentService.getLoadedDocuments());
    }

    @PostMapping("/upload")
    public ResponseEntity<LoadResult> uploadDocument(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(
                    LoadResult.builder()
                            .message("File is empty")
                            .errors(1)
                            .build()
            );
        }

        String filename = file.getOriginalFilename();
        if (filename == null || (!filename.endsWith(".html") && !filename.endsWith(".htm"))) {
            return ResponseEntity.badRequest().body(
                    LoadResult.builder()
                            .message("Only HTML files are supported")
                            .errors(1)
                            .build()
            );
        }

        try {
            String content = new String(file.getBytes(), StandardCharsets.UTF_8);
            int chunks = documentService.loadDocumentFromString(content, filename);

            return ResponseEntity.ok(LoadResult.builder()
                    .filesProcessed(1)
                    .chunksCreated(chunks)
                    .errors(0)
                    .message("Successfully loaded " + filename + " with " + chunks + " chunks")
                    .build());
        } catch (IOException e) {
            log.error("Error reading uploaded file: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(
                    LoadResult.builder()
                            .message("Error processing file: " + e.getMessage())
                            .errors(1)
                            .build()
            );
        }
    }

    @PostMapping("/load")
    public ResponseEntity<LoadResult> loadFromDirectory(@RequestBody DirectoryRequest request) {
        if (request.path() == null || request.path().isBlank()) {
            return ResponseEntity.badRequest().body(
                    LoadResult.builder()
                            .message("Directory path is required")
                            .errors(1)
                            .build()
            );
        }

        String collectionName = request.collectionName() != null ? request.collectionName() : "documents";
        LoadResult result = documentService.loadDocumentsFromDirectory(request.path(), collectionName);
        return ResponseEntity.ok(result);
    }

    @DeleteMapping
    public ResponseEntity<Void> clearDocuments() {
        documentService.clearDocuments();
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/view/{filename}")
    public ResponseEntity<Resource> viewDocument(@PathVariable String filename) {
        String decodedFilename = URLDecoder.decode(filename, StandardCharsets.UTF_8);
        DocumentInfo docInfo = documentService.getDocumentByFilename(decodedFilename);

        if (docInfo == null || docInfo.getFilePath() == null) {
            return ResponseEntity.notFound().build();
        }

        File file = new File(docInfo.getFilePath());
        if (!file.exists()) {
            return ResponseEntity.notFound().build();
        }

        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE)
                .body(resource);
    }

    public record DirectoryRequest(String path, String collectionName) {}
}
