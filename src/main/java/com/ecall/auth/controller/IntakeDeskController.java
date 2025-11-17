package com.ecall.auth.controller;

import com.ecall.auth.service.IntakeDeskService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/intake-desks")
@RequiredArgsConstructor
public class IntakeDeskController {

    private final IntakeDeskService intakeDeskService;

    @Value("${kakao.maps.javascript.api.key:}")
    private String kakaoMapsApiKey;

    /**
     * Get Kakao Maps API Key
     */
    @GetMapping("/kakao-api-key")
    public ResponseEntity<Map<String, Object>> getKakaoApiKey() {
        return ResponseEntity.ok(Map.of(
                "success", true,
                "apiKey", kakaoMapsApiKey != null ? kakaoMapsApiKey : ""
        ));
    }

    /**
     * Get all operators with role "Operator"
     */
    @GetMapping("/operators")
    public ResponseEntity<Map<String, Object>> getOperators() {
        try {
            List<Map<String, Object>> operators = intakeDeskService.getOperators();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", operators
            ));
        } catch (Exception e) {
            log.error("Error fetching operators", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get all intake desks
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllIntakeDesks() {
        try {
            List<Map<String, Object>> intakeDesks = intakeDeskService.getAllIntakeDesks();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", intakeDesks
            ));
        } catch (Exception e) {
            log.error("Error fetching intake desks", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Create a new intake desk
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createIntakeDesk(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "조직명은 필수입니다"
                ));
            }

            Map<String, Object> intakeDesk = intakeDeskService.createIntakeDesk(name, description);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", intakeDesk
            ));
        } catch (Exception e) {
            log.error("Error creating intake desk", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Update an intake desk
     */
    @PatchMapping("/{intakeDeskId}")
    public ResponseEntity<Map<String, Object>> updateIntakeDesk(
            @PathVariable String intakeDeskId,
            @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");

            if (name == null || name.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "조직명은 필수입니다"
                ));
            }

            Map<String, Object> intakeDesk = intakeDeskService.updateIntakeDesk(intakeDeskId, name, description);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", intakeDesk
            ));
        } catch (Exception e) {
            log.error("Error updating intake desk", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Delete an intake desk
     */
    @DeleteMapping("/{intakeDeskId}")
    public ResponseEntity<Map<String, Object>> deleteIntakeDesk(@PathVariable String intakeDeskId) {
        try {
            intakeDeskService.deleteIntakeDesk(intakeDeskId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "조직이 삭제되었습니다"
            ));
        } catch (Exception e) {
            log.error("Error deleting intake desk", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Assign multiple operators to intake desk
     */
    @PostMapping("/{intakeDeskId}/assign-members")
    public ResponseEntity<Map<String, Object>> assignMembers(
            @PathVariable String intakeDeskId,
            @RequestBody Map<String, Object> request) {
        try {
            @SuppressWarnings("unchecked")
            List<String> operatorIds = (List<String>) request.get("operatorIds");

            if (operatorIds == null || operatorIds.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "배치할 대원을 선택해주세요"
                ));
            }

            intakeDeskService.assignOperatorsToIntakeDesk(intakeDeskId, operatorIds);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", operatorIds.size() + "명의 대원이 배치되었습니다"
            ));
        } catch (Exception e) {
            log.error("Error assigning members to intake desk", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get intake desk members
     */
    @GetMapping("/{intakeDeskId}/members")
    public ResponseEntity<Map<String, Object>> getIntakeDeskMembers(@PathVariable String intakeDeskId) {
        try {
            List<Map<String, Object>> members = intakeDeskService.getIntakeDeskMembers(intakeDeskId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", members
            ));
        } catch (Exception e) {
            log.error("Error fetching intake desk members", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Remove member from intake desk
     */
    @DeleteMapping("/{intakeDeskId}/members/{operatorId}")
    public ResponseEntity<Map<String, Object>> removeMember(
            @PathVariable String intakeDeskId,
            @PathVariable String operatorId) {
        try {
            intakeDeskService.removeMember(intakeDeskId, operatorId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "대원이 제거되었습니다"
            ));
        } catch (Exception e) {
            log.error("Error removing member from intake desk", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Assign leader to intake desk
     */
    @PostMapping("/{intakeDeskId}/assign-leader")
    public ResponseEntity<Map<String, Object>> assignLeader(
            @PathVariable String intakeDeskId,
            @RequestBody Map<String, String> request) {
        try {
            String operatorId = request.get("operatorId");
            String leaderType = request.get("leaderType");

            if (operatorId == null || operatorId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "조직장을 선택해주세요"
                ));
            }

            if (leaderType == null || leaderType.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "조직장 유형을 선택해주세요"
                ));
            }

            Map<String, Object> intakeDesk = intakeDeskService.assignLeader(intakeDeskId, operatorId, leaderType);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", intakeDesk
            ));
        } catch (Exception e) {
            log.error("Error assigning leader to intake desk", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Remove leader from intake desk
     */
    @DeleteMapping("/{intakeDeskId}/leaders/{leaderType}")
    public ResponseEntity<Map<String, Object>> removeLeader(
            @PathVariable String intakeDeskId,
            @PathVariable String leaderType) {
        try {
            Map<String, Object> intakeDesk = intakeDeskService.removeLeader(intakeDeskId, leaderType);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", intakeDesk
            ));
        } catch (Exception e) {
            log.error("Error removing leader from intake desk", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
