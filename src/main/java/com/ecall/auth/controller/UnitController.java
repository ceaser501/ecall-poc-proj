package com.ecall.auth.controller;

import com.ecall.auth.service.UnitService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/units")
@RequiredArgsConstructor
public class UnitController {

    private final UnitService unitService;

    /**
     * Get all operators (optionally filtered by role)
     */
    @GetMapping("/operators")
    public ResponseEntity<Map<String, Object>> getOperators(@RequestParam(required = false) String role) {
        try {
            List<Map<String, Object>> operators = unitService.getOperators(role);

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
     * Get all units
     */
    @GetMapping
    public ResponseEntity<Map<String, Object>> getAllUnits() {
        try {
            List<Map<String, Object>> units = unitService.getAllUnits();

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", units
            ));
        } catch (Exception e) {
            log.error("Error fetching units", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Create a new unit
     */
    @PostMapping
    public ResponseEntity<Map<String, Object>> createUnit(@RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            String leader1Id = request.get("leader1Id");
            String leader2Id = request.get("leader2Id");

            if (name == null || name.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "부대명은 필수입니다"
                ));
            }

            Map<String, Object> unit = unitService.createUnit(name, description, leader1Id, leader2Id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", unit
            ));
        } catch (Exception e) {
            log.error("Error creating unit", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Update a unit
     */
    @PatchMapping("/{unitId}")
    public ResponseEntity<Map<String, Object>> updateUnit(
            @PathVariable String unitId,
            @RequestBody Map<String, String> request) {
        try {
            String name = request.get("name");
            String description = request.get("description");
            String leader1Id = request.get("leader1Id");
            String leader2Id = request.get("leader2Id");

            if (name == null || name.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "부대명은 필수입니다"
                ));
            }

            Map<String, Object> unit = unitService.updateUnit(unitId, name, description, leader1Id, leader2Id);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", unit
            ));
        } catch (Exception e) {
            log.error("Error updating unit", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Delete a unit
     */
    @DeleteMapping("/{unitId}")
    public ResponseEntity<Map<String, Object>> deleteUnit(@PathVariable String unitId) {
        try {
            unitService.deleteUnit(unitId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "부대가 삭제되었습니다"
            ));
        } catch (Exception e) {
            log.error("Error deleting unit", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Add member to unit
     */
    @PostMapping("/{unitId}/members")
    public ResponseEntity<Map<String, Object>> addMember(
            @PathVariable String unitId,
            @RequestBody Map<String, String> request) {
        try {
            String operatorId = request.get("operatorId");

            if (operatorId == null || operatorId.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                        "success", false,
                        "error", "대원 ID는 필수입니다"
                ));
            }

            unitService.addMember(unitId, operatorId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "부대원이 추가되었습니다"
            ));
        } catch (Exception e) {
            log.error("Error adding member to unit", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Assign multiple operators to unit
     */
    @PostMapping("/{unitId}/assign-members")
    public ResponseEntity<Map<String, Object>> assignMembers(
            @PathVariable String unitId,
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

            unitService.assignOperatorsToUnit(unitId, operatorIds);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", operatorIds.size() + "명의 대원이 배치되었습니다"
            ));
        } catch (Exception e) {
            log.error("Error assigning members to unit", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Get unit members
     */
    @GetMapping("/{unitId}/members")
    public ResponseEntity<Map<String, Object>> getUnitMembers(@PathVariable String unitId) {
        try {
            List<Map<String, Object>> members = unitService.getUnitMembers(unitId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "data", members
            ));
        } catch (Exception e) {
            log.error("Error fetching unit members", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }

    /**
     * Remove member from unit
     */
    @DeleteMapping("/{unitId}/members/{operatorId}")
    public ResponseEntity<Map<String, Object>> removeMember(
            @PathVariable String unitId,
            @PathVariable String operatorId) {
        try {
            unitService.removeMember(unitId, operatorId);

            return ResponseEntity.ok(Map.of(
                    "success", true,
                    "message", "부대원이 제거되었습니다"
            ));
        } catch (Exception e) {
            log.error("Error removing member from unit", e);
            return ResponseEntity.internalServerError().body(Map.of(
                    "success", false,
                    "error", e.getMessage()
            ));
        }
    }
}
