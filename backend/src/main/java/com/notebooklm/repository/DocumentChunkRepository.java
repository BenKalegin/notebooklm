package com.notebooklm.repository;

import com.notebooklm.model.DocumentChunk;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
import java.util.UUID;

public interface DocumentChunkRepository extends JpaRepository<DocumentChunk, UUID> {
    
    @Query(nativeQuery = true, value = """
        SELECT cast(c.id as varchar) as id, 
               cast(c.document_id as varchar) as docId, 
               c.chunk_text, 
               c.heading_path,
               (e.embedding <=> cast(:embedding as vector)) as distance
        FROM document_chunks c
        JOIN chunk_embeddings e ON e.chunk_id = c.id
        WHERE c.user_id = :userId
          AND (:tenantId IS NULL OR cast(c.tenant_id as varchar) = cast(:tenantId as varchar))
        ORDER BY distance ASC
        LIMIT :topK
    """)
    List<Object[]> findSimilarChunks(
        @Param("userId") UUID userId,
        @Param("tenantId") UUID tenantId,
        @Param("embedding") String embedding,
        @Param("topK") int topK
    );
}
