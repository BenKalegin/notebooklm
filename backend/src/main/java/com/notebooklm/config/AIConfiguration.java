package com.notebooklm.config;

import com.notebooklm.service.infor.InforChatClient;
import com.notebooklm.service.infor.InforEmbeddingClient;
import com.notebooklm.service.infor.InforGenAIClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.ChatClient;
import org.springframework.ai.embedding.EmbeddingClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.web.client.RestTemplate;

@Configuration
public class AIConfiguration {
    private static final Logger logger = LoggerFactory.getLogger(AIConfiguration.class);

    @Value("${app.ai.provider:openai}")
    private String aiProvider;

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "infor")
    public InforGenAIClient inforGenAIClient(InforGenAIConfig config, RestTemplate restTemplate) {
        logger.info("[AI Configuration] Creating Infor GenAI client");
        return new InforGenAIClient(config, restTemplate);
    }

    @Bean
    @Primary
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "infor")
    public ChatClient inforChatClient(InforGenAIClient inforClient, InforGenAIConfig config) {
        logger.info("[AI Configuration] Using Infor GenAI for chat with model: {}", config.getLlmModel());
        String modelPrefix = config.getLlmModel();
        if (modelPrefix == null || modelPrefix.isEmpty()) {
            modelPrefix = "claude-sonnet-4";
        }
        return new InforChatClient(inforClient, modelPrefix);
    }

    @Bean(name = "inforEmbeddingClient")
    @Primary
    @ConditionalOnProperty(name = "app.ai.provider", havingValue = "infor")
    public EmbeddingClient inforEmbeddingClientWrapper(
            @org.springframework.beans.factory.annotation.Qualifier("openAiEmbeddingClient") EmbeddingClient openAiEmbeddingClient) {
        logger.warn("[AI Configuration] Using OpenAI for embeddings (Infor GenAI doesn't provide embedding endpoints)");
        // Infor GenAI doesn't typically provide embedding endpoints
        // Fall back to OpenAI for embeddings
        return new InforEmbeddingClient(openAiEmbeddingClient);
    }
}
