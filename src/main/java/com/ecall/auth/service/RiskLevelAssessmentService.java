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
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class RiskLevelAssessmentService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key}")
    private String openaiApiKey;

    /**
     * Assess risk level from emergency call transcript
     * @param transcript Full transcript of the emergency call
     * @return Map containing "level" (Integer 1-5) and "reason" (String)
     */
    public Map<String, Object> assessRiskLevel(String transcript) {
        try {
            log.info("Assessing risk level for transcript length: {}", transcript != null ? transcript.length() : 0);

            if (transcript == null || transcript.trim().isEmpty()) {
                log.warn("Empty transcript provided for risk assessment");
                return Map.of(
                    "level", 3,
                    "reason", "No transcript available for assessment"
                );
            }

            // Prepare OpenAI API request
            String prompt = buildRiskAssessmentPrompt(transcript);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", List.of(
                Map.of("role", "system", "content", "You are an emergency call risk assessment expert. Analyze the transcript and provide a risk level (1-5) and reason."),
                Map.of("role", "user", "content", prompt)
            ));
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 200);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(openaiApiKey);

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                "https://api.openai.com/v1/chat/completions",
                HttpMethod.POST,
                entity,
                String.class
            );

            // Parse response
            JsonNode root = objectMapper.readTree(response.getBody());
            String content = root.path("choices").get(0).path("message").path("content").asText();

            log.info("OpenAI risk assessment response: {}", content);

            // Parse the response to extract level and reason
            return parseRiskAssessmentResponse(content);

        } catch (Exception e) {
            log.error("Error assessing risk level: {}", e.getMessage(), e);
            return Map.of(
                "level", 3,
                "reason", "Error during risk assessment: " + e.getMessage()
            );
        }
    }

    private String buildRiskAssessmentPrompt(String transcript) {
        return """
            Analyze the following emergency call transcript and assess the risk level on a scale of 1-5:

            - Level 1: Very Low Risk (minor inquiry, non-urgent)
            - Level 2: Low Risk (requires attention but not urgent)
            - Level 3: Moderate Risk (potentially dangerous situation)
            - Level 4: High Risk (serious danger, immediate response needed)
            - Level 5: Critical Risk (life-threatening, multiple casualties)

            Transcript:
            """ + transcript + """

            Respond in the following JSON format:
            {
              "level": <number 1-5>,
              "reason": "<brief explanation in English, 1-2 sentences>"
            }

            Only respond with the JSON, no additional text.
            """;
    }

    private Map<String, Object> parseRiskAssessmentResponse(String content) {
        try {
            // Try to parse as JSON directly
            JsonNode json = objectMapper.readTree(content);
            int level = json.path("level").asInt(3);
            String reason = json.path("reason").asText("Unable to determine risk level");

            // Ensure level is within 1-5 range
            level = Math.max(1, Math.min(5, level));

            log.info("Parsed risk assessment - Level: {}, Reason: {}", level, reason);

            return Map.of(
                "level", level,
                "reason", reason
            );

        } catch (Exception e) {
            log.error("Failed to parse risk assessment response: {}", content, e);

            // Fallback: try to extract level from text
            int level = 3;
            String reason = "Unable to parse risk assessment";

            if (content.contains("\"level\"")) {
                try {
                    String levelStr = content.substring(content.indexOf("\"level\"") + 8);
                    levelStr = levelStr.substring(levelStr.indexOf(":") + 1);
                    levelStr = levelStr.substring(0, levelStr.indexOf(",")).trim();
                    level = Integer.parseInt(levelStr);
                    level = Math.max(1, Math.min(5, level));
                } catch (Exception ex) {
                    log.warn("Failed to extract level from text");
                }
            }

            if (content.contains("\"reason\"")) {
                try {
                    String reasonStr = content.substring(content.indexOf("\"reason\""));
                    reasonStr = reasonStr.substring(reasonStr.indexOf(":") + 1);
                    reasonStr = reasonStr.substring(reasonStr.indexOf("\"") + 1);
                    reasonStr = reasonStr.substring(0, reasonStr.indexOf("\""));
                    reason = reasonStr;
                } catch (Exception ex) {
                    log.warn("Failed to extract reason from text");
                }
            }

            return Map.of(
                "level", level,
                "reason", reason
            );
        }
    }

    /**
     * Result class for risk level assessment
     */
    public static class RiskAssessment {
        private final int level;
        private final String reason;

        public RiskAssessment(int level, String reason) {
            this.level = level;
            this.reason = reason;
        }

        public int getLevel() {
            return level;
        }

        public String getReason() {
            return reason;
        }
    }
}
