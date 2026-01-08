package com.notebooklm.service;

import com.notebooklm.repository.DocumentChunkRepository;
import org.springframework.stereotype.Service;
import java.util.*;

@Service
public class RetrievalService {
    
    private final DocumentChunkRepository chunkRepository;
    private final EmbeddingService embeddingService;
    
    public record RetrievedChunk(UUID chunkId, UUID docId, String chunkText, String headingPath, Double distance) {}

    public RetrievalService(DocumentChunkRepository chunkRepository, EmbeddingService embeddingService) {
        this.chunkRepository = chunkRepository;
        this.embeddingService = embeddingService;
    }

    public List<RetrievedChunk> search(UUID userId, UUID tenantId, String query, int topK) {
        String queryEmbedding = embeddingService.embedToString(query);
        List<Object[]> results = chunkRepository.findSimilarChunks(userId, tenantId, queryEmbedding, topK);
        
        List<RetrievedChunk> retrieved = new ArrayList<>();
        for (Object[] row : results) {
            UUID chunkId = UUID.fromString((String) row[0]);
            UUID docId = UUID.fromString((String) row[1]);
            String text = (String) row[2];
            String heading = (String) row[3];
            Double distance = (Double) row[4];
            
            retrieved.add(new RetrievedChunk(chunkId, docId, text, heading, distance));
        }
        return retrieved;
    }
}
