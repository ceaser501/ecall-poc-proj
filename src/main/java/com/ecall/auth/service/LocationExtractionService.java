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
public class LocationExtractionService {

    private final RestTemplate restTemplate;
    private final AddressService addressService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${openai.api.key:}")
    private String openaiApiKey;

    public String extractLocationFromTranscript(String transcript) {
        try {
            // If no OpenAI API key, fall back to simple extraction
            if (openaiApiKey == null || openaiApiKey.isEmpty()) {
                log.warn("OpenAI API key not configured, using simple extraction");
                return extractSimpleLocation(transcript);
            }

            // Use OpenAI to extract location information
            String extractedLocation = extractWithAI(transcript);

            if (extractedLocation != null && !extractedLocation.isEmpty()) {
                // Verify with Kakao Map API
                String formalAddress = addressService.searchAddress(extractedLocation);
                return formalAddress;
            }

            return null;

        } catch (Exception e) {
            log.error("Error extracting location from transcript: {}", e.getMessage());
            return extractSimpleLocation(transcript);
        }
    }

    private String extractWithAI(String transcript) {
        try {
            String url = "https://api.openai.com/v1/chat/completions";

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + openaiApiKey);

            // Prepare the prompt
            String systemPrompt = "You are a location extraction expert. Extract location information from emergency call transcripts. " +
                    "Return ONLY the location information (address, landmark, or area name) without any explanation. " +
                    "If multiple locations are mentioned, return the most specific one. " +
                    "If no clear location is found, return 'NONE'.";

            String userPrompt = "Extract the location from this emergency call transcript:\n\n" + transcript;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-3.5-turbo");
            requestBody.put("messages", new Object[]{
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            });
            requestBody.put("temperature", 0.3);
            requestBody.put("max_tokens", 100);

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
                    String content = choices.get(0).get("message").get("content").asText().trim();

                    if (!"NONE".equalsIgnoreCase(content)) {
                        log.info("AI extracted location: {}", content);
                        return content;
                    }
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Error calling OpenAI API: {}", e.getMessage());
            return null;
        }
    }

    private String extractSimpleLocation(String text) {
        // Fallback: simple pattern matching for explicit addresses
        // Pattern for Korean address
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                "([가-힣]+(?:시|도)\\s*[가-힣]+(?:구|군)\\s*[가-힣]+(?:동|읍|면)\\s*\\d+[-\\d]*)"
        );
        java.util.regex.Matcher matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        // Pattern for road address
        pattern = java.util.regex.Pattern.compile("([가-힣]+(?:로|대로|길)\\s*\\d+)");
        matcher = pattern.matcher(text);

        if (matcher.find()) {
            return matcher.group(1).trim();
        }

        return null;
    }
}
