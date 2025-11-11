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
        try {
            // Generate custom ID: cl-{uuid}
            String callerId = "cl-" + UUID.randomUUID().toString();

            // Create caller data
            Map<String, Object> callerData = new HashMap<>();
            callerData.put("id", callerId);
            callerData.put("phone_number", phoneNumber);
            callerData.put("name", name != null ? name : "Unknown");

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

            log.info("Caller inserted successfully: {} (phone: {})", callerId, phoneNumber);
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
}
