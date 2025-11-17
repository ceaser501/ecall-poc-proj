package com.ecall.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class IncidentTypeClassificationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    public String classifyIncidentType(String transcript) {
        try {
            // If no OpenAI API key, return null (will use keyword matching fallback)
            if (openaiApiKey == null || openaiApiKey.isEmpty()) {
                log.warn("OpenAI API key not configured, cannot classify incident type with AI");
                return null;
            }

            // Use OpenAI to classify incident type
            String classifiedType = classifyWithAI(transcript);

            if (classifiedType != null && !classifiedType.isEmpty()) {
                return classifiedType;
            }

            return null;

        } catch (Exception e) {
            log.error("Error classifying incident type from transcript: {}", e.getMessage());
            return null;
        }
    }

    private String classifyWithAI(String transcript) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openaiApiKey);

            // Prepare the prompt
            String systemPrompt = "You are an emergency call classifier. Analyze the emergency call transcript and classify it into ONE of these categories: " +
                    "\"emergency\", \"fire\", \"crime\", or \"accident\". " +
                    "Return ONLY the category name without any explanation. " +
                    "Use these guidelines:\n" +
                    "- \"crime\": Violence, theft, assault, weapons, threats, robbery, kidnapping, murder\n" +
                    "- \"fire\": Fire, smoke, burning, flames, explosion\n" +
                    "- \"accident\": Car crash, injuries, falls, bleeding, broken bones\n" +
                    "- \"emergency\": Medical emergencies that don't fit other categories\n" +
                    "If you cannot determine the category, return \"NONE\".";

            String userPrompt = "Classify this emergency call transcript:\n\n" + transcript;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", new Object[]{
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            });
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 20);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode choices = root.get("choices");

                if (choices != null && choices.isArray() && choices.size() > 0) {
                    String content = choices.get(0).get("message").get("content").asText().trim().toLowerCase();

                    // Validate response is one of the expected categories
                    if (content.equals("crime") || content.equals("fire") ||
                        content.equals("accident") || content.equals("emergency")) {
                        log.info("AI classified incident type: {}", content);
                        return content;
                    } else if (!"none".equalsIgnoreCase(content)) {
                        log.warn("AI returned unexpected category: {}", content);
                    }
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Error calling OpenAI API for incident classification: {}", e.getMessage());
            return null;
        }
    }
}
