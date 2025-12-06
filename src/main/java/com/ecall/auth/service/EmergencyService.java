package com.ecall.auth.service;

import com.ecall.auth.config.SupabaseConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDateTime;
import java.util.*;
import com.fasterxml.jackson.core.type.TypeReference;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmergencyService {

    private final SupabaseConfig supabaseConfig;
    private final RestTemplate restTemplate;
    private final WebClient webClient;
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
            emergencyData.put("call_started_at", LocalDateTime.now().toString());
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
     * Delete emergency call record
     * @param emergencyId Emergency ID to delete
     */
    public void deleteEmergency(String emergencyId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = supabaseConfig.getApiUrl() + "/emergency?id=eq." + emergencyId;

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );

            log.info("Emergency call deleted successfully: {}", emergencyId);

        } catch (Exception e) {
            log.error("Error deleting emergency call: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to delete emergency call: " + e.getMessage(), e);
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
            updates.put("updated_at", LocalDateTime.now().toString());

            String result = webClient.patch()
                    .uri("/emergency?id=eq." + emergencyId)
                    .header("Prefer", "return=representation")
                    .header("Content-Type", "application/json")
                    .bodyValue(updates)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

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
        updates.put("call_ended_at", LocalDateTime.now().toString());
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
            emergencyData.put("call_started_at", LocalDateTime.now().toString());
            emergencyData.put("call_ended_at", LocalDateTime.now().toString());
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
     * Insert emergency call with all details including location and media (with custom ID)
     */
    public String insertEmergencyCompleteWithDetailsAndId(
            String emergencyId, String callerId, String operatorId, String sttEngine, String language,
            Integer totalDurationMs, Integer speakersCount, Integer utterancesCount,
            String riskLevel, String riskLevelReason,
            String incidentType, String callerLocation, Double latitude, Double longitude,
            String roadAddress, String postalCode, String address1, String address2,
            String mediaAssetId, LocalDateTime callStartedAt, LocalDateTime callEndedAt) {
        return insertEmergencyWithId(emergencyId, callerId, operatorId, sttEngine, language,
                totalDurationMs, speakersCount, utterancesCount, riskLevel, riskLevelReason,
                incidentType, callerLocation, latitude, longitude, roadAddress, postalCode,
                address1, address2, mediaAssetId, callStartedAt, callEndedAt);
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
            String mediaAssetId, LocalDateTime callStartedAt, LocalDateTime callEndedAt) {
        // Generate custom ID: em-{uuid}
        String emergencyId = "em-" + UUID.randomUUID().toString();
        return insertEmergencyWithId(emergencyId, callerId, operatorId, sttEngine, language,
                totalDurationMs, speakersCount, utterancesCount, riskLevel, riskLevelReason,
                incidentType, callerLocation, latitude, longitude, roadAddress, postalCode,
                address1, address2, mediaAssetId, callStartedAt, callEndedAt);
    }

    /**
     * Internal method to insert emergency with specified ID
     */
    private String insertEmergencyWithId(
            String emergencyId, String callerId, String operatorId, String sttEngine, String language,
            Integer totalDurationMs, Integer speakersCount, Integer utterancesCount,
            String riskLevel, String riskLevelReason,
            String incidentType, String callerLocation, Double latitude, Double longitude,
            String roadAddress, String postalCode, String address1, String address2,
            String mediaAssetId, LocalDateTime callStartedAt, LocalDateTime callEndedAt) {
        try {

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

    /**
     * Get all emergencies with caller and operator details
     */
    public List<Map<String, Object>> getAllEmergencies() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Get all emergencies
            String emergencyUrl = supabaseConfig.getApiUrl() + "/emergency?order=call_started_at.desc";
            ResponseEntity<String> emergencyResponse = restTemplate.exchange(
                    emergencyUrl,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (emergencyResponse.getStatusCode() == HttpStatus.OK && emergencyResponse.getBody() != null) {
                List<Map<String, Object>> emergencies = objectMapper.readValue(
                        emergencyResponse.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                // Get all callers and operators
                Map<String, Map<String, Object>> callersMap = getAllCallers();
                Map<String, Map<String, Object>> operatorsMap = getAllOperators();

                // Manually join the data
                for (Map<String, Object> emergency : emergencies) {
                    String callerId = (String) emergency.get("caller_id");
                    String operatorId = (String) emergency.get("operator_id");

                    if (callerId != null && callersMap.containsKey(callerId)) {
                        emergency.put("caller", callersMap.get(callerId));
                    }

                    if (operatorId != null && operatorsMap.containsKey(operatorId)) {
                        emergency.put("operator", operatorsMap.get(operatorId));
                    }
                }

                log.info("Retrieved {} emergency records", emergencies.size());
                return emergencies;
            }

            return new ArrayList<>();

        } catch (Exception e) {
            log.error("Error fetching emergencies: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch emergencies: " + e.getMessage(), e);
        }
    }

    /**
     * Get all callers as a map (id -> caller data)
     */
    private Map<String, Map<String, Object>> getAllCallers() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = supabaseConfig.getApiUrl() + "/caller";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> callers = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                Map<String, Map<String, Object>> callersMap = new HashMap<>();
                for (Map<String, Object> caller : callers) {
                    callersMap.put((String) caller.get("id"), caller);
                }
                return callersMap;
            }

            return new HashMap<>();
        } catch (Exception e) {
            log.error("Error fetching callers: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get all operators as a map (id -> operator data)
     */
    private Map<String, Map<String, Object>> getAllOperators() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = supabaseConfig.getApiUrl() + "/operator";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> operators = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                Map<String, Map<String, Object>> operatorsMap = new HashMap<>();
                for (Map<String, Object> operator : operators) {
                    operatorsMap.put((String) operator.get("id"), operator);
                }
                return operatorsMap;
            }

            return new HashMap<>();
        } catch (Exception e) {
            log.error("Error fetching operators: {}", e.getMessage());
            return new HashMap<>();
        }
    }

    /**
     * Get emergency by ID with caller and operator details
     */
    public Map<String, Object> getEmergencyById(String emergencyId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            // Get emergency by ID
            String url = supabaseConfig.getApiUrl() + "/emergency?id=eq." + emergencyId;

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> emergencies = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );

                if (!emergencies.isEmpty()) {
                    Map<String, Object> emergency = emergencies.get(0);

                    // Get caller and operator details
                    String callerId = (String) emergency.get("caller_id");
                    String operatorId = (String) emergency.get("operator_id");

                    if (callerId != null) {
                        emergency.put("caller", getCallerById(callerId));
                    }

                    if (operatorId != null) {
                        emergency.put("operator", getOperatorById(operatorId));
                    }

                    log.info("Retrieved emergency record: {}", emergencyId);
                    return emergency;
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Error fetching emergency by ID: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to fetch emergency: " + e.getMessage(), e);
        }
    }

    /**
     * Get caller by ID
     */
    private Map<String, Object> getCallerById(String callerId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = supabaseConfig.getApiUrl() + "/caller?id=eq." + callerId;

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> callers = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                if (!callers.isEmpty()) {
                    return callers.get(0);
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Error fetching caller {}: {}", callerId, e.getMessage());
            return null;
        }
    }

    /**
     * Get operator by ID
     */
    private Map<String, Object> getOperatorById(String operatorId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);
            String url = supabaseConfig.getApiUrl() + "/operator?id=eq." + operatorId;

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                List<Map<String, Object>> operators = objectMapper.readValue(
                        response.getBody(),
                        new TypeReference<List<Map<String, Object>>>() {}
                );
                if (!operators.isEmpty()) {
                    return operators.get(0);
                }
            }

            return null;
        } catch (Exception e) {
            log.error("Error fetching operator {}: {}", operatorId, e.getMessage());
            return null;
        }
    }
}
