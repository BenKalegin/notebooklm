package com.notebooklm.service.infor;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.metadata.ChatGenerationMetadata;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

public class InforChatClient implements ChatClient {

    private final InforGenAIClient client;
    private final String modelPrefix;

    public InforChatClient(InforGenAIClient client, String modelPrefix) {
        this.client = client;
        this.modelPrefix = modelPrefix;
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        // Combine all messages into a single prompt
        StringBuilder combinedPrompt = new StringBuilder();
        for (Message message : prompt.getInstructions()) {
            combinedPrompt.append(message.getContent()).append("\n\n");
        }

        // Call Infor GenAI
        Map<String, Object> response = client.generate(
            combinedPrompt.toString().trim(),
            modelPrefix,
            0.2,
            4000
        );

        // Extract content
        String content = (String) response.get("content");
        if (content == null) {
            content = response.toString();
        }

        // Create Generation with metadata
        Generation generation = new Generation(content);

        // Add metadata if available
        Map<String, Object> usage = (Map<String, Object>) response.get("usage");
        if (usage != null) {
            generation = new Generation(content, Map.of(
                "prompt_tokens", usage.getOrDefault("prompt_tokens", 0),
                "completion_tokens", usage.getOrDefault("completion_tokens", 0),
                "total_tokens", usage.getOrDefault("total_tokens", 0)
            ));
        }

        return new ChatResponse(List.of(generation));
    }
}
