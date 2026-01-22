package com.chatbot.repository;

import com.chatbot.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationRepository extends JpaRepository<Conversation, String> {

    List<Conversation> findBySessionIdOrderByUpdatedAtDesc(String sessionId);

    Conversation findByIdAndSessionId(String id, String sessionId);

    void deleteByIdAndSessionId(String id, String sessionId);
}
