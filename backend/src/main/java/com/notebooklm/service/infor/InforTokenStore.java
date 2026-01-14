package com.notebooklm.service.infor;

import com.notebooklm.config.InforGenAIConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

public class InforTokenStore {
    private static final Logger logger = LoggerFactory.getLogger(InforTokenStore.class);

    private final InforGenAIConfig config;
    private final RestTemplate restTemplate;
    private String token;
    private long expiryTime;

    public InforTokenStore(InforGenAIConfig config, RestTemplate restTemplate) {
        this.config = config;
        this.restTemplate = restTemplate;
    }

    public synchronized String getToken() {
        if (token != null && System.currentTimeMillis() < expiryTime) {
            return token;
        }
        return obtainToken();
    }

    private String obtainToken() {
        MultiValueMap<String, String> payload = new LinkedMultiValueMap<>();
        payload.add("grant_type", "password");
        payload.add("username", config.getUsername());
        payload.add("password", config.getPassword());
        payload.add("client_id", config.getClientId());
        payload.add("client_secret", config.getClientSecret());
        payload.add("scope", "gaichatsvc Infor-IDM");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(payload, headers);

        logger.debug("[INFOR GenAI] Requesting token from: {}", config.getTokenUrl());

        ResponseEntity<Map> response = restTemplate.postForEntity(
            config.getTokenUrl(),
            request,
            Map.class
        );

        if (!response.getStatusCode().is2xxSuccessful()) {
            throw new RuntimeException("Failed to obtain Infor GenAI token: " + response.getStatusCode());
        }

        Map<String, Object> data = response.getBody();
        this.token = (String) data.get("access_token");
        Integer expiresIn = (Integer) data.get("expires_in");
        if (expiresIn == null) expiresIn = 7200;
        this.expiryTime = System.currentTimeMillis() + (expiresIn - 60) * 1000L;

        logger.info("[INFOR GenAI] Token obtained successfully");
        return this.token;
    }
}
