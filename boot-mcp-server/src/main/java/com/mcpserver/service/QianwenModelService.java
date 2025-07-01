package com.mcpserver.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class QianwenModelService {
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.ai.dash-scope.api-key}")
    private String apiKey;

    public String generateAnswer(String prompt) {
        String url = "https://dashscope.aliyuncs.com/api/v1/services/aigc/text-generation/generation";
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Authorization", "Bearer " + apiKey);

        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("prompt", prompt);
        requestBody.put("max_tokens", 150);

        HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);
        ResponseEntity<Map> responseEntity = restTemplate.postForEntity(url, requestEntity, Map.class);

        Map<String, Object> responseBody = responseEntity.getBody();
        if (responseBody != null && responseBody.containsKey("output")) {
            return responseBody.get("output").toString();
        }
        return "Error generating answer";
    }
} 