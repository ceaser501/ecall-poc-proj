package com.ecall.auth.service;

import com.ecall.auth.config.SupabaseConfig;
import com.ecall.auth.dto.LoginRequest;
import com.ecall.auth.dto.LoginResponse;
import com.ecall.auth.dto.OperatorRegistrationRequest;
import com.ecall.auth.dto.OperatorRegistrationResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OperatorService {

    private final SupabaseConfig supabaseConfig;
    private final RestTemplate restTemplate;
    private final MediaAssetService mediaAssetService;
    private final BCryptPasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public OperatorRegistrationResponse registerOperator(OperatorRegistrationRequest request) {
        try {
            // Validate password confirmation
            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return OperatorRegistrationResponse.builder()
                        .success(false)
                        .message("Passwords do not match")
                        .build();
            }

            // Hash password
            String hashedPassword = passwordEncoder.encode(request.getPassword());

            // Generate custom ID: op-{uuid}
            String customId = "op-" + UUID.randomUUID().toString();

            // Create operator data with hardcoded organization
            Map<String, Object> operatorData = new HashMap<>();
            operatorData.put("id", customId);
            operatorData.put("operator_id", request.getOperatorId());
            operatorData.put("password", hashedPassword);
            operatorData.put("name", request.getName());
            operatorData.put("age", request.getAge());
            operatorData.put("gender", request.getGender());
            operatorData.put("organization_code", "0000"); // 하드코딩
            operatorData.put("organization_name", "접수1팀"); // 하드코딩
            operatorData.put("role", request.getRole());
            operatorData.put("phone_number", request.getPhoneNumber());
            operatorData.put("address", request.getAddress());
            operatorData.put("address_detail", request.getAddressDetail());
            operatorData.put("join_date", java.time.LocalDate.now().toString()); // yyyy-mm-dd
            operatorData.put("photo_id", null);
            operatorData.put("is_active", true);

            // Send request to Supabase
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            headers.set("Prefer", "return=representation");

            HttpEntity<Map<String, Object>> entity = new HttpEntity<>(operatorData, headers);

            String url = supabaseConfig.getApiUrl() + "/operator";

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            log.info("Operator registered successfully: {} with id: {}", request.getName(), customId);

            return OperatorRegistrationResponse.builder()
                    .success(true)
                    .message("Registration successful")
                    .operatorId(request.getName())
                    .id(customId)
                    .build();

        } catch (Exception e) {
            log.error("Error during operator registration", e);
            return OperatorRegistrationResponse.builder()
                    .success(false)
                    .message("Registration failed: " + e.getMessage())
                    .build();
        }
    }

    public LoginResponse login(LoginRequest request) {
        try {
            // Query Supabase to find operator by operator_id
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            String url = supabaseConfig.getApiUrl() + "/operator?operator_id=eq." + request.getOperatorId();

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            // Parse response
            JsonNode jsonArray = objectMapper.readTree(response.getBody());

            if (jsonArray.isEmpty()) {
                log.warn("Login failed: Operator ID not found: {}", request.getOperatorId());
                return LoginResponse.builder()
                        .success(false)
                        .message("Invalid operator ID or password")
                        .build();
            }

            JsonNode operatorData = jsonArray.get(0);
            String storedPassword = operatorData.get("password").asText();
            boolean isActive = operatorData.get("is_active").asBoolean();

            // Check if operator is active
            if (!isActive) {
                log.warn("Login failed: Operator is inactive: {}", request.getOperatorId());
                return LoginResponse.builder()
                        .success(false)
                        .message("Account is inactive")
                        .build();
            }

            // Verify password
            if (!passwordEncoder.matches(request.getPassword(), storedPassword)) {
                log.warn("Login failed: Invalid password for operator: {}", request.getOperatorId());
                return LoginResponse.builder()
                        .success(false)
                        .message("Invalid operator ID or password")
                        .build();
            }

            // Login successful
            String name = operatorData.get("name").asText();
            String id = operatorData.get("id").asText();
            String role = operatorData.has("role") && !operatorData.get("role").isNull()
                    ? operatorData.get("role").asText() : null;
            String organizationName = operatorData.has("organization_name") && !operatorData.get("organization_name").isNull()
                    ? operatorData.get("organization_name").asText() : null;
            String joinDate = operatorData.has("join_date") && !operatorData.get("join_date").isNull()
                    ? operatorData.get("join_date").asText() : null;
            String photoId = operatorData.has("photo_id") && !operatorData.get("photo_id").isNull()
                    ? operatorData.get("photo_id").asText() : null;

            // Get photo URL if photo_id exists
            String photoUrl = photoId != null ? mediaAssetService.getPhotoUrl(photoId) : null;

            log.info("Login successful: {} (ID: {})", request.getOperatorId(), id);

            return LoginResponse.builder()
                    .success(true)
                    .message("Login successful")
                    .operatorId(request.getOperatorId())
                    .name(name)
                    .id(id)
                    .role(role)
                    .organizationName(organizationName)
                    .joinDate(joinDate)
                    .photoUrl(photoUrl)
                    .build();

        } catch (Exception e) {
            log.error("Error during login", e);
            return LoginResponse.builder()
                    .success(false)
                    .message("Login failed: " + e.getMessage())
                    .build();
        }
    }

}
