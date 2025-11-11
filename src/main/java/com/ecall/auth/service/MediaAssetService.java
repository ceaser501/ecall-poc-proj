package com.ecall.auth.service;

import com.ecall.auth.config.SupabaseConfig;
import com.ecall.auth.dto.MediaAssetResponse;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@Slf4j
public class MediaAssetService {

    private final SupabaseConfig supabaseConfig;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public MediaAssetService(SupabaseConfig supabaseConfig) {
        this.supabaseConfig = supabaseConfig;

        // Create RestTemplate with PATCH support
        this.restTemplate = new RestTemplate();
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setBufferRequestBody(false);

        // Enable PATCH method via reflection
        try {
            Field methodsField = HttpURLConnection.class.getDeclaredField("methods");
            methodsField.setAccessible(true);
            String[] methods = (String[]) methodsField.get(null);
            String[] newMethods = new String[methods.length + 1];
            System.arraycopy(methods, 0, newMethods, 0, methods.length);
            newMethods[methods.length] = "PATCH";
            methodsField.set(null, newMethods);
            log.info("PATCH method enabled for HttpURLConnection");
        } catch (Exception e) {
            log.warn("Failed to enable PATCH method: {}", e.getMessage());
        }

        this.restTemplate.setRequestFactory(requestFactory);
    }

    public MediaAssetResponse uploadPhoto(MultipartFile file, String operatorId) {
        try {
            // Validate file
            if (file.isEmpty()) {
                return MediaAssetResponse.builder()
                        .success(false)
                        .message("File is empty")
                        .build();
            }

            // Generate unique filename
            String originalFilename = file.getOriginalFilename();
            String extension = "";
            String fileExtension = "";
            if (originalFilename != null && originalFilename.contains(".")) {
                extension = originalFilename.substring(originalFilename.lastIndexOf("."));
                fileExtension = extension.substring(1); // Remove the dot
            } else {
                extension = ".jpg";
                fileExtension = "jpg";
            }
            String filename = "operator_photos/" + UUID.randomUUID() + extension;

            // Upload to Supabase Storage
            String storageUrl = supabaseConfig.getSupabaseUrl() + "/storage/v1/object/operator-photos/" + filename;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<byte[]> entity = new HttpEntity<>(file.getBytes(), headers);

            ResponseEntity<String> uploadResponse = restTemplate.exchange(
                    storageUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to upload file to storage: {}", uploadResponse.getBody());
                return MediaAssetResponse.builder()
                        .success(false)
                        .message("Failed to upload file to storage")
                        .build();
            }

            // Get public URL
            String publicUrl = supabaseConfig.getSupabaseUrl() + "/storage/v1/object/public/operator-photos/" + filename;

            // Create media_asset record
            String assetId = "asset-" + UUID.randomUUID().toString();

            // Determine file type (IMAGE, AUDIO, DOC)
            String contentType = file.getContentType();
            String fileType = "DOC"; // Default
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    fileType = "IMAGE";
                } else if (contentType.startsWith("audio/")) {
                    fileType = "AUDIO";
                } else if (contentType.startsWith("video/")) {
                    fileType = "VIDEO";
                }
            }

            Map<String, Object> assetData = new HashMap<>();
            assetData.put("id", assetId);
            assetData.put("file_name", originalFilename);
            assetData.put("file_path", filename);
            assetData.put("file_url", publicUrl);
            assetData.put("file_type", fileType);
            assetData.put("file_size", file.getSize());
            assetData.put("file_extension", fileExtension);
            assetData.put("upload_at", java.time.Instant.now().toString());

            HttpHeaders dbHeaders = new HttpHeaders();
            dbHeaders.setContentType(MediaType.APPLICATION_JSON);
            dbHeaders.set("apikey", supabaseConfig.getSupabaseKey());
            dbHeaders.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            dbHeaders.set("Prefer", "return=representation");

            HttpEntity<Map<String, Object>> dbEntity = new HttpEntity<>(assetData, dbHeaders);

            String dbUrl = supabaseConfig.getApiUrl() + "/media_asset";
            ResponseEntity<String> dbResponse = restTemplate.exchange(
                    dbUrl,
                    HttpMethod.POST,
                    dbEntity,
                    String.class
            );

            // Update operator photo_id
            updateOperatorPhotoId(operatorId, assetId);

            log.info("Photo uploaded successfully for operator: {}", operatorId);

            return MediaAssetResponse.builder()
                    .success(true)
                    .message("Photo uploaded successfully")
                    .id(assetId)
                    .fileUrl(publicUrl)
                    .build();

        } catch (Exception e) {
            log.error("Error uploading photo", e);
            return MediaAssetResponse.builder()
                    .success(false)
                    .message("Upload failed: " + e.getMessage())
                    .build();
        }
    }

    private void updateOperatorPhotoId(String operatorId, String photoId) {
        try {
            log.info("Attempting to update operator photo_id - operatorId: {}, photoId: {}", operatorId, photoId);

            String url = supabaseConfig.getApiUrl() + "/operator?id=eq." + operatorId;
            java.net.URL urlObj = new java.net.URL(url);
            HttpURLConnection conn = (HttpURLConnection) urlObj.openConnection();

            // Set PATCH method using reflection workaround
            try {
                conn.setRequestMethod("PATCH");
            } catch (java.net.ProtocolException e) {
                // If PATCH is not supported, use X-HTTP-Method-Override
                conn.setRequestMethod("POST");
                conn.setRequestProperty("X-HTTP-Method-Override", "PATCH");
            }

            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("apikey", supabaseConfig.getSupabaseKey());
            conn.setRequestProperty("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            conn.setRequestProperty("Prefer", "return=representation");
            conn.setDoOutput(true);

            // Create JSON body
            String jsonBody = "{\"photo_id\":\"" + photoId + "\"}";

            try (java.io.OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            int responseCode = conn.getResponseCode();
            log.info("Updated operator photo_id - Response code: {}", responseCode);

            if (responseCode >= 200 && responseCode < 300) {
                log.info("Successfully updated operator photo_id for operator: {}", operatorId);
            } else {
                // Read error response
                try (java.io.BufferedReader br = new java.io.BufferedReader(
                        new java.io.InputStreamReader(conn.getErrorStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    log.error("Failed to update operator photo_id: {}", response.toString());
                }
            }

            conn.disconnect();
        } catch (Exception e) {
            log.error("Error updating operator photo_id - operatorId: {}, photoId: {}, Error: {}",
                operatorId, photoId, e.getMessage(), e);
        }
    }

    /**
     * Save audio/media file to storage and create media_asset record
     */
    public String saveMediaAsset(String filename, String contentType, byte[] fileBytes, String uploadedBy) {
        try {
            // Generate unique filename
            String extension = "";
            String fileExtension = "";
            if (filename != null && filename.contains(".")) {
                extension = filename.substring(filename.lastIndexOf("."));
                fileExtension = extension.substring(1); // Remove the dot
            } else {
                extension = ".mp3";
                fileExtension = "mp3";
            }
            String storagePath = "audio/" + UUID.randomUUID() + extension;

            // Upload to Supabase Storage
            String storageUrl = supabaseConfig.getSupabaseUrl() + "/storage/v1/object/emergency-audio/" + storagePath;

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_OCTET_STREAM);
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<byte[]> entity = new HttpEntity<>(fileBytes, headers);

            ResponseEntity<String> uploadResponse = restTemplate.exchange(
                    storageUrl,
                    HttpMethod.POST,
                    entity,
                    String.class
            );

            if (!uploadResponse.getStatusCode().is2xxSuccessful()) {
                log.error("Failed to upload file to storage: {}", uploadResponse.getBody());
                return null;
            }

            // Get public URL
            String publicUrl = supabaseConfig.getSupabaseUrl() + "/storage/v1/object/public/emergency-audio/" + storagePath;

            // Create media_asset record
            String assetId = "asset-" + UUID.randomUUID().toString();

            // Determine file type (IMAGE, AUDIO, DOC)
            String fileType = "DOC"; // Default
            if (contentType != null) {
                if (contentType.startsWith("image/")) {
                    fileType = "IMAGE";
                } else if (contentType.startsWith("audio/")) {
                    fileType = "AUDIO";
                } else if (contentType.startsWith("video/")) {
                    fileType = "VIDEO";
                }
            }

            Map<String, Object> assetData = new HashMap<>();
            assetData.put("id", assetId);
            assetData.put("file_name", filename);
            assetData.put("file_path", storagePath);
            assetData.put("file_url", publicUrl);
            assetData.put("file_type", fileType);
            assetData.put("file_size", fileBytes.length);
            assetData.put("file_extension", fileExtension);
            assetData.put("upload_at", java.time.Instant.now().toString());

            HttpHeaders dbHeaders = new HttpHeaders();
            dbHeaders.setContentType(MediaType.APPLICATION_JSON);
            dbHeaders.set("apikey", supabaseConfig.getSupabaseKey());
            dbHeaders.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());
            dbHeaders.set("Prefer", "return=representation");

            HttpEntity<Map<String, Object>> dbEntity = new HttpEntity<>(assetData, dbHeaders);

            String dbUrl = supabaseConfig.getApiUrl() + "/media_asset";
            ResponseEntity<String> dbResponse = restTemplate.exchange(
                    dbUrl,
                    HttpMethod.POST,
                    dbEntity,
                    String.class
            );

            log.info("Media asset created successfully: {}", assetId);
            return assetId;

        } catch (Exception e) {
            log.error("Error saving media asset", e);
            return null;
        }
    }

    public String getPhotoUrl(String photoId) {
        try {
            if (photoId == null) {
                return null;
            }

            HttpHeaders headers = new HttpHeaders();
            headers.set("apikey", supabaseConfig.getSupabaseKey());
            headers.set("Authorization", "Bearer " + supabaseConfig.getSupabaseKey());

            HttpEntity<String> entity = new HttpEntity<>(headers);

            String url = supabaseConfig.getApiUrl() + "/media_asset?id=eq." + photoId;
            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            JsonNode jsonArray = objectMapper.readTree(response.getBody());
            if (!jsonArray.isEmpty()) {
                return jsonArray.get(0).get("file_url").asText();
            }

            return null;
        } catch (Exception e) {
            log.error("Error getting photo URL", e);
            return null;
        }
    }
}
