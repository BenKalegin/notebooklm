package com.notebooklm.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "chunk_embeddings")
public class ChunkEmbedding {
    @Id
    private UUID chunkId;
    
    private String model;
    private Instant createdAt;

    public ChunkEmbedding() {}

    public ChunkEmbedding(UUID chunkId, String model, Instant createdAt) {
        this.chunkId = chunkId;
        this.model = model;
        this.createdAt = createdAt;
    }

    public UUID getChunkId() { return chunkId; }
    public void setChunkId(UUID chunkId) { this.chunkId = chunkId; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
