package com.ecall.auth.controller;

import com.ecall.auth.service.RiskLevelAssessmentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/openai")
@RequiredArgsConstructor
@Slf4j
public class RiskAssessmentController {

    private final RiskLevelAssessmentService riskLevelAssessmentService;

    /**
     * Assess risk level from emergency call transcript
     *
     * Request body:
     * {
     *   "transcript": "accumulated conversation text..."
     * }
     *
     * Response:
     * {
     *   "severityLevel": 1-5 (1=Critical, 5=Minimal - Frontend scale),
     *   "reason": "explanation text"
     * }
     */
    @PostMapping("/assess-risk")
    public ResponseEntity<Map<String, Object>> assessRisk(@RequestBody Map<String, String> request) {
        try {
            String transcript = request.get("transcript");

            if (transcript == null || transcript.trim().isEmpty()) {
                log.warn("Empty transcript received for risk assessment");
                return ResponseEntity.ok(Map.of(
                    "severityLevel", 3,
                    "reason", "No transcript provided"
                ));
            }

            log.info("Assessing risk for transcript length: {}", transcript.length());

            // Call service to get risk assessment
            Map<String, Object> assessment = riskLevelAssessmentService.assessRiskLevel(transcript);

            // Both backend and frontend use 1-5 scale where 1=Low, 5=Critical
            // No conversion needed
            int severityLevel = (int) assessment.get("level");
            String reason = (String) assessment.get("reason");

            Map<String, Object> response = new HashMap<>();
            response.put("severityLevel", severityLevel);
            response.put("reason", reason);

            log.info("Risk assessment complete: severityLevel={}, reason={}", severityLevel, reason);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error in risk assessment endpoint: {}", e.getMessage(), e);
            return ResponseEntity.ok(Map.of(
                "severityLevel", 3,
                "reason", "Error during assessment: " + e.getMessage()
            ));
        }
    }
}
