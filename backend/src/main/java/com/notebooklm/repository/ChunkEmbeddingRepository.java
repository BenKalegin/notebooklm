package com.notebooklm.repository;

import com.notebooklm.model.ChunkEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;
import java.util.UUID;

public interface ChunkEmbeddingRepository extends JpaRepository<ChunkEmbedding, UUID> {
    @Modifying
    @Transactional
    @Query(nativeQuery = true, value = "INSERT INTO chunk_embeddings (chunk_id, embedding, model) VALUES (:chunkId, cast(:embedding as vector), :model)")
    void saveEmbedding(@Param("chunkId") UUID chunkId, @Param("embedding") String embedding, @Param("model") String model);
}
