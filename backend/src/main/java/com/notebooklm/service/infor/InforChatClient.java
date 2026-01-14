package com.notebooklm.service.infor;

import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.chat.ChatResponse;
import org.springframework.ai.chat.Generation;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.prompt.Prompt;

import java.util.List;
import java.util.Map;

public class InforChatClient implements ChatClient {

    private final InforGenAIClient client;
    private final String modelPrefix;
    public Integer lastTotalTokens;

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

        // Extract token usage
        Map<String, Object> tokenUsage = (Map<String, Object>) response.get("token_usage");
        if (tokenUsage != null) {
            lastTotalTokens = (Integer) tokenUsage.get("total");
        }

        Generation generation = new Generation(content);
        return new ChatResponse(List.of(generation));
    }
}
