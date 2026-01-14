package com.notebooklm.service.infor;

import com.notebooklm.config.InforGenAIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.*;

public class InforGenAIClient {
    private static final Logger logger = LoggerFactory.getLogger(InforGenAIClient.class);

    private final InforGenAIConfig config;
    private final RestTemplate restTemplate;
    private final InforTokenStore tokenStore;
    private final String apiPrefix;
    private List<Map<String, Object>> modelsCache;

    public InforGenAIClient(InforGenAIConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
        this.tokenStore = new InforTokenStore(config, restTemplate);
        this.apiPrefix = config.getApiUrl() + "/" + config.getTenantId() + "/GENAI/chatsvc/api/v1";

        logger.info("[INFOR GenAI] Initialized client with API prefix: {}", apiPrefix);

        if (config.getTenantId() == null || config.getApiUrl() == null) {
            throw new IllegalArgumentException("INFOR_TENANT_ID and INFOR_API_URL must be set");
        }
    }

    private HttpHeaders getHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
        headers.set("x-infor-logicalidprefix", "lid://infor.ionapi");
        return headers;
    }

    private HttpHeaders getHeadersWithAuth() {
        HttpHeaders headers = getHeaders();
        headers.setBearerAuth(tokenStore.getToken());
        return headers;
    }

    public List<Map<String, Object>> getModels() {
        if (modelsCache != null) {
            return modelsCache;
        }

        String url = apiPrefix + "/models";
        HttpEntity<Void> request = new HttpEntity<>(getHeadersWithAuth());

        ResponseEntity<List> response = restTemplate.exchange(
            url,
            HttpMethod.GET,
            request,
            List.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to get models: " + response.getStatusCode());
        }

        modelsCache = response.getBody();
        return modelsCache;
    }

    public ModelInfo resolveModel(String modelPrefix) {
        List<Map<String, Object>> models = getModels();

        for (Map<String, Object> model : models) {
            String modelName = (String) model.get("name");
            List<Map<String, String>> versions = (List<Map<String, String>>) model.get("versions");

            if (versions != null) {
                for (Map<String, String> version : versions) {
                    String versionId = version.get("id");
                    if (versionId != null && versionId.equalsIgnoreCase(modelPrefix)) {
                        return new ModelInfo(modelName, versionId);
                    }
                }
            }
        }

        logger.error("Model with prefix '{}' not found. Available models:", modelPrefix);
        for (Map<String, Object> model : models) {
            List<Map<String, String>> versions = (List<Map<String, String>>) model.get("versions");
            if (versions != null) {
                for (Map<String, String> version : versions) {
                    logger.error("  - {}", version.get("id"));
                }
            }
        }
        throw new IllegalArgumentException("Model with prefix '" + modelPrefix + "' not found");
    }

    public Map<String, Object> generate(String prompt, String modelPrefix, double temperature, int maxTokens) {
        ModelInfo modelInfo = resolveModel(modelPrefix);

        Map<String, Object> config = new HashMap<>();
        config.put("temperature", temperature);
        config.put("max_tokens", maxTokens);

        Map<String, Object> payload = new HashMap<>();
        payload.put("prompt", prompt);
        payload.put("model", modelInfo.name);
        payload.put("version", modelInfo.versionId);
        payload.put("config", config);

        String url = apiPrefix + "/prompt/";
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(payload, getHeadersWithAuth());

        logger.debug("[INFOR GenAI] Calling generate with model: {} ({})", modelInfo.name, modelInfo.versionId);

        ResponseEntity<Map> response = restTemplate.postForEntity(url, request, Map.class);

        if (!response.getStatusCode().is2xxSuccessful()) {
            logger.error("[INFOR GenAI] HTTP {}", response.getStatusCode());
            throw new RuntimeException("Failed to generate: " + response.getStatusCode());
        }

        Map<String, Object> result = response.getBody();

        // Log token usage
        Map<String, Object> usage = (Map<String, Object>) result.get("usage");
        if (usage != null) {
            logger.info("[INFOR GenAI] model={} prompt_tokens={} completion_tokens={} total_tokens={}",
                modelInfo.versionId,
                usage.get("prompt_tokens"),
                usage.get("completion_tokens"),
                usage.get("total_tokens")
            );
        }

        return result;
    }

    public static record ModelInfo(String name, String versionId) {}
}
