package com.chatbot.controller;

import com.chatbot.model.Conversation;
import com.chatbot.service.ConversationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/conversations")
public class ConversationController {

    private final ConversationService conversationService;

    public ConversationController(ConversationService conversationService) {
        this.conversationService = conversationService;
    }

    @GetMapping
    public ResponseEntity<List<ConversationDto>> getConversations(
            HttpServletRequest request,
            HttpServletResponse response) {
        String sessionId = conversationService.getOrCreateSessionId(request, response);
        List<Conversation> conversations = conversationService.getConversations(sessionId);
        List<ConversationDto> dtos = conversations.stream()
                .map(this::toDto)
                .toList();
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/{id}")
    public ResponseEntity<ConversationDto> getConversation(
            @PathVariable String id,
            HttpServletRequest request,
            HttpServletResponse response) {
        String sessionId = conversationService.getOrCreateSessionId(request, response);
        Conversation conversation = conversationService.getConversation(sessionId, id);
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(conversation));
    }

    @PostMapping
    public ResponseEntity<ConversationDto> createConversation(
            HttpServletRequest request,
            HttpServletResponse response) {
        String sessionId = conversationService.getOrCreateSessionId(request, response);
        Conversation conversation = conversationService.createConversation(sessionId);
        return ResponseEntity.ok(toDto(conversation));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ConversationDto> updateConversation(
            @PathVariable String id,
            @RequestBody UpdateConversationRequest updateRequest,
            HttpServletRequest request,
            HttpServletResponse response) {
        String sessionId = conversationService.getOrCreateSessionId(request, response);
        Conversation conversation = conversationService.updateConversation(
                sessionId, id, updateRequest.title(), updateRequest.messagesJson());
        if (conversation == null) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(toDto(conversation));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, Boolean>> deleteConversation(
            @PathVariable String id,
            HttpServletRequest request,
            HttpServletResponse response) {
        String sessionId = conversationService.getOrCreateSessionId(request, response);
        boolean deleted = conversationService.deleteConversation(sessionId, id);
        if (!deleted) {
            return ResponseEntity.notFound().build();
        }
        return ResponseEntity.ok(Map.of("success", true));
    }

    private ConversationDto toDto(Conversation conversation) {
        return new ConversationDto(
                conversation.getId(),
                conversation.getTitle(),
                conversation.getMessagesJson(),
                conversation.getCreatedAt().toEpochMilli(),
                conversation.getUpdatedAt().toEpochMilli()
        );
    }

    public record ConversationDto(
            String id,
            String title,
            String messagesJson,
            long createdAt,
            long updatedAt
    ) {}

    public record UpdateConversationRequest(
            String title,
            String messagesJson
    ) {}
}
