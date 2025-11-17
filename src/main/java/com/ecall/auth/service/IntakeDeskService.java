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

@Slf4j
@Service
public class IntakeDeskService {

    private final SupabaseConfig supabaseConfig;
    private RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    {
        objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(com.fasterxml.jackson.annotation.JsonInclude.Include.ALWAYS);
    }

    public IntakeDeskService(SupabaseConfig supabaseConfig) {
        this.supabaseConfig = supabaseConfig;
    }

    @PostConstruct
    public void init() {
        this.restTemplate = new RestTemplate();
        log.info("RestTemplate initialized for IntakeDeskService");
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
     * Get all operators with role "Operator"
     */
    public List<Map<String, Object>> getOperators() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/operator?role=eq.Operator&select=id,name,operator_id,role";

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

                operators.add(operator);
            }

            return operators;

        } catch (Exception e) {
            log.error("Error fetching operators", e);
            throw new RuntimeException("Failed to fetch operators: " + e.getMessage(), e);
        }
    }

    /**
     * Get all intake desks (from dispatch_force table with type='intake')
     */
    public List<Map<String, Object>> getAllIntakeDesks() {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?type=eq.intake&select=*";

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonArray = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> intakeDesks = new ArrayList<>();

            for (JsonNode node : jsonArray) {
                Map<String, Object> intakeDesk = new HashMap<>();
                String intakeDeskId = node.get("id").asText();
                intakeDesk.put("id", intakeDeskId);
                intakeDesk.put("name", node.get("name").asText());

                // Get created_at
                if (node.has("created_at") && !node.get("created_at").isNull()) {
                    intakeDesk.put("created_at", node.get("created_at").asText());
                }

                // Get leader IDs
                String leader1Id = node.has("operator_leader1_id") && !node.get("operator_leader1_id").isNull()
                        ? node.get("operator_leader1_id").asText() : null;
                String leader2Id = node.has("operator_leader2_id") && !node.get("operator_leader2_id").isNull()
                        ? node.get("operator_leader2_id").asText() : null;

                intakeDesk.put("leader1_id", leader1Id);
                intakeDesk.put("leader2_id", leader2Id);

                // Get leader names
                if (leader1Id != null && !leader1Id.isEmpty()) {
                    Map<String, Object> leader1 = getOperatorById(leader1Id);
                    intakeDesk.put("leader1_name", leader1 != null ? leader1.get("name") : null);
                } else {
                    intakeDesk.put("leader1_name", null);
                }

                if (leader2Id != null && !leader2Id.isEmpty()) {
                    Map<String, Object> leader2 = getOperatorById(leader2Id);
                    intakeDesk.put("leader2_name", leader2 != null ? leader2.get("name") : null);
                } else {
                    intakeDesk.put("leader2_name", null);
                }

                // Count actual members
                int memberCount = countIntakeDeskMembers(intakeDeskId);
                intakeDesk.put("member_count", memberCount);

                intakeDesks.add(intakeDesk);
            }

            return intakeDesks;

        } catch (Exception e) {
            log.error("Error fetching intake desks", e);
            throw new RuntimeException("Failed to fetch intake desks: " + e.getMessage(), e);
        }
    }

    /**
     * Count members in an intake desk
     */
    private int countIntakeDeskMembers(String intakeDeskId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            // Get the dispatch_force record to get organization_code
            String intakeDeskUrl = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + intakeDeskId + "&select=id,name";
            HttpEntity<String> intakeDeskEntity = new HttpEntity<>(headers);
            ResponseEntity<String> intakeDeskResponse = restTemplate.exchange(
                    intakeDeskUrl,
                    HttpMethod.GET,
                    intakeDeskEntity,
                    String.class
            );

            JsonNode intakeDeskArray = objectMapper.readTree(intakeDeskResponse.getBody());
            if (intakeDeskArray.isEmpty()) {
                return 0;
            }

            JsonNode intakeDeskNode = intakeDeskArray.get(0);
            String intakeDeskName = intakeDeskNode.get("name").asText();

            // Count operators with matching organization_name
            String url = supabaseConfig.getApiUrl() + "/operator?organization_name=eq." + intakeDeskName + "&select=id";

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonArray = objectMapper.readTree(response.getBody());
            return jsonArray.size();

        } catch (Exception e) {
            log.error("Error counting intake desk members", e);
            return 0;
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

            String url = supabaseConfig.getApiUrl() + "/operator?id=eq." + operatorId + "&select=id,name,operator_id,role";

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

            return operator;

        } catch (Exception e) {
            log.error("Error fetching operator by ID: {}", operatorId, e);
            return null;
        }
    }

    /**
     * Create a new intake desk (in dispatch_force table with type='intake')
     */
    public Map<String, Object> createIntakeDesk(String name, String description) {
        try {
            Map<String, Object> intakeDeskData = new HashMap<>();
            intakeDeskData.put("name", name);
            intakeDeskData.put("type", "intake");
            intakeDeskData.put("member_count", 0);
            // Set default leader IDs to empty string instead of null
            intakeDeskData.put("operator_leader1_id", "");
            intakeDeskData.put("operator_leader2_id", "");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            String url = supabaseConfig.getApiUrl() + "/dispatch_force";

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(intakeDeskData, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            JsonNode result = objectMapper.readTree(response.getBody());
            JsonNode intakeDeskNode = result.isArray() ? result.get(0) : result;

            Map<String, Object> newIntakeDesk = new HashMap<>();
            newIntakeDesk.put("id", intakeDeskNode.get("id").asText());
            newIntakeDesk.put("name", intakeDeskNode.get("name").asText());
            newIntakeDesk.put("leader1_id", null);
            newIntakeDesk.put("leader2_id", null);
            newIntakeDesk.put("leader1_name", null);
            newIntakeDesk.put("leader2_name", null);
            newIntakeDesk.put("member_count", 0);

            log.info("Created intake desk: {}", name);
            return newIntakeDesk;

        } catch (Exception e) {
            log.error("Error creating intake desk", e);
            throw new RuntimeException("Failed to create intake desk: " + e.getMessage(), e);
        }
    }

    /**
     * Update an intake desk
     */
    public Map<String, Object> updateIntakeDesk(String intakeDeskId, String name, String description) {
        try {
            Map<String, Object> updateData = new HashMap<>();
            updateData.put("name", name);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + intakeDeskId;

            String responseBody = executePatchRequest(url, updateData, headers);

            JsonNode result = objectMapper.readTree(responseBody);
            JsonNode intakeDeskNode = result.isArray() ? result.get(0) : result;

            Map<String, Object> updatedIntakeDesk = new HashMap<>();
            updatedIntakeDesk.put("id", intakeDeskNode.get("id").asText());
            updatedIntakeDesk.put("name", intakeDeskNode.get("name").asText());

            // Get leader IDs and names
            String leader1Id = intakeDeskNode.has("operator_leader1_id") && !intakeDeskNode.get("operator_leader1_id").isNull()
                    ? intakeDeskNode.get("operator_leader1_id").asText() : null;
            String leader2Id = intakeDeskNode.has("operator_leader2_id") && !intakeDeskNode.get("operator_leader2_id").isNull()
                    ? intakeDeskNode.get("operator_leader2_id").asText() : null;

            updatedIntakeDesk.put("leader1_id", leader1Id);
            updatedIntakeDesk.put("leader2_id", leader2Id);

            if (leader1Id != null && !leader1Id.isEmpty()) {
                Map<String, Object> leader1 = getOperatorById(leader1Id);
                updatedIntakeDesk.put("leader1_name", leader1 != null ? leader1.get("name") : null);
            } else {
                updatedIntakeDesk.put("leader1_name", null);
            }

            if (leader2Id != null && !leader2Id.isEmpty()) {
                Map<String, Object> leader2 = getOperatorById(leader2Id);
                updatedIntakeDesk.put("leader2_name", leader2 != null ? leader2.get("name") : null);
            } else {
                updatedIntakeDesk.put("leader2_name", null);
            }

            int memberCount = countIntakeDeskMembers(intakeDeskId);
            updatedIntakeDesk.put("member_count", memberCount);

            log.info("Updated intake desk: {}", intakeDeskId);
            return updatedIntakeDesk;

        } catch (Exception e) {
            log.error("Error updating intake desk", e);
            throw new RuntimeException("Failed to update intake desk: " + e.getMessage(), e);
        }
    }

    /**
     * Delete an intake desk
     */
    public void deleteIntakeDesk(String intakeDeskId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + intakeDeskId;

            HttpEntity<String> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                    url,
                    HttpMethod.DELETE,
                    entity,
                    String.class
            );

            log.info("Deleted intake desk: {}", intakeDeskId);

        } catch (Exception e) {
            log.error("Error deleting intake desk", e);
            throw new RuntimeException("Failed to delete intake desk: " + e.getMessage(), e);
        }
    }

    /**
     * Assign operators to intake desk
     */
    public void assignOperatorsToIntakeDesk(String intakeDeskId, List<String> operatorIds) {
        try {
            // Get intake desk name
            Map<String, Object> intakeDesk = getIntakeDeskById(intakeDeskId);
            String intakeDeskName = (String) intakeDesk.get("name");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            int successCount = 0;
            for (String operatorId : operatorIds) {
                try {
                    Map<String, Object> updateData = new HashMap<>();
                    updateData.put("organization_code", intakeDeskId);
                    updateData.put("organization_name", intakeDeskName);

                    String url = supabaseConfig.getApiUrl() + "/operator?id=eq." + operatorId;
                    executePatchRequest(url, updateData, headers);
                    successCount++;
                    log.info("Operator {} assigned to intake desk {} ({})", operatorId, intakeDeskId, intakeDeskName);
                } catch (Exception e) {
                    log.error("Failed to assign operator {} to intake desk {}", operatorId, intakeDeskId, e);
                }
            }

            // Update intake desk's member_count
            if (successCount > 0) {
                int actualMemberCount = countIntakeDeskMembers(intakeDeskId);

                Map<String, Object> updateData = new HashMap<>();
                updateData.put("member_count", actualMemberCount);

                String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + intakeDeskId;
                executePatchRequest(url, updateData, headers);

                log.info("Successfully assigned {} operators to intake desk {}. Total member count: {}",
                        successCount, intakeDeskId, actualMemberCount);
            }

        } catch (Exception e) {
            log.error("Error assigning operators to intake desk", e);
            throw new RuntimeException("Failed to assign operators: " + e.getMessage(), e);
        }
    }

    /**
     * Get intake desk members
     */
    public List<Map<String, Object>> getIntakeDeskMembers(String intakeDeskId) {
        try {
            // Get intake desk name
            Map<String, Object> intakeDesk = getIntakeDeskById(intakeDeskId);
            String intakeDeskName = (String) intakeDesk.get("name");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/operator?organization_name=eq." + intakeDeskName +
                    "&select=id,name,operator_id,role,organization_name";

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonArray = objectMapper.readTree(response.getBody());
            List<Map<String, Object>> members = new ArrayList<>();

            for (JsonNode node : jsonArray) {
                Map<String, Object> member = new HashMap<>();
                member.put("id", node.get("id").asText());
                member.put("name", node.get("name").asText());
                member.put("operator_id", node.get("operator_id").asText());
                member.put("role", node.has("role") && !node.get("role").isNull() ? node.get("role").asText() : null);
                member.put("organization_name", node.has("organization_name") && !node.get("organization_name").isNull()
                        ? node.get("organization_name").asText() : null);

                members.add(member);
            }

            return members;

        } catch (Exception e) {
            log.error("Error fetching intake desk members", e);
            throw new RuntimeException("Failed to fetch intake desk members: " + e.getMessage(), e);
        }
    }

    /**
     * Remove member from intake desk
     */
    public void removeMember(String intakeDeskId, String operatorId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            // Remove organization_name from operator
            Map<String, Object> operatorUpdate = new HashMap<>();
            operatorUpdate.put("organization_name", null);

            String operatorUrl = supabaseConfig.getApiUrl() + "/operator?id=eq." + operatorId;
            executePatchRequest(operatorUrl, operatorUpdate, headers);

            // Update intake desk's member_count
            int actualMemberCount = countIntakeDeskMembers(intakeDeskId);
            Map<String, Object> intakeDeskUpdate = new HashMap<>();
            intakeDeskUpdate.put("member_count", actualMemberCount);

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + intakeDeskId;
            executePatchRequest(url, intakeDeskUpdate, headers);

            log.info("Member removed from intake desk {}: operator {}. New member count: {}",
                    intakeDeskId, operatorId, actualMemberCount);

        } catch (Exception e) {
            log.error("Error removing member from intake desk", e);
            throw new RuntimeException("Failed to remove member: " + e.getMessage(), e);
        }
    }

    /**
     * Get intake desk by ID
     */
    private Map<String, Object> getIntakeDeskById(String intakeDeskId) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + intakeDeskId + "&type=eq.intake";

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonArray = objectMapper.readTree(response.getBody());
            if (jsonArray.isEmpty()) {
                throw new RuntimeException("Intake desk not found: " + intakeDeskId);
            }

            JsonNode intakeDeskNode = jsonArray.get(0);
            Map<String, Object> intakeDesk = new HashMap<>();
            intakeDesk.put("id", intakeDeskNode.get("id").asText());
            intakeDesk.put("name", intakeDeskNode.get("name").asText());
            intakeDesk.put("member_count", intakeDeskNode.has("member_count") ? intakeDeskNode.get("member_count").asInt() : 0);

            return intakeDesk;

        } catch (Exception e) {
            log.error("Error fetching intake desk by ID", e);
            throw new RuntimeException("Failed to fetch intake desk: " + e.getMessage(), e);
        }
    }

    /**
     * Assign leader to intake desk
     */
    public Map<String, Object> assignLeader(String intakeDeskId, String operatorId, String leaderType) {
        try {
            // Get intake desk info
            Map<String, Object> intakeDesk = getIntakeDeskById(intakeDeskId);
            String intakeDeskName = (String) intakeDesk.get("name");

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            // Update dispatch_force table
            Map<String, Object> updateData = new HashMap<>();
            if ("leader1".equals(leaderType)) {
                updateData.put("operator_leader1_id", operatorId);
            } else if ("leader2".equals(leaderType)) {
                updateData.put("operator_leader2_id", operatorId);
            } else {
                throw new IllegalArgumentException("Invalid leader type: " + leaderType);
            }

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + intakeDeskId;
            String responseBody = executePatchRequest(url, updateData, headers);

            // Update operator table - assign organization_code and organization_name
            Map<String, Object> operatorUpdate = new HashMap<>();
            operatorUpdate.put("organization_code", intakeDeskId);
            operatorUpdate.put("organization_name", intakeDeskName);

            String operatorUrl = supabaseConfig.getApiUrl() + "/operator?id=eq." + operatorId;
            executePatchRequest(operatorUrl, operatorUpdate, headers);
            log.info("Updated operator {} with organization_code={}, organization_name={}",
                    operatorId, intakeDeskId, intakeDeskName);

            JsonNode result = objectMapper.readTree(responseBody);
            JsonNode intakeDeskNode = result.isArray() ? result.get(0) : result;

            Map<String, Object> updatedIntakeDesk = new HashMap<>();
            updatedIntakeDesk.put("id", intakeDeskNode.get("id").asText());
            updatedIntakeDesk.put("name", intakeDeskNode.get("name").asText());

            String leader1Id = intakeDeskNode.has("operator_leader1_id") && !intakeDeskNode.get("operator_leader1_id").isNull()
                    ? intakeDeskNode.get("operator_leader1_id").asText() : null;
            String leader2Id = intakeDeskNode.has("operator_leader2_id") && !intakeDeskNode.get("operator_leader2_id").isNull()
                    ? intakeDeskNode.get("operator_leader2_id").asText() : null;

            updatedIntakeDesk.put("leader1_id", leader1Id);
            updatedIntakeDesk.put("leader2_id", leader2Id);

            if (leader1Id != null && !leader1Id.isEmpty()) {
                Map<String, Object> leader1 = getOperatorById(leader1Id);
                updatedIntakeDesk.put("leader1_name", leader1 != null ? leader1.get("name") : null);
            } else {
                updatedIntakeDesk.put("leader1_name", null);
            }

            if (leader2Id != null && !leader2Id.isEmpty()) {
                Map<String, Object> leader2 = getOperatorById(leader2Id);
                updatedIntakeDesk.put("leader2_name", leader2 != null ? leader2.get("name") : null);
            } else {
                updatedIntakeDesk.put("leader2_name", null);
            }

            int memberCount = countIntakeDeskMembers(intakeDeskId);
            updatedIntakeDesk.put("member_count", memberCount);

            log.info("Assigned {} to intake desk {}: {}", leaderType, intakeDeskId, operatorId);
            return updatedIntakeDesk;

        } catch (Exception e) {
            log.error("Error assigning leader to intake desk", e);
            throw new RuntimeException("Failed to assign leader: " + e.getMessage(), e);
        }
    }

    /**
     * Remove leader from intake desk
     */
    public Map<String, Object> removeLeader(String intakeDeskId, String leaderType) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            Map<String, Object> updateData = new HashMap<>();
            if ("leader1".equals(leaderType)) {
                updateData.put("operator_leader1_id", "");
            } else if ("leader2".equals(leaderType)) {
                updateData.put("operator_leader2_id", "");
            } else {
                throw new IllegalArgumentException("Invalid leader type: " + leaderType);
            }

            String url = supabaseConfig.getApiUrl() + "/dispatch_force?id=eq." + intakeDeskId;
            String responseBody = executePatchRequest(url, updateData, headers);

            JsonNode result = objectMapper.readTree(responseBody);
            JsonNode intakeDeskNode = result.isArray() ? result.get(0) : result;

            Map<String, Object> updatedIntakeDesk = new HashMap<>();
            updatedIntakeDesk.put("id", intakeDeskNode.get("id").asText());
            updatedIntakeDesk.put("name", intakeDeskNode.get("name").asText());

            String leader1Id = intakeDeskNode.has("operator_leader1_id") && !intakeDeskNode.get("operator_leader1_id").isNull()
                    ? intakeDeskNode.get("operator_leader1_id").asText() : null;
            String leader2Id = intakeDeskNode.has("operator_leader2_id") && !intakeDeskNode.get("operator_leader2_id").isNull()
                    ? intakeDeskNode.get("operator_leader2_id").asText() : null;

            updatedIntakeDesk.put("leader1_id", leader1Id);
            updatedIntakeDesk.put("leader2_id", leader2Id);

            if (leader1Id != null && !leader1Id.isEmpty()) {
                Map<String, Object> leader1 = getOperatorById(leader1Id);
                updatedIntakeDesk.put("leader1_name", leader1 != null ? leader1.get("name") : null);
            } else {
                updatedIntakeDesk.put("leader1_name", null);
            }

            if (leader2Id != null && !leader2Id.isEmpty()) {
                Map<String, Object> leader2 = getOperatorById(leader2Id);
                updatedIntakeDesk.put("leader2_name", leader2 != null ? leader2.get("name") : null);
            } else {
                updatedIntakeDesk.put("leader2_name", null);
            }

            int memberCount = countIntakeDeskMembers(intakeDeskId);
            updatedIntakeDesk.put("member_count", memberCount);

            log.info("Removed {} from intake desk {}", leaderType, intakeDeskId);
            return updatedIntakeDesk;

        } catch (Exception e) {
            log.error("Error removing leader from intake desk", e);
            throw new RuntimeException("Failed to remove leader: " + e.getMessage(), e);
        }
    }
}
