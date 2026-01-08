package com.notebooklm.service.dto;

import java.util.List;

public record AssistantResponse(String answer_markdown, List<Citation> citations, String confidence) {
    public record Citation(String document_id, String chunk_id, String quote) {}
}
