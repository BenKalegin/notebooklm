package com.notebooklm.model;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "document_chunks")
public class DocumentChunk {
    @Id
    private UUID id;
    private UUID documentId;
    private UUID userId;
    private UUID tenantId;
    private Integer chunkIndex;
    private String headingPath;
    private Integer startChar;
    private Integer endChar;
    
    @Column(columnDefinition = "TEXT")
    private String chunkText;
    
    private Integer tokenCountEst;
    private Instant createdAt;

    public DocumentChunk() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public UUID getDocumentId() { return documentId; }
    public void setDocumentId(UUID documentId) { this.documentId = documentId; }
    public UUID getUserId() { return userId; }
    public void setUserId(UUID userId) { this.userId = userId; }
    public UUID getTenantId() { return tenantId; }
    public void setTenantId(UUID tenantId) { this.tenantId = tenantId; }
    public Integer getChunkIndex() { return chunkIndex; }
    public void setChunkIndex(Integer chunkIndex) { this.chunkIndex = chunkIndex; }
    public String getHeadingPath() { return headingPath; }
    public void setHeadingPath(String headingPath) { this.headingPath = headingPath; }
    public Integer getStartChar() { return startChar; }
    public void setStartChar(Integer startChar) { this.startChar = startChar; }
    public Integer getEndChar() { return endChar; }
    public void setEndChar(Integer endChar) { this.endChar = endChar; }
    public String getChunkText() { return chunkText; }
    public void setChunkText(String chunkText) { this.chunkText = chunkText; }
    public Integer getTokenCountEst() { return tokenCountEst; }
    public void setTokenCountEst(Integer tokenCountEst) { this.tokenCountEst = tokenCountEst; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
