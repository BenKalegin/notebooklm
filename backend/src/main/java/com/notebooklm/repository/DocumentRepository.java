package com.notebooklm.repository;

import com.notebooklm.model.Document;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.UUID;
import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    List<Document> findByUserId(UUID userId);
    Optional<Document> findByUserIdAndMd5(UUID userId, String md5);
    
    @Query("SELECT d FROM Document d WHERE d.userId = :userId AND d.rawMarkdown LIKE CONCAT('%', :internalId, '%')")
    List<Document> findByUserIdAndInternalId(@Param("userId") UUID userId, @Param("internalId") String internalId);
}
