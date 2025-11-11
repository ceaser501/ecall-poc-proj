package com.ecall.auth.controller;

import com.ecall.auth.dto.LoginRequest;
import com.ecall.auth.dto.LoginResponse;
import com.ecall.auth.dto.MediaAssetResponse;
import com.ecall.auth.dto.OperatorRegistrationRequest;
import com.ecall.auth.dto.OperatorRegistrationResponse;
import com.ecall.auth.service.AddressService;
import com.ecall.auth.service.CallerService;
import com.ecall.auth.service.ChecklistResponseService;
import com.ecall.auth.service.EmergencyService;
import com.ecall.auth.service.IncidentTypeClassificationService;
import com.ecall.auth.service.LocationExtractionService;
import com.ecall.auth.service.MediaAssetService;
import com.ecall.auth.service.OperatorService;
import com.ecall.auth.service.RiskLevelAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
@CrossOrigin(origins = "*")
public class OperatorController {

    private final OperatorService operatorService;
    private final MediaAssetService mediaAssetService;
    private final AddressService addressService;
    private final LocationExtractionService locationExtractionService;
    private final IncidentTypeClassificationService incidentTypeClassificationService;
    private final ChecklistResponseService checklistResponseService;
    private final CallerService callerService;
    private final EmergencyService emergencyService;
    private final RiskLevelAssessmentService riskLevelAssessmentService;

    @PostMapping("/register")
    public ResponseEntity<OperatorRegistrationResponse> registerOperator(
            @RequestBody OperatorRegistrationRequest request) {

        log.info("Registration request received for operator ID: {}", request.getOperatorId());

        OperatorRegistrationResponse response = operatorService.registerOperator(request);

        if (response.isSuccess()) {
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@RequestBody LoginRequest request) {
        log.info("Login request received for operator ID: {}", request.getOperatorId());

        LoginResponse response = operatorService.login(request);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
        }
    }

    @PostMapping("/upload-photo")
    public ResponseEntity<MediaAssetResponse> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @RequestParam("operatorId") String operatorId) {

        log.info("Photo upload request received for operator ID: {}", operatorId);

        MediaAssetResponse response = mediaAssetService.uploadPhoto(file, operatorId);

        if (response.isSuccess()) {
            return ResponseEntity.ok(response);
        } else {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
        }
    }

    @GetMapping("/search-address")
    public ResponseEntity<Map<String, String>> searchAddress(@RequestParam("query") String query) {
        log.info("Address search request: {}", query);

        String formalAddress = addressService.searchAddress(query);

        Map<String, String> response = new HashMap<>();
        response.put("query", query);
        response.put("address", formalAddress);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/extract-location")
    public ResponseEntity<Map<String, String>> extractLocation(@RequestBody Map<String, String> request) {
        String transcript = request.get("transcript");
        log.info("Location extraction request for transcript length: {}", transcript != null ? transcript.length() : 0);

        String location = locationExtractionService.extractLocationFromTranscript(transcript);

        Map<String, String> response = new HashMap<>();
        response.put("location", location != null ? location : "");
        response.put("success", location != null ? "true" : "false");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/classify-incident")
    public ResponseEntity<Map<String, String>> classifyIncident(@RequestBody Map<String, String> request) {
        String transcript = request.get("transcript");
        log.info("Incident classification request for transcript length: {}", transcript != null ? transcript.length() : 0);

        String incidentType = incidentTypeClassificationService.classifyIncidentType(transcript);

        Map<String, String> response = new HashMap<>();
        response.put("incidentType", incidentType != null ? incidentType : "");
        response.put("success", incidentType != null ? "true" : "false");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/save-checklist")
    public ResponseEntity<Map<String, Object>> saveChecklist(@RequestBody Map<String, Object> request) {
        String emergencyCallId = (String) request.get("emergencyCallId");
        List<Map<String, String>> responses = (List<Map<String, String>>) request.get("responses");

        log.info("Checklist save request for emergency: {} with {} responses", emergencyCallId, responses != null ? responses.size() : 0);

        if (emergencyCallId == null || emergencyCallId.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Emergency call ID is required"
            ));
        }

        if (responses == null || responses.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of(
                    "success", false,
                    "message", "Checklist responses are required"
            ));
        }

        // Convert to ChecklistResponse objects
        List<ChecklistResponseService.ChecklistResponse> checklistResponses = new java.util.ArrayList<>();
        for (Map<String, String> resp : responses) {
            String question = resp.get("question");
            String answer = resp.get("answer");
            checklistResponses.add(new ChecklistResponseService.ChecklistResponse(question, answer));
        }

        // Save to database
        boolean success = checklistResponseService.saveChecklistResponses(emergencyCallId, checklistResponses);

        Map<String, Object> response = new HashMap<>();
        response.put("success", success);
        response.put("message", success ? "Checklist saved successfully" : "Failed to save checklist");

        return ResponseEntity.ok(response);
    }

    @PostMapping("/complete-emergency")
    public ResponseEntity<Map<String, Object>> completeEmergency(@RequestBody Map<String, Object> request) {
        String callerPhoneNumber = (String) request.get("callerPhoneNumber");
        String callerName = (String) request.get("callerName");
        String operatorId = (String) request.get("operatorId");
        String language = (String) request.get("language");
        String transcript = (String) request.get("transcript");
        Integer totalDurationMs = (Integer) request.get("totalDurationMs");
        Integer speakersCount = (Integer) request.get("speakersCount");
        Integer utterancesCount = (Integer) request.get("utterancesCount");
        String incidentType = (String) request.get("incidentType");
        String callerLocation = (String) request.get("callerLocation");
        String mediaAssetId = (String) request.get("mediaAssetId");
        Long callStartedAtMs = request.get("callStartedAtMs") != null ?
            ((Number) request.get("callStartedAtMs")).longValue() : null;
        Long callEndedAtMs = request.get("callEndedAtMs") != null ?
            ((Number) request.get("callEndedAtMs")).longValue() : null;

        log.info("Emergency complete request - caller: {}, operator: {}, duration: {}ms, speakers: {}, utterances: {}, type: {}, location: {}, media: {}, transcript length: {}",
                callerPhoneNumber, operatorId, totalDurationMs, speakersCount, utterancesCount,
                incidentType, callerLocation, mediaAssetId, transcript != null ? transcript.length() : 0);

        try {
            // Step 1: Assess risk level using AI
            Map<String, Object> riskAssessment = riskLevelAssessmentService.assessRiskLevel(transcript);
            int riskLevel = (Integer) riskAssessment.get("level");
            String riskLevelReason = (String) riskAssessment.get("reason");

            log.info("Risk assessment - Level: {}, Reason: {}", riskLevel, riskLevelReason);

            // Step 2: Create/get caller
            String callerId = callerService.getOrCreateCaller(callerPhoneNumber, callerName);
            log.info("Caller ID: {}", callerId);

            // Step 3: Extract location information if caller location provided
            String extractedLocation = null;
            Double latitude = null;
            Double longitude = null;
            String roadAddress = null;
            String postalCode = null;
            String address1 = null;
            String address2 = null;

            if (callerLocation != null && !callerLocation.isEmpty()) {
                // Normalize location text (fix common speech recognition errors)
                String normalizedLocation = normalizeLocationText(callerLocation);
                log.info("Extracting location from: {} (normalized: {})", callerLocation, normalizedLocation);

                try {
                    // Get detailed location information from Kakao API
                    AddressService.AddressDetail addressDetail = addressService.searchAddressDetail(normalizedLocation);
                    if (addressDetail != null) {
                        extractedLocation = normalizedLocation; // Store original text
                        latitude = addressDetail.getLatitude();
                        longitude = addressDetail.getLongitude();
                        roadAddress = addressDetail.getRoadAddress();
                        postalCode = addressDetail.getPostalCode();

                        // Use region info to construct address1 and address2
                        // address1: 시/도 + 구
                        // address2: 동 + 건물명
                        if (addressDetail.getRegion1() != null && addressDetail.getRegion2() != null) {
                            address1 = addressDetail.getRegion1() + " " + addressDetail.getRegion2();
                        }
                        if (addressDetail.getRegion3() != null) {
                            address2 = addressDetail.getRegion3();
                            if (addressDetail.getBuildingName() != null && !addressDetail.getBuildingName().isEmpty()) {
                                address2 += " " + addressDetail.getBuildingName();
                            }
                        }

                        log.info("Address detail found - Road: {}, Postal: {}, Lat: {}, Lng: {}, Address1: {}, Address2: {}",
                                roadAddress, postalCode, latitude, longitude, address1, address2);
                    } else {
                        // If Kakao API fails, just store the normalized location
                        extractedLocation = normalizedLocation;
                        log.warn("Failed to get address detail from Kakao API");
                    }
                } catch (Exception e) {
                    log.warn("Failed to extract location details: {}", e.getMessage());
                    extractedLocation = normalizedLocation;
                }
            }

            // Step 4: Use call times from frontend timeline (using Korea timezone - Asia/Seoul)
            java.time.ZoneId seoulZone = java.time.ZoneId.of("Asia/Seoul");
            java.time.OffsetDateTime callStartedAt;
            java.time.OffsetDateTime callEndedAt;

            if (callStartedAtMs != null && callEndedAtMs != null) {
                // Use times from frontend timeline
                callStartedAt = java.time.OffsetDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(callStartedAtMs),
                    seoulZone
                );
                callEndedAt = java.time.OffsetDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(callEndedAtMs),
                    seoulZone
                );
            } else if (callStartedAtMs != null) {
                // Fallback: use start time + duration
                callStartedAt = java.time.OffsetDateTime.ofInstant(
                    java.time.Instant.ofEpochMilli(callStartedAtMs),
                    seoulZone
                );
                if (totalDurationMs != null) {
                    callEndedAt = callStartedAt.plusNanos(totalDurationMs * 1_000_000L);
                } else {
                    callEndedAt = java.time.OffsetDateTime.now(seoulZone);
                }
            } else {
                // Fallback: use current time
                callEndedAt = java.time.OffsetDateTime.now(seoulZone);
                if (totalDurationMs != null) {
                    callStartedAt = callEndedAt.minusNanos(totalDurationMs * 1_000_000L);
                } else {
                    callStartedAt = callEndedAt;
                }
            }

            log.info("Call times - Started: {}, Ended: {}, Duration: {}ms",
                callStartedAt, callEndedAt, totalDurationMs);

            // Step 5: Create emergency record with all information
            String emergencyId = emergencyService.insertEmergencyCompleteWithDetails(
                    callerId, operatorId, "clova", language,
                    totalDurationMs, speakersCount, utterancesCount,
                    String.valueOf(riskLevel), riskLevelReason,
                    incidentType, extractedLocation, latitude, longitude,
                    roadAddress, postalCode, address1, address2,
                    mediaAssetId, callStartedAt, callEndedAt
            );
            log.info("Emergency call created: {}", emergencyId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "emergencyId", emergencyId,
                    "callerId", callerId,
                    "riskLevel", riskLevel,
                    "riskLevelReason", riskLevelReason
            ));

        } catch (Exception e) {
            log.error("Failed to complete emergency: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "message", "Failed to create emergency record: " + e.getMessage()
            ));
        }
    }

    /**
     * Normalize location text to fix common speech recognition errors
     */
    private String normalizeLocationText(String location) {
        if (location == null || location.isEmpty()) {
            return location;
        }

        String normalized = location;

        // Fix common Korean district name recognition errors
        // "ganggu" -> "Gangseo-gu"
        normalized = normalized.replaceAll("(?i)\\bganggu\\b", "Gangseo-gu");

        // Add more common corrections as needed
        // "gangnam" -> "Gangnam-gu"
        normalized = normalized.replaceAll("(?i)\\bgangnam(?!-gu)\\b", "Gangnam-gu");

        // "gangdong" -> "Gangdong-gu"
        normalized = normalized.replaceAll("(?i)\\bgangdong(?!-gu)\\b", "Gangdong-gu");

        // "jongno" -> "Jongno-gu"
        normalized = normalized.replaceAll("(?i)\\bjongno(?!-gu)\\b", "Jongno-gu");

        // "mapo" -> "Mapo-gu"
        normalized = normalized.replaceAll("(?i)\\bmapo(?!-gu)\\b", "Mapo-gu");

        return normalized;
    }
}
