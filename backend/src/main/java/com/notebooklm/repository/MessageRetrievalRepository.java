package com.notebooklm.repository;

import com.notebooklm.model.MessageRetrieval;
import com.notebooklm.model.MessageRetrievalId;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MessageRetrievalRepository extends JpaRepository<MessageRetrieval, MessageRetrievalId> {
}
