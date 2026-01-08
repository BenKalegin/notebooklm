package com.notebooklm.model;

import jakarta.persistence.Embeddable;
import java.io.Serializable;
import java.util.Objects;
import java.util.UUID;

@Embeddable
public class MessageRetrievalId implements Serializable {
    private UUID messageId;
    private UUID chunkId;

    public MessageRetrievalId() {}

    public MessageRetrievalId(UUID messageId, UUID chunkId) {
        this.messageId = messageId;
        this.chunkId = chunkId;
    }

    public UUID getMessageId() { return messageId; }
    public void setMessageId(UUID messageId) { this.messageId = messageId; }
    public UUID getChunkId() { return chunkId; }
    public void setChunkId(UUID chunkId) { this.chunkId = chunkId; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MessageRetrievalId that = (MessageRetrievalId) o;
        return Objects.equals(messageId, that.messageId) && Objects.equals(chunkId, that.chunkId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(messageId, chunkId);
    }
}
