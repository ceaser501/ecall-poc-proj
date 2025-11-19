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
            String systemPrompt = "You are an emergency call classifier. Analyze the emergency call transcript and classify it into ONE of these categories:\n\n" +
                    "**Category Definitions:**\n" +
                    "1. **disaster**: Fire, explosion, smoke, natural disasters, building collapse, gas leaks, or facility/environmental hazards\n" +
                    "   - Keywords: fire(불,화재), smoke(연기), explosion(폭발), earthquake(지진), collapse(붕괴), flood(홍수), gas leak(가스)\n\n" +
                    "2. **medical**: Cardiac arrest, unconsciousness, severe bleeding, breathing difficulties - situations requiring immediate medical intervention\n" +
                    "   - Keywords: heart attack(심장마비), unconscious(의식없음), bleeding(출혈), breathing(호흡곤란), chest pain(가슴통증)\n\n" +
                    "3. **crime**: Assault, stabbing, shooting, intrusion, threats - crimes posing serious threat to life or physical safety\n" +
                    "   - Keywords: knife(칼), gun(총), assault(폭행), robbery(강도), kidnapping(납치), stabbed(찔림), weapon(무기)\n\n" +
                    "4. **traffic**: Traffic accidents, vehicle vs pedestrian, multi-car collisions, vehicle-related accidents\n" +
                    "   - Keywords: car accident(교통사고), collision(충돌), hit by car(차에 치임), crash(사고)\n" +
                    "   - NOT elevator/building incidents\n\n" +
                    "5. **rescue**: Trapped, buried, drowning, isolated at heights, stuck in elevator/building - situations requiring physical rescue intervention\n" +
                    "   - Keywords: trapped(갇힘), stuck(끼임,갇힘), elevator(엘리베이터,승강기), drowning(익수,물에빠짐), buried(매몰), isolated(고립), locked in(갇혔어요)\n" +
                    "   - IMPORTANT: Elevator emergencies are ALWAYS rescue, NOT traffic\n\n" +
                    "6. **other**: Simple inquiries, false reports, non-emergency situations\n" +
                    "   - Examples: information requests, wrong number, noise complaints, general questions\n\n" +
                    "**Critical Classification Rules:**\n" +
                    "1. Elevator/building trapped situations → ALWAYS rescue (NOT traffic)\n" +
                    "2. Fire/explosion/smoke → disaster (NOT rescue, even if rescue is needed)\n" +
                    "3. Someone being chased with weapons → crime (NOT traffic)\n" +
                    "4. Drowning or water-related → rescue (NOT medical)\n" +
                    "5. Vehicle accidents on roads → traffic (NOT rescue)\n" +
                    "6. Minor issues with no emergency → other\n\n" +
                    "Return ONLY ONE category name: disaster, medical, crime, traffic, rescue, or other";

            String userPrompt = "Classify this emergency call transcript:\n\n" + transcript;

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", "gpt-4o-mini");
            requestBody.put("messages", new Object[]{
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user", "content", userPrompt)
            });
            requestBody.put("temperature", 0.2);
            requestBody.put("max_tokens", 30);

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
                    if (content.equals("disaster") || content.equals("medical") || content.equals("crime") ||
                        content.equals("traffic") || content.equals("rescue") || content.equals("other")) {
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
