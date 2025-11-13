package com.ecall.auth.service;

import com.ecall.auth.config.SupabaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
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
     * @param emergencyId Emergency ID (em-{uuid})
     * @param callerId Caller ID (cl-{uuid})
     * @param operatorId Operator ID (op-{uuid})
     * @param incidentType Incident type (emergency, fire, crime, accident, etc.)
     * @param responses List of checklist responses
     * @return true if successful, false otherwise
     */
    public boolean saveChecklistResponses(String emergencyId, String callerId, String operatorId,
                                         String incidentType, List<ChecklistResponse> responses) {
        try {
            // Delete existing responses for this emergency call first
            deleteChecklistResponses(emergencyId);

            // Get current timestamp for response_time
            LocalDateTime now = LocalDateTime.now();

            // Prepare batch insert data
            List<Map<String, Object>> batchData = new ArrayList<>();

            for (int i = 0; i < responses.size(); i++) {
                ChecklistResponse response = responses.get(i);
                String responseId = "cr-" + UUID.randomUUID().toString();

                Map<String, Object> data = new HashMap<>();
                data.put("id", responseId);
                data.put("emergency_id", emergencyId);
                data.put("caller_id", callerId);
                data.put("operator_id", operatorId);
                data.put("incident_type", incidentType);
                data.put("question", response.getQuestion());
                data.put("answer", response.getAnswer());
                data.put("question_order", i + 1);
                data.put("response_time", now.toString());
                data.put("is_critical", response.isCritical());

                if (response.getNotes() != null && !response.getNotes().isEmpty()) {
                    data.put("notes", response.getNotes());
                }

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
                    emergencyId, responses.size());
            return true;

        } catch (Exception e) {
            log.error("Error saving checklist responses: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Delete existing checklist responses for an emergency call
     * @param emergencyId Emergency ID
     */
    private void deleteChecklistResponses(String emergencyId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = supabaseConfig.getApiUrl() + "/checklist_response?emergency_id=eq." + emergencyId;

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );

            log.info("Deleted existing checklist responses for emergency: {}", emergencyId);

        } catch (Exception e) {
            // Log but don't fail - might be first time saving
            log.debug("No existing checklist responses to delete for emergency: {}", emergencyId);
        }
    }

    /**
     * Get checklist responses for an emergency call
     * @param emergencyId Emergency ID
     * @return List of checklist responses
     */
    public List<ChecklistResponse> getChecklistResponses(String emergencyId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = supabaseConfig.getApiUrl() + "/checklist_response?emergency_id=eq." + emergencyId + "&order=question_order.asc";

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
                checklistResponse.setCritical(item.get("is_critical") != null && (Boolean) item.get("is_critical"));
                checklistResponse.setNotes((String) item.get("notes"));
                responses.add(checklistResponse);
            }

            log.info("Retrieved {} checklist responses for emergency: {}", responses.size(), emergencyId);
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
        private boolean isCritical;
        private String notes;

        public ChecklistResponse() {}

        public ChecklistResponse(String question, String answer) {
            this.question = question;
            this.answer = answer;
            this.isCritical = false;
        }

        public ChecklistResponse(String question, String answer, boolean isCritical) {
            this.question = question;
            this.answer = answer;
            this.isCritical = isCritical;
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

        public boolean isCritical() {
            return isCritical;
        }

        public void setCritical(boolean critical) {
            isCritical = critical;
        }

        public String getNotes() {
            return notes;
        }

        public void setNotes(String notes) {
            this.notes = notes;
        }
    }
}
