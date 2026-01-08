package com.notebooklm.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Entity
@Table(name = "message_retrievals")
@IdClass(MessageRetrievalId.class)
public class MessageRetrieval {
    @Id
    private UUID messageId;
    @Id
    private UUID chunkId;
    
    private Float score;
    private Integer rank;

    public MessageRetrieval() {}

    public MessageRetrieval(UUID messageId, UUID chunkId, Float score, Integer rank) {
        this.messageId = messageId;
        this.chunkId = chunkId;
        this.score = score;
        this.rank = rank;
    }

    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    public UUID getChunkId() { return chunkId; }
    public void setChunkId(UUID chunkId) { this.chunkId = chunkId; }
    public Float getScore() { return score; }
    public void setScore(Float score) { this.score = score; }
    public Integer getRank() { return rank; }
    public void setRank(Integer rank) { this.rank = rank; }
}
