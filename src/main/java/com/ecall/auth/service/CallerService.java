package com.ecall.auth.service;

import com.ecall.auth.config.SupabaseConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CallerService {

    private final SupabaseConfig supabaseConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Insert a new caller record into the database
     * @param phoneNumber Caller's phone number
     * @param name Caller's name (can be null)
     * @return caller_id (cl-{uuid})
     */
    public String insertCaller(String phoneNumber, String name) {
        return insertCaller(phoneNumber, name, null, null);
    }

    /**
     * Insert a new caller record into the database with full details
     * @param phoneNumber Caller's phone number
     * @param name Caller's name (can be null)
     * @param age Caller's age (can be null)
     * @param gender Caller's gender (can be null)
     * @return caller_id (cl-{uuid})
     */
    public String insertCaller(String phoneNumber, String name, Integer age, String gender) {
        try {
            // Generate custom ID: cl-{uuid}
            String callerId = "cl-" + UUID.randomUUID().toString();

            // Create caller data
            Map<String, Object> callerData = new HashMap<>();
            callerData.put("id", callerId);
            callerData.put("phone_number", phoneNumber);
            if (name != null && !name.isEmpty()) {
                callerData.put("name", name);
            }
            if (age != null) {
                callerData.put("age", age);
            }
            if (gender != null && !gender.isEmpty()) {
                callerData.put("gender", gender);
            }

            // Send request to Supabase
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(callerData, headers);

            String url = supabaseConfig.getApiUrl() + "/caller";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("Caller inserted successfully: {} (phone: {}, name: {}, age: {}, gender: {})",
                    callerId, phoneNumber, name, age, gender);
            return callerId;

        } catch (Exception e) {
            log.error("Error inserting caller: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to insert caller: " + e.getMessage(), e);
        }
    }

    /**
     * Get caller by phone number
     * @param phoneNumber Caller's phone number
     * @return caller_id if found, null otherwise
     */
    public String getCallerByPhoneNumber(String phoneNumber) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/caller?phone_number=eq." + phoneNumber;

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Parse response
            JsonNode jsonArray = objectMapper.readTree(response.getBody());

            if (!jsonArray.isEmpty()) {
                String callerId = jsonArray.get(0).get("id").asText();
                log.info("Found existing caller: {} (phone: {})", callerId, phoneNumber);
                return callerId;
            }

            return null;

        } catch (Exception e) {
            log.error("Error getting caller by phone number: {}", e.getMessage(), e);
            return null;
        }
    }

    /**
     * Get or create a caller record
     * @param phoneNumber Caller's phone number
     * @param name Caller's name (can be null)
     * @return caller_id
     */
    public String getOrCreateCaller(String phoneNumber, String name) {
        // Try to find existing caller
        String callerId = getCallerByPhoneNumber(phoneNumber);

        if (callerId != null) {
            log.info("Using existing caller: {}", callerId);
            return callerId;
        }

        // Create new caller
        log.info("Creating new caller for phone: {}", phoneNumber);
        return insertCaller(phoneNumber, name);
    }

    /**
     * Update caller's name
     * @param callerId Caller ID
     * @param name Caller's name
     */
    public void updateCallerName(String callerId, String name) {
        try {
            if (name == null || name.isEmpty()) {
                log.warn("Cannot update caller name with empty value");
                return;
            }

            Map<String, Object> updates = new HashMap<>();
            updates.put("name", name);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updates, headers);

            String url = supabaseConfig.getApiUrl() + "/caller?id=eq." + callerId;

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    String.class
            );

            log.info("Caller name updated successfully: {} -> {}", callerId, name);

        } catch (Exception e) {
            log.error("Error updating caller name: {}", e.getMessage(), e);
        }
    }

    /**
     * Extract caller name from transcript using AI
     * @param transcript The conversation transcript
     * @return Extracted name or null
     */
    public String extractNameFromTranscript(String transcript) {
        // Simple pattern matching for Korean names
        // Look for patterns like "제 이름은 홍길동입니다", "저는 김철수입니다", "이름이 박영희예요" etc.

        try {
            // Pattern 1: "이름은 XXX" or "이름이 XXX"
            java.util.regex.Pattern pattern1 = java.util.regex.Pattern.compile(
                "(?:제|저의|내)\\s*이름[은이]\\s*([가-힣]{2,4})(?:[이예입]|\\s|$)"
            );
            java.util.regex.Matcher matcher1 = pattern1.matcher(transcript);
            if (matcher1.find()) {
                String name = matcher1.group(1).trim();
                log.info("Extracted name from transcript (pattern 1): {}", name);
                return name;
            }

            // Pattern 2: "저는 XXX입니다" or "나는 XXX예요"
            java.util.regex.Pattern pattern2 = java.util.regex.Pattern.compile(
                "(?:저|나)[는]\\s*([가-힣]{2,4})(?:[이예입]|\\s|$)"
            );
            java.util.regex.Matcher matcher2 = pattern2.matcher(transcript);
            if (matcher2.find()) {
                String name = matcher2.group(1).trim();
                // Filter out common words that are not names
                if (!name.matches("여기|거기|저기|어디|지금|아까")) {
                    log.info("Extracted name from transcript (pattern 2): {}", name);
                    return name;
                }
            }

            log.debug("No name found in transcript");
            return null;

        } catch (Exception e) {
            log.error("Error extracting name from transcript: {}", e.getMessage());
            return null;
        }
    }
}
