package com.notebooklm.service.infor;

import org.springframework.ai.document.Document;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.ai.embedding.EmbeddingRequest;
import org.springframework.ai.embedding.EmbeddingResponse;
import org.springframework.ai.embedding.Embedding;

import java.util.ArrayList;
import java.util.List;

/**
 * Note: Infor GenAI may not provide embedding endpoints.
 * This is a placeholder that uses Spring AI's default sentence transformer approach.
 * For production, you may need to integrate with a local embedding model or use OpenAI for embeddings only.
 */
public class InforEmbeddingClient implements EmbeddingClient {

    private final EmbeddingClient fallbackClient;

    public InforEmbeddingClient(EmbeddingClient fallbackClient) {
        this.fallbackClient = fallbackClient;
    }

    @Override
    public List<Double> embed(String text) {
        // Delegate to fallback (OpenAI) for embeddings
        // Infor GenAI typically doesn't provide embedding endpoints
        return fallbackClient.embed(text);
    }

    @Override
    public List<Double> embed(Document document) {
        return fallbackClient.embed(document);
    }

    @Override
    public EmbeddingResponse call(EmbeddingRequest request) {
        return fallbackClient.call(request);
    }

    @Override
    public List<List<Double>> embed(List<String> texts) {
        return fallbackClient.embed(texts);
    }
}
