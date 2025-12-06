package com.ecall.auth.controller;

import com.ecall.auth.service.EmergencyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/emergencies")
@RequiredArgsConstructor
public class EmergencyController {

    private final EmergencyService emergencyService;

    /**
     * Get all emergencies with caller and operator details
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllEmergencies() {
        try {
            List<Map<String, Object>> emergencies = emergencyService.getAllEmergencies();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", emergencies
            ));
        } catch (Exception e) {
            log.error("Error fetching emergencies", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get emergency by ID with caller and operator details
     */
    @GetMapping("/{emergencyId}")
    public ResponseEntity<Map<String, Object>> getEmergencyById(@PathVariable String emergencyId) {
        try {
            Map<String, Object> emergency = emergencyService.getEmergencyById(emergencyId);

            if (emergency == null) {
                return ResponseEntity.ok(Map.of(
                        "success", false,
                        "error", "Emergency not found"
                ));
            }

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", emergency
            ));
        } catch (Exception e) {
            log.error("Error fetching emergency by ID", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
