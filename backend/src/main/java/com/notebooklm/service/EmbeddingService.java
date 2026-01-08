package com.notebooklm.service;

import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
public class EmbeddingService {

    private final EmbeddingClient embeddingModel;

    public EmbeddingService(EmbeddingClient embeddingModel) {
        this.embeddingModel = embeddingModel;
    }

    public List<Double> embed(String text) {
        return embeddingModel.embed(text);
    }
    
    public String embedToString(String text) {
        List<Double> embedding = embed(text);
        return embedding.toString();
    }
}
