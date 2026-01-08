package com.notebooklm.repository;

import com.notebooklm.model.Chat;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;

public interface ChatRepository extends JpaRepository<Chat, UUID> {
    List<Chat> findByUserIdOrderByUpdatedAtDesc(UUID userId);
}
