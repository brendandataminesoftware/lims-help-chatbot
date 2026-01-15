package com.chatbot.controller;

import com.chatbot.config.RagConfig;
import com.chatbot.model.ChatRequest;
import com.chatbot.model.ChatResponse;
import com.chatbot.service.ChatService;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;

@RestController
@RequestMapping("/api/chat")
public class ChatController {

    private static final Logger log = LoggerFactory.getLogger(ChatController.class);
    private static final String systemPromptFileNAME = "system-prompt.txt";

    private final ChatService chatService;
    private final RagConfig ragConfig;
    private Path systemPromptFile;

    public ChatController(ChatService chatService, RagConfig ragConfig) {
        this.chatService = chatService;
        this.ragConfig = ragConfig;
    }

    @PostConstruct
    public void init() {
        Path dataDir = Paths.get(ragConfig.getDataDir());
        try {
            Files.createDirectories(dataDir);
        } catch (IOException e) {
            log.error("Failed to create data directory: {}", dataDir, e);
        }
        this.systemPromptFile = dataDir.resolve(systemPromptFileNAME);
        log.info("System prompt file path: {}", systemPromptFile.toAbsolutePath());
    }

    @PostMapping
    public ResponseEntity<ChatResponse> chat(@RequestBody ChatRequest request) {
        log.info("Received chat request: {}", request.getMessage());

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return ResponseEntity.badRequest().body(
                    ChatResponse.builder()
                            .message("Message cannot be empty")
                            .build()
            );
        }

        ChatResponse response = chatService.chat(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<String> chatStream(@RequestBody ChatRequest request) {
        log.info("Received streaming chat request: {}", request.getMessage());

        if (request.getMessage() == null || request.getMessage().isBlank()) {
            return Flux.just("Message cannot be empty");
        }

        return chatService.chatStream(request);
    }

    @GetMapping("/system-prompt")
    public ResponseEntity<Map<String, String>> getSystemPrompt() {
        try {
            String prompt;
            if (Files.exists(systemPromptFile)) {
                prompt = Files.readString(systemPromptFile);
                log.debug("Loaded system prompt from file");
            } else {
                prompt = chatService.getDefaultSystemPrompt();
                log.debug("Using default system prompt");
            }
            return ResponseEntity.ok(Map.of(
                "prompt", prompt,
                "isDefault", String.valueOf(!Files.exists(systemPromptFile))
            ));
        } catch (IOException e) {
            log.error("Error reading system prompt file", e);
            return ResponseEntity.ok(Map.of(
                "prompt", chatService.getDefaultSystemPrompt(),
                "isDefault", "true"
            ));
        }
    }

    @PostMapping("/system-prompt")
    public ResponseEntity<Map<String, String>> saveSystemPrompt(@RequestBody Map<String, String> request) {
        String prompt = request.get("prompt");
        if (prompt == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Prompt cannot be null"));
        }

        try {
            Files.writeString(systemPromptFile, prompt);
            log.info("System prompt saved to file");
            return ResponseEntity.ok(Map.of("message", "System prompt saved successfully"));
        } catch (IOException e) {
            log.error("Error saving system prompt file", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to save system prompt"));
        }
    }

    @DeleteMapping("/system-prompt")
    public ResponseEntity<Map<String, String>> resetSystemPrompt() {
        try {
            if (Files.exists(systemPromptFile)) {
                Files.delete(systemPromptFile);
                log.info("System prompt file deleted, reverting to default");
            }
            return ResponseEntity.ok(Map.of(
                "message", "System prompt reset to default",
                "prompt", chatService.getDefaultSystemPrompt()
            ));
        } catch (IOException e) {
            log.error("Error deleting system prompt file", e);
            return ResponseEntity.internalServerError().body(Map.of("error", "Failed to reset system prompt"));
        }
    }
}
