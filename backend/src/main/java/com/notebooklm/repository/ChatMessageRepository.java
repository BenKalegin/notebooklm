package com.notebooklm.repository;

import com.notebooklm.model.ChatMessage;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {
    List<ChatMessage> findByChatIdOrderByCreatedAtAsc(UUID chatId);
}
