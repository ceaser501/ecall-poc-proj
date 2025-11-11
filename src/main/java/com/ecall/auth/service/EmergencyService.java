package com.ecall.auth.service;

import com.ecall.auth.config.SupabaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.OffsetDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyService {

    private final SupabaseConfig supabaseConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Insert a new emergency call record
     * @param callerId Caller ID (cl-{uuid})
     * @param operatorId Operator ID (op-{uuid})
     * @param sttEngine STT engine used (e.g., "clova")
     * @param language Detected language
     * @return emergency_id (em-{uuid})
     */
    public String insertEmergency(String callerId, String operatorId, String sttEngine, String language) {
        try {
            // Generate custom ID: em-{uuid}
            String emergencyId = "em-" + UUID.randomUUID().toString();

            // Create emergency data
            Map<String, Object> emergencyData = new HashMap<>();
            emergencyData.put("id", emergencyId);
            emergencyData.put("caller_id", callerId);
            emergencyData.put("operator_id", operatorId);
            emergencyData.put("call_started_at", OffsetDateTime.now().toString());
            emergencyData.put("stt_engine", sttEngine);
            emergencyData.put("language", language);
            emergencyData.put("status", "in_progress"); // Initial status

            // Send request to Supabase
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emergencyData, headers);

            String url = supabaseConfig.getApiUrl() + "/emergency";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("Emergency call inserted successfully: {} (caller: {}, operator: {})",
                    emergencyId, callerId, operatorId);
            return emergencyId;

        } catch (Exception e) {
            log.error("Error inserting emergency call: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to insert emergency call: " + e.getMessage(), e);
        }
    }

    /**
     * Update emergency call with transcription results
     * @param emergencyId Emergency ID to update
     * @param updates Map of fields to update
     */
    public void updateEmergency(String emergencyId, Map<String, Object> updates) {
        try {
            // Add updated_at timestamp
            updates.put("updated_at", OffsetDateTime.now().toString());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(updates, headers);

            String url = supabaseConfig.getApiUrl() + "/emergency?id=eq." + emergencyId;

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.PATCH,
                    entity,
                    String.class
            );

            log.info("Emergency call updated successfully: {}", emergencyId);

        } catch (Exception e) {
            log.error("Error updating emergency call: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to update emergency call: " + e.getMessage(), e);
        }
    }

    /**
     * Update emergency call with analysis results (location, type, etc.)
     * @param emergencyId Emergency ID
     * @param incidentType Incident type (emergency, fire, crime, accident)
     * @param location Location description
     * @param latitude Latitude coordinate
     * @param longitude Longitude coordinate
     * @param roadAddress Road address
     */
    public void updateEmergencyWithAnalysis(String emergencyId, String incidentType,
                                           String location, Double latitude, Double longitude,
                                           String roadAddress) {
        Map<String, Object> updates = new HashMap<>();

        if (incidentType != null && !incidentType.isEmpty()) {
            updates.put("type", incidentType);
        }
        if (location != null && !location.isEmpty()) {
            updates.put("caller_location", location);
        }
        if (latitude != null) {
            updates.put("latitude", latitude);
        }
        if (longitude != null) {
            updates.put("longitude", longitude);
        }
        if (roadAddress != null && !roadAddress.isEmpty()) {
            updates.put("road_address", roadAddress);
        }

        if (!updates.isEmpty()) {
            updateEmergency(emergencyId, updates);
        }
    }

    /**
     * Complete an emergency call
     * @param emergencyId Emergency ID
     * @param totalDurationMs Total call duration in milliseconds
     * @param speakersCount Number of speakers detected
     * @param utterancesCount Total number of utterances
     */
    public void completeEmergency(String emergencyId, Integer totalDurationMs,
                                  Integer speakersCount, Integer utterancesCount) {
        Map<String, Object> updates = new HashMap<>();
        updates.put("call_ended_at", OffsetDateTime.now().toString());
        updates.put("status", "completed");

        if (totalDurationMs != null) {
            updates.put("total_duration_ms", totalDurationMs);
        }
        if (speakersCount != null) {
            updates.put("speakers_count", speakersCount);
        }
        if (utterancesCount != null) {
            updates.put("utterances_count", utterancesCount);
        }

        updateEmergency(emergencyId, updates);
    }

    /**
     * Insert emergency call with all data after speech recognition is complete
     * @param callerId Caller ID
     * @param operatorId Operator ID
     * @param sttEngine STT engine used
     * @param language Detected language
     * @param totalDurationMs Total call duration in milliseconds
     * @param speakersCount Number of speakers detected
     * @param utterancesCount Total number of utterances
     * @param riskLevel Risk level assessment
     * @param riskLevelReason Risk level reason
     * @return emergency_id
     */
    public String insertEmergencyComplete(String callerId, String operatorId, String sttEngine,
                                         String language, Integer totalDurationMs, Integer speakersCount,
                                         Integer utterancesCount, String riskLevel, String riskLevelReason) {
        try {
            // Generate custom ID: em-{uuid}
            String emergencyId = "em-" + UUID.randomUUID().toString();

            // Create emergency data
            Map<String, Object> emergencyData = new HashMap<>();
            emergencyData.put("id", emergencyId);
            emergencyData.put("caller_id", callerId);
            emergencyData.put("operator_id", operatorId);
            emergencyData.put("call_started_at", OffsetDateTime.now().toString());
            emergencyData.put("call_ended_at", OffsetDateTime.now().toString());
            emergencyData.put("stt_engine", sttEngine);
            emergencyData.put("language", language);
            emergencyData.put("status", "completed");

            if (totalDurationMs != null) {
                emergencyData.put("total_duration_ms", totalDurationMs);
            }
            if (speakersCount != null) {
                emergencyData.put("speakers_count", speakersCount);
            }
            if (utterancesCount != null) {
                emergencyData.put("utterances_count", utterancesCount);
            }
            if (riskLevel != null && !riskLevel.isEmpty()) {
                emergencyData.put("risk_level", riskLevel);
            }
            if (riskLevelReason != null && !riskLevelReason.isEmpty()) {
                emergencyData.put("risk_level_reason", riskLevelReason);
            }

            // Send request to Supabase
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emergencyData, headers);

            String url = supabaseConfig.getApiUrl() + "/emergency";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("Emergency call inserted (complete): {} (caller: {}, operator: {})",
                    emergencyId, callerId, operatorId);
            return emergencyId;

        } catch (Exception e) {
            log.error("Error inserting complete emergency call: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to insert emergency call: " + e.getMessage(), e);
        }
    }

    /**
     * Insert emergency call with all details including location and media
     */
    public String insertEmergencyCompleteWithDetails(
            String callerId, String operatorId, String sttEngine, String language,
            Integer totalDurationMs, Integer speakersCount, Integer utterancesCount,
            String riskLevel, String riskLevelReason,
            String incidentType, String callerLocation, Double latitude, Double longitude,
            String roadAddress, String postalCode, String address1, String address2,
            String mediaAssetId, OffsetDateTime callStartedAt, OffsetDateTime callEndedAt) {
        try {
            // Generate custom ID: em-{uuid}
            String emergencyId = "em-" + UUID.randomUUID().toString();

            // Create emergency data
            Map<String, Object> emergencyData = new HashMap<>();
            emergencyData.put("id", emergencyId);
            emergencyData.put("caller_id", callerId);
            emergencyData.put("operator_id", operatorId);
            emergencyData.put("call_started_at", callStartedAt.toString());
            emergencyData.put("call_ended_at", callEndedAt.toString());
            emergencyData.put("stt_engine", sttEngine);
            emergencyData.put("language", language);
            emergencyData.put("status", "completed");

            if (totalDurationMs != null) {
                emergencyData.put("total_duration_ms", totalDurationMs);
            }
            if (speakersCount != null) {
                emergencyData.put("speakers_count", speakersCount);
            }
            if (utterancesCount != null) {
                emergencyData.put("utterances_count", utterancesCount);
            }
            if (riskLevel != null && !riskLevel.isEmpty()) {
                emergencyData.put("risk_level", riskLevel);
            }
            if (riskLevelReason != null && !riskLevelReason.isEmpty()) {
                emergencyData.put("risk_level_reason", riskLevelReason);
            }
            if (incidentType != null && !incidentType.isEmpty()) {
                emergencyData.put("type", incidentType);
            }
            if (callerLocation != null && !callerLocation.isEmpty()) {
                emergencyData.put("caller_location", callerLocation);
            }
            if (latitude != null) {
                emergencyData.put("latitude", latitude);
            }
            if (longitude != null) {
                emergencyData.put("longitude", longitude);
            }
            if (roadAddress != null && !roadAddress.isEmpty()) {
                emergencyData.put("road_address", roadAddress);
            }
            if (postalCode != null && !postalCode.isEmpty()) {
                emergencyData.put("postal_code", postalCode);
            }
            if (address1 != null && !address1.isEmpty()) {
                emergencyData.put("address1", address1);
            }
            if (address2 != null && !address2.isEmpty()) {
                emergencyData.put("address2", address2);
            }
            if (mediaAssetId != null && !mediaAssetId.isEmpty()) {
                emergencyData.put("audio_id", mediaAssetId);
            }

            // Send request to Supabase
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(emergencyData, headers);

            String url = supabaseConfig.getApiUrl() + "/emergency";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("Emergency call inserted with details: {} (caller: {}, operator: {}, type: {}, location: {})",
                    emergencyId, callerId, operatorId, incidentType, callerLocation);
            return emergencyId;

        } catch (Exception e) {
            log.error("Error inserting emergency call with details: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to insert emergency call: " + e.getMessage(), e);
        }
    }
}
