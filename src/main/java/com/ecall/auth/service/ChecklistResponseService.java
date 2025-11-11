package com.ecall.auth.service;

import com.ecall.auth.config.SupabaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ChecklistResponseService {

    private final SupabaseConfig supabaseConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Save checklist responses for an emergency call
     * @param emergencyCallId Emergency call ID (em-{uuid})
     * @param responses List of checklist responses
     * @return true if successful, false otherwise
     */
    public boolean saveChecklistResponses(String emergencyCallId, List<ChecklistResponse> responses) {
        try {
            // Delete existing responses for this emergency call first
            deleteChecklistResponses(emergencyCallId);

            // Prepare batch insert data
            List<Map<String, Object>> batchData = new ArrayList<>();

            for (int i = 0; i < responses.size(); i++) {
                ChecklistResponse response = responses.get(i);
                String responseId = "cr-" + UUID.randomUUID().toString();

                Map<String, Object> data = new HashMap<>();
                data.put("id", responseId);
                data.put("emergency_call_id", emergencyCallId);
                data.put("question", response.getQuestion());
                data.put("answer", response.getAnswer());
                data.put("question_order", i + 1);

                batchData.add(data);
            }

            // Send batch insert request to Supabase
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            HttpEntity<List<Map<String, Object>>> entity = new HttpEntity<>(batchData, headers);

            String url = supabaseConfig.getApiUrl() + "/checklist_response";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("Checklist responses saved successfully for emergency: {} ({} responses)",
                    emergencyCallId, responses.size());
            return true;

        } catch (Exception e) {
            log.error("Error saving checklist responses: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete existing checklist responses for an emergency call
     * @param emergencyCallId Emergency call ID
     */
    private void deleteChecklistResponses(String emergencyCallId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = supabaseConfig.getApiUrl() + "/checklist_response?emergency_call_id=eq." + emergencyCallId;

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );

            log.info("Deleted existing checklist responses for emergency: {}", emergencyCallId);

        } catch (Exception e) {
            // Log but don't fail - might be first time saving
            log.debug("No existing checklist responses to delete for emergency: {}", emergencyCallId);
        }
    }

    /**
     * Get checklist responses for an emergency call
     * @param emergencyCallId Emergency call ID
     * @return List of checklist responses
     */
    public List<ChecklistResponse> getChecklistResponses(String emergencyCallId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = supabaseConfig.getApiUrl() + "/checklist_response?emergency_call_id=eq." + emergencyCallId + "&order=question_order.asc";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Parse response
            List<Map<String, Object>> jsonArray = objectMapper.readValue(response.getBody(), List.class);
            List<ChecklistResponse> responses = new ArrayList<>();

            for (Map<String, Object> item : jsonArray) {
                ChecklistResponse checklistResponse = new ChecklistResponse();
                checklistResponse.setQuestion((String) item.get("question"));
                checklistResponse.setAnswer((String) item.get("answer"));
                responses.add(checklistResponse);
            }

            log.info("Retrieved {} checklist responses for emergency: {}", responses.size(), emergencyCallId);
            return responses;

        } catch (Exception e) {
            log.error("Error getting checklist responses: {}", e.getMessage(), e);
            return new ArrayList<>();
        }
    }

    /**
     * Inner class for checklist response data
     */
    public static class ChecklistResponse {
        private String question;
        private String answer;

        public ChecklistResponse() {}

        public ChecklistResponse(String question, String answer) {
            this.question = question;
            this.answer = answer;
        }

        public String getQuestion() {
            return question;
        }

        public void setQuestion(String question) {
            this.question = question;
        }

        public String getAnswer() {
            return answer;
        }

        public void setAnswer(String answer) {
            this.answer = answer;
        }
    }
}
