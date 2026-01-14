package com.notebooklm.service.dto;

import java.util.List;

public record AssistantResponse(String answer_markdown, List<Citation> citations, String confidence, Integer total_tokens) {
    public record Citation(String document_id, String chunk_id, String quote) {}
}
