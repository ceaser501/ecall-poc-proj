package com.ecall.auth.service;

import com.ecall.auth.config.SupabaseConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import jakarta.annotation.PostConstruct;
import java.util.*;

@Service
@Slf4j
public class UnitService {

    private final SupabaseConfig supabaseConfig;
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);
    }

    public UnitService(SupabaseConfig supabaseConfig) {
        this.supabaseConfig = supabaseConfig;
    }

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        log.info("RestTemplate initialized for UnitService");
    }

    /**
     * Helper method to make PATCH requests using Apache HttpClient
     */
    private String executePatchRequest(String url, Map<String, Object> data, HttpHeaders headers) throws Exception {
        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPatch httpPatch = new HttpPatch(url);

            // Set headers
            headers.forEach((key, values) -> {
                for (String value : values) {
                    httpPatch.setHeader(key, value);
                }
            });

            // Set request body
            String jsonData = objectMapper.writeValueAsString(data);
            log.debug("PATCH request to {}: {}", url, jsonData);

            if (jsonData != null && !jsonData.isEmpty()) {
                StringEntity entity = new StringEntity(jsonData, "UTF-8");
                entity.setContentType("application/json");
                httpPatch.setEntity(entity);
            }

            // Execute request
            try (CloseableHttpResponse response = httpClient.execute(httpPatch)) {
                int statusCode = response.getStatusLine().getStatusCode();

                String responseBody = "";
                if (response.getEntity() != null) {
                    responseBody = EntityUtils.toString(response.getEntity(), "UTF-8");
                }

                log.debug("PATCH response from {}: status={}, body={}", url, statusCode, responseBody);

                if (statusCode >= 200 && statusCode < 300) {
                    return responseBody;
                } else {
                    throw new RuntimeException("PATCH request failed with status " + statusCode + ": " + responseBody);
                }
            }
        }
    }

    /**
     * Get all operators (optionally filtered by role)
     */
    public List<Map<String, Object>> getOperators(String role) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/operator";
            if (role != null && !role.isEmpty()) {
                url += "?role=eq." + role + "&select=id,name,operator_id,role,organization_name,organization_code";
            } else {
                url += "?select=id,name,operator_id,role,organization_name,organization_code";
            }

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonArray = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> operators = new ArrayList<>();

            for (JsonNode node : jsonArray) {
                Map<String, Object> operator = new HashMap<>();
                operator.put("id", node.get("id").asText());
                operator.put("name", node.get("name").asText());
                operator.put("operator_id", node.get("operator_id").asText());
                operator.put("role", node.has("role") && !node.get("role").isNull() ? node.get("role").asText() : null);
                operator.put("organization_name", node.has("organization_name") && !node.get("organization_name").isNull()
                    ? node.get("organization_name").asText() : null);
                operator.put("organization_code", node.has("organization_code") && !node.get("organization_code").isNull()
                    ? node.get("organization_code").asText() : null);
                operators.add(operator);
            }

            log.info("Found {} operators" + (role != null ? " with role " + role : ""), operators.size());
            return operators;

        } catch (Exception e) {
            log.error("Error fetching operators", e);
            return new ArrayList<>();
        }
    }

    /**
     * Create a new unit (dispatch_force)
     */
    public Map<String, Object> createUnit(String name, String description, String leader1Id, String leader2Id) {
        try {
            // Generate custom ID: df-{uuid}
            String unitId = "df-" + UUID.randomUUID().toString();

            // Create unit data
            Map<String, Object> unitData = new HashMap<>();
            unitData.put("id", unitId);
            unitData.put("name", name);
            unitData.put("operator_leader1_id", leader1Id);
            unitData.put("operator_leader2_id", leader2Id);
            unitData.put("member_count", 0); // Initial member count is 0
            unitData.put("created_at", java.time.LocalDateTime.now().toString());

            // Send request to Supabase
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(unitData, headers);

            String url = supabaseConfig.getApiUrl() + "/dispatch_force";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode result = objectMapper.readTree(response.getBody());
            JsonNode unitNode = result.isArray() ? result.get(0) : result;

            Map<String, Object> createdUnit = new HashMap<>();
            createdUnit.put("id", unitNode.get("id").asText());
            createdUnit.put("name", unitNode.get("name").asText());
            createdUnit.put("operator_leader1_id", unitNode.has("operator_leader1_id") && !unitNode.get("operator_leader1_id").isNull()
                ? unitNode.get("operator_leader1_id").asText() : null);
            createdUnit.put("operator_leader2_id", unitNode.has("operator_leader2_id") && !unitNode.get("operator_leader2_id").isNull()
                ? unitNode.get("operator_leader2_id").asText() : null);
            createdUnit.put("member_count", unitNode.get("member_count").asInt());
            createdUnit.put("created_at", unitNode.get("created_at").asText());

            log.info("Unit created successfully: {} (ID: {})", name, unitId);
            return createdUnit;

        } catch (Exception e) {
            log.error("Error creating unit", e);
            throw new RuntimeException("Failed to create unit: " + e.getMessage(), e);
        }
    }

    /**
     * Get all units (dispatch_force)
     */
    public List<Map<String, Object>> getAllUnits() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?type=eq.dispatch&select=*&order=created_at.desc";

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonArray = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> units = new ArrayList<>();

            for (JsonNode node : jsonArray) {
                Map<String, Object> unit = new HashMap<>();
                String unitId = node.get("id").asText();

                unit.put("id", unitId);
                unit.put("name", node.get("name").asText());
                unit.put("type", node.has("type") && !node.get("type").isNull()
                    ? node.get("type").asText() : "dispatch");

                String leader1Id = node.has("operator_leader1_id") && !node.get("operator_leader1_id").isNull()
                    ? node.get("operator_leader1_id").asText() : null;
                String leader2Id = node.has("operator_leader2_id") && !node.get("operator_leader2_id").isNull()
                    ? node.get("operator_leader2_id").asText() : null;

                unit.put("operator_leader1_id", leader1Id);
                unit.put("operator_leader2_id", leader2Id);

                // Get leader details (name, role, join_date, phone_number)
                if (leader1Id != null) {
                    Map<String, Object> leader1 = getOperatorDetailsById(leader1Id);
                    unit.put("leader1_name", leader1 != null ? leader1.get("name") : null);
                    unit.put("leader1_role", leader1 != null ? leader1.get("role") : null);
                    unit.put("leader1_join_date", leader1 != null ? leader1.get("join_date") : null);
                    unit.put("leader1_phone", leader1 != null ? leader1.get("phone_number") : null);
                } else {
                    unit.put("leader1_name", null);
                    unit.put("leader1_role", null);
                    unit.put("leader1_join_date", null);
                    unit.put("leader1_phone", null);
                }

                if (leader2Id != null) {
                    Map<String, Object> leader2 = getOperatorDetailsById(leader2Id);
                    unit.put("leader2_name", leader2 != null ? leader2.get("name") : null);
                    unit.put("leader2_role", leader2 != null ? leader2.get("role") : null);
                    unit.put("leader2_join_date", leader2 != null ? leader2.get("join_date") : null);
                    unit.put("leader2_phone", leader2 != null ? leader2.get("phone_number") : null);
                } else {
                    unit.put("leader2_name", null);
                    unit.put("leader2_role", null);
                    unit.put("leader2_join_date", null);
                    unit.put("leader2_phone", null);
                }

                // Calculate actual member count by querying operators
                int actualMemberCount = countUnitMembers(unitId);
                unit.put("member_count", actualMemberCount);
                log.debug("Unit {} has actual member_count: {}", node.get("name").asText(), actualMemberCount);

                unit.put("created_at", node.get("created_at").asText());
                units.add(unit);
            }

            log.info("Found {} units", units.size());
            return units;

        } catch (Exception e) {
            log.error("Error fetching units", e);
            return new ArrayList<>();
        }
    }

    /**
     * Update unit (dispatch_force)
     */
    public Map<String, Object> updateUnit(String unitId, String name, String description,
                                          String leader1Id, String leader2Id) {
        try {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", name);
            if (leader1Id != null) {
                updateData.put("operator_leader1_id", leader1Id);
            }
            if (leader2Id != null) {
                updateData.put("operator_leader2_id", leader2Id);
            }

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + unitId;

            // Use custom PATCH implementation
            String responseBody = executePatchRequest(url, updateData, headers);

            JsonNode result = objectMapper.readTree(responseBody);
            JsonNode unitNode = result.isArray() ? result.get(0) : result;

            Map<String, Object> updatedUnit = new HashMap<>();
            updatedUnit.put("id", unitNode.get("id").asText());
            updatedUnit.put("name", unitNode.get("name").asText());
            updatedUnit.put("operator_leader1_id", unitNode.has("operator_leader1_id") && !unitNode.get("operator_leader1_id").isNull()
                ? unitNode.get("operator_leader1_id").asText() : null);
            updatedUnit.put("operator_leader2_id", unitNode.has("operator_leader2_id") && !unitNode.get("operator_leader2_id").isNull()
                ? unitNode.get("operator_leader2_id").asText() : null);
            updatedUnit.put("member_count", unitNode.get("member_count").asInt());

            log.info("Unit updated successfully: {}", unitId);
            return updatedUnit;

        } catch (Exception e) {
            log.error("Error updating unit", e);
            throw new RuntimeException("Failed to update unit: " + e.getMessage(), e);
        }
    }

    /**
     * Delete unit (dispatch_force)
     */
    public void deleteUnit(String unitId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + unitId;

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );

            log.info("Unit deleted successfully: {}", unitId);

        } catch (Exception e) {
            log.error("Error deleting unit", e);
            throw new RuntimeException("Failed to delete unit: " + e.getMessage(), e);
        }
    }

    /**
     * Add member to unit (increment member_count)
     */
    public void addMember(String unitId, String operatorId) {
        try {
            // First, get current member_count
            Map<String, Object> unit = getUnitById(unitId);
            int currentCount = (int) unit.get("member_count");

            // Increment member_count
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("member_count", currentCount + 1);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + unitId;

            // Use custom PATCH implementation
            executePatchRequest(url, updateData, headers);

            log.info("Member added to unit {}: operator {}", unitId, operatorId);

        } catch (Exception e) {
            log.error("Error adding member to unit", e);
            throw new RuntimeException("Failed to add member: " + e.getMessage(), e);
        }
    }

    /**
     * Remove member from unit (decrement member_count)
     */
    public void removeMember(String unitId, String operatorId) {
        try {
            // Get unit details to check if this operator is a leader
            Map<String, Object> unit = getUnitById(unitId);
            String leader1Id = (String) unit.get("operator_leader1_id");
            String leader2Id = (String) unit.get("operator_leader2_id");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            // Remove organization_code and organization_name from operator
            Map<String, Object> operatorUpdate = new HashMap<>();
            operatorUpdate.put("organization_code", null);
            operatorUpdate.put("organization_name", null);

            String operatorUrl = supabaseConfig.getApiUrl() + "/operator?id=eq." + operatorId;
            executePatchRequest(operatorUrl, operatorUpdate, headers);

            // If removing a leader, also remove from unit's leader fields
            Map<String, Object> unitUpdate = new HashMap<>();
            boolean needsUnitUpdate = false;

            if (operatorId.equals(leader1Id)) {
                unitUpdate.put("operator_leader1_id", null);
                needsUnitUpdate = true;
                log.info("Removing leader1 from unit {}", unitId);
            }

            if (operatorId.equals(leader2Id)) {
                unitUpdate.put("operator_leader2_id", null);
                needsUnitUpdate = true;
                log.info("Removing leader2 from unit {}", unitId);
            }

            // Update unit's member_count by counting actual members
            int actualMemberCount = countUnitMembers(unitId);
            unitUpdate.put("member_count", actualMemberCount);

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + unitId;
            executePatchRequest(url, unitUpdate, headers);

            log.info("Member removed from unit {}: operator {}. New member count: {}",
                     unitId, operatorId, actualMemberCount);

        } catch (Exception e) {
            log.error("Error removing member from unit", e);
            throw new RuntimeException("Failed to remove member: " + e.getMessage(), e);
        }
    }

    /**
     * Assign multiple operators to unit (batch update organization_code and organization_name)
     */
    public void assignOperatorsToUnit(String unitId, List<String> operatorIds) {
        try {
            // Get unit details
            Map<String, Object> unit = getUnitById(unitId);
            String unitName = (String) unit.get("name");
            int currentMemberCount = (int) unit.get("member_count");

            // Update each operator's organization_code and organization_name
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            int successCount = 0;
            for (String operatorId : operatorIds) {
                try {
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("organization_code", unitId);
                    updateData.put("organization_name", unitName);

                    String url = supabaseConfig.getApiUrl() + "/operator?id=eq." + operatorId;
                    executePatchRequest(url, updateData, headers);
                    successCount++;
                    log.info("Operator {} assigned to unit {} ({})", operatorId, unitId, unitName);
                } catch (Exception e) {
                    log.error("Failed to assign operator {} to unit {}", operatorId, unitId, e);
                }
            }

            // Update unit's member_count by counting actual members
            if (successCount > 0) {
                // Count actual members in the unit
                int actualMemberCount = countUnitMembers(unitId);

                Map<String, Object> updateData = new HashMap<>();
                updateData.put("member_count", actualMemberCount);

                String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + unitId;
                executePatchRequest(url, updateData, headers);

                log.info("Successfully assigned {} operators to unit {}. Total member count: {}",
                         successCount, unitId, actualMemberCount);
            }

        } catch (Exception e) {
            log.error("Error assigning operators to unit", e);
            throw new RuntimeException("Failed to assign operators: " + e.getMessage(), e);
        }
    }

    /**
     * Count members in a unit
     */
    private int countUnitMembers(String unitId) {
        try {
            List<Map<String, Object>> members = getUnitMembers(unitId);
            return members.size();
        } catch (Exception e) {
            log.error("Error counting unit members for unit {}", unitId, e);
            return 0;
        }
    }

    /**
     * Get unit members (operators assigned to this unit)
     * Includes both regular members and leaders
     */
    public List<Map<String, Object>> getUnitMembers(String unitId) {
        try {
            // First get unit details to get leader IDs
            Map<String, Object> unit = getUnitById(unitId);
            String leader1Id = (String) unit.get("operator_leader1_id");
            String leader2Id = (String) unit.get("operator_leader2_id");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            // Query operators where organization_code matches unitId
            String url = supabaseConfig.getApiUrl() + "/operator?organization_code=eq." + unitId +
                         "&select=id,name,operator_id,role,organization_name,organization_code,phone_number,join_date";

            log.info("Querying unit members with URL: {}", url);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            log.info("Response from getUnitMembers for unit {}: {}", unitId, response.getBody());

            JsonNode jsonArray = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> members = new ArrayList<>();
            Set<String> addedMemberIds = new HashSet<>();

            // Add regular members
            for (JsonNode node : jsonArray) {
                Map<String, Object> member = new HashMap<>();
                String memberId = node.get("id").asText();
                member.put("id", memberId);
                member.put("name", node.get("name").asText());
                member.put("operator_id", node.get("operator_id").asText());
                member.put("role", node.has("role") && !node.get("role").isNull() ? node.get("role").asText() : null);
                member.put("organization_name", node.has("organization_name") && !node.get("organization_name").isNull()
                    ? node.get("organization_name").asText() : null);
                member.put("phone_number", node.has("phone_number") && !node.get("phone_number").isNull()
                    ? node.get("phone_number").asText() : null);
                member.put("join_date", node.has("join_date") && !node.get("join_date").isNull()
                    ? node.get("join_date").asText() : null);

                // Mark if this member is a leader
                if (memberId.equals(leader1Id)) {
                    member.put("position", "팀장");
                } else if (memberId.equals(leader2Id)) {
                    member.put("position", "부팀장");
                } else {
                    member.put("position", null);
                }

                members.add(member);
                addedMemberIds.add(memberId);
            }

            // Add leaders if they are not already in the member list
            if (leader1Id != null && !addedMemberIds.contains(leader1Id)) {
                Map<String, Object> leader = getOperatorById(leader1Id);
                if (leader != null) {
                    leader.put("position", "팀장");
                    members.add(0, leader); // Add at the beginning
                }
            }

            if (leader2Id != null && !addedMemberIds.contains(leader2Id)) {
                Map<String, Object> deputy = getOperatorById(leader2Id);
                if (deputy != null) {
                    deputy.put("position", "부팀장");
                    members.add(leader1Id != null && !addedMemberIds.contains(leader1Id) ? 1 : 0, deputy);
                }
            }

            log.info("Found {} total members (including leaders) for unit {}", members.size(), unitId);
            return members;

        } catch (Exception e) {
            log.error("Error fetching unit members", e);
            return new ArrayList<>();
        }
    }

    /**
     * Get unit by ID (dispatch_force)
     */
    private Map<String, Object> getUnitById(String unitId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + unitId;

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonArray = objectMapper.readTree(response.getBody());
            if (jsonArray.isEmpty()) {
                throw new RuntimeException("Unit not found: " + unitId);
            }

            JsonNode unitNode = jsonArray.get(0);
            Map<String, Object> unit = new HashMap<>();
            unit.put("id", unitNode.get("id").asText());
            unit.put("name", unitNode.get("name").asText());
            unit.put("member_count", unitNode.get("member_count").asInt());
            unit.put("operator_leader1_id", unitNode.has("operator_leader1_id") && !unitNode.get("operator_leader1_id").isNull()
                ? unitNode.get("operator_leader1_id").asText() : null);
            unit.put("operator_leader2_id", unitNode.has("operator_leader2_id") && !unitNode.get("operator_leader2_id").isNull()
                ? unitNode.get("operator_leader2_id").asText() : null);

            return unit;

        } catch (Exception e) {
            log.error("Error fetching unit by ID", e);
            throw new RuntimeException("Failed to fetch unit: " + e.getMessage(), e);
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

            String url = supabaseConfig.getApiUrl() + "/operator?id=eq." + operatorId +
                         "&select=id,name,operator_id,role,organization_name,organization_code";

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonArray = objectMapper.readTree(response.getBody());
            if (jsonArray.isEmpty()) {
                return null;
            }

            JsonNode node = jsonArray.get(0);
            Map<String, Object> operator = new HashMap<>();
            operator.put("id", node.get("id").asText());
            operator.put("name", node.get("name").asText());
            operator.put("operator_id", node.get("operator_id").asText());
            operator.put("role", node.has("role") && !node.get("role").isNull() ? node.get("role").asText() : null);
            operator.put("organization_name", node.has("organization_name") && !node.get("organization_name").isNull()
                ? node.get("organization_name").asText() : null);

            return operator;

        } catch (Exception e) {
            log.error("Error fetching operator by ID: {}", operatorId, e);
            return null;
        }
    }

    /**
     * Get operator details by ID (including role, join_date, and phone_number)
     */
    private Map<String, Object> getOperatorDetailsById(String operatorId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/operator?id=eq." + operatorId +
                         "&select=id,name,operator_id,role,organization_name,organization_code,join_date,phone_number";

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonArray = objectMapper.readTree(response.getBody());
            if (jsonArray.isEmpty()) {
                return null;
            }

            JsonNode node = jsonArray.get(0);
            Map<String, Object> operator = new HashMap<>();
            operator.put("id", node.get("id").asText());
            operator.put("name", node.get("name").asText());
            operator.put("operator_id", node.get("operator_id").asText());
            operator.put("role", node.has("role") && !node.get("role").isNull() ? node.get("role").asText() : null);
            operator.put("organization_name", node.has("organization_name") && !node.get("organization_name").isNull()
                ? node.get("organization_name").asText() : null);
            operator.put("join_date", node.has("join_date") && !node.get("join_date").isNull()
                ? node.get("join_date").asText() : null);
            operator.put("phone_number", node.has("phone_number") && !node.get("phone_number").isNull()
                ? node.get("phone_number").asText() : null);

            return operator;

        } catch (Exception e) {
            log.error("Error fetching operator details by ID: {}", operatorId, e);
            return null;
        }
    }
}
