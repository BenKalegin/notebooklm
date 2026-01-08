package com.notebooklm.repository;

import com.notebooklm.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUserId(UUID userId);
    Optional<Document> findByUserIdAndMd5(UUID userId, String md5);
}
