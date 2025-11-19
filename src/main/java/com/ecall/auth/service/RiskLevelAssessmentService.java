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
            requestBody.put("model", "gpt-4o-mini");
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
            You are an emergency risk assessment expert. Analyze this emergency call transcript and assess the risk level (위험도) on a scale of 1-5.

            **Severity Level Guidelines (위험도 5단계 기준):**

            **Level 5 (최고): Immediate Life Threat (즉시 생명 위협)**
            - Examples: Cardiac arrest (심정지), Active stabbing/shooting (칼부림/총격 현행범), Severe building fire (건물 전소)
            - Keywords: not breathing(호흡없음), cardiac arrest(심정지), unconscious(의식없음), severe bleeding(대량출혈),
                        active weapon use(무기 사용 중), building on fire(건물 화재), explosion(폭발)

            **Level 4 (높음): Severe/Urgent - Can worsen if delayed (중증/긴급 - 지체 시 악화 가능)**
            - Examples: Serious injuries(중상), assault in progress(폭행 진행 중), weapon threats(무기 위협),
                        someone being chased(추격당함), fire with smoke(연기가 있는 화재)
            - Keywords: weapon(무기), knife(칼), gun(총), chase(쫓음), following(뒤따름), serious injury(중상),
                        fire(불), smoke(연기), assault(폭행), threat(위협)
            - Context: Danger is present and active

            **Level 3 (보통): Moderate Accident/Patient (보통 사고/환자 - 급격 악화 징후 적음)**
            - Examples: Minor injuries(경상), minor car accident(경미한 교통사고), mild pain(가벼운 통증)
            - Keywords: minor injury(경미한 부상), small accident(작은 사고), fell down(넘어짐), minor bleeding(가벼운 출혈)
            - Context: Stable situation, no immediate threat

            **Level 2 (낮음): Minor/Confirmation Request (경미/확인 요청 - 현재 위험 거의 없음)**
            - Examples: Property damage(재산 피해), noise complaint(소음 민원), minor dispute(사소한 분쟁)
            - Keywords: noise(소음), parking(주차), complaint(민원), check(확인), information(정보)

            **Level 1 (최저): No Dispatch Needed (출동 불필요 - 기록/이관할 민원성 신고)**
            - Examples: General inquiry(일반 문의), information request(정보 요청), wrong number(오인 신고)
            - Keywords: inquiry(문의), question(질문), information(정보), wrong call(잘못 건 전화)

            **Assessment Rules:**
            1. Look for ACTUAL danger indicators, not just keywords
            2. Consider CONTEXT - Is the threat active or past?
            3. Assess SEVERITY - How immediate is the danger?
            4. If weapon is mentioned + someone is in danger NOW → Level 4-5
            5. If fire/explosion is ACTIVE → Level 4-5
            6. If it's just an inquiry or past event → Level 1-2
            7. BE AGGRESSIVE with weapon/violence/fire scenarios - err on the side of HIGHER levels

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
