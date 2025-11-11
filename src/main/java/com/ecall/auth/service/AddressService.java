package com.ecall.auth.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@Slf4j
@RequiredArgsConstructor
public class AddressService {

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AddressDetail {
        private Double latitude;        // 위도
        private Double longitude;       // 경도
        private String roadAddress;     // 도로명주소
        private String jibunAddress;    // 지번주소
        private String postalCode;      // 우편번호
        private String region1;         // 시/도
        private String region2;         // 구
        private String region3;         // 동
        private String buildingName;    // 건물명
    }

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    // Kakao REST API Key - .env에서 가져오거나 여기에 직접 입력
    @Value("${kakao.api.key:}")
    private String kakaoApiKey;

    public String searchAddress(String query) {
        try {
            // If no API key, return original query
            if (kakaoApiKey == null || kakaoApiKey.isEmpty()) {
                log.warn("Kakao API key not configured, returning original query");
                return query;
            }

            // Try Kakao Local API - Keyword search
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", query)
                    .queryParam("size", 1)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode documents = root.get("documents");

                if (documents != null && documents.isArray() && documents.size() > 0) {
                    JsonNode firstResult = documents.get(0);

                    // Get road address or jibun address
                    String roadAddress = firstResult.has("road_address_name") ?
                            firstResult.get("road_address_name").asText() : null;
                    String address = firstResult.has("address_name") ?
                            firstResult.get("address_name").asText() : null;
                    String placeName = firstResult.has("place_name") ?
                            firstResult.get("place_name").asText() : null;

                    // Prefer road address, fallback to regular address
                    if (roadAddress != null && !roadAddress.isEmpty()) {
                        return roadAddress;
                    } else if (address != null && !address.isEmpty()) {
                        return address;
                    } else if (placeName != null && !placeName.isEmpty()) {
                        return placeName + " (" + address + ")";
                    }
                }
            }

            // If keyword search failed, try address search
            return searchByAddress(query);

        } catch (Exception e) {
            log.error("Error searching address: {}", e.getMessage());
            return query; // Return original query if search fails
        }
    }

    private String searchByAddress(String query) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/search/address.json")
                    .queryParam("query", query)
                    .queryParam("size", 1)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode documents = root.get("documents");

                if (documents != null && documents.isArray() && documents.size() > 0) {
                    JsonNode firstResult = documents.get(0);

                    // Try to get road address first
                    JsonNode roadAddress = firstResult.get("road_address");
                    if (roadAddress != null && !roadAddress.isNull()) {
                        return roadAddress.get("address_name").asText();
                    }

                    // Fallback to jibun address
                    JsonNode address = firstResult.get("address");
                    if (address != null && !address.isNull()) {
                        return address.get("address_name").asText();
                    }
                }
            }

            return query; // Return original if no results

        } catch (Exception e) {
            log.error("Error searching by address: {}", e.getMessage());
            return query;
        }
    }

    /**
     * Search address and get detailed information including coordinates, postal code, etc.
     */
    public AddressDetail searchAddressDetail(String query) {
        try {
            // If no API key, return null
            if (kakaoApiKey == null || kakaoApiKey.isEmpty()) {
                log.warn("Kakao API key not configured");
                return null;
            }

            // Try address search first (more accurate for addresses)
            AddressDetail detail = searchDetailByAddress(query);
            if (detail != null) {
                return detail;
            }

            // If address search failed, try keyword search
            return searchDetailByKeyword(query);

        } catch (Exception e) {
            log.error("Error searching address detail: {}", e.getMessage(), e);
            return null;
        }
    }

    private AddressDetail searchDetailByAddress(String query) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/search/address.json")
                    .queryParam("query", query)
                    .queryParam("size", 1)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode documents = root.get("documents");

                if (documents != null && documents.isArray() && documents.size() > 0) {
                    JsonNode firstResult = documents.get(0);

                    AddressDetail.AddressDetailBuilder builder = AddressDetail.builder();

                    // Get coordinates (x=longitude, y=latitude)
                    if (firstResult.has("x") && firstResult.has("y")) {
                        builder.longitude(firstResult.get("x").asDouble());
                        builder.latitude(firstResult.get("y").asDouble());
                    }

                    // Get road address info
                    JsonNode roadAddress = firstResult.get("road_address");
                    if (roadAddress != null && !roadAddress.isNull()) {
                        builder.roadAddress(roadAddress.get("address_name").asText());
                        if (roadAddress.has("zone_no")) {
                            builder.postalCode(roadAddress.get("zone_no").asText());
                        }
                        if (roadAddress.has("building_name")) {
                            builder.buildingName(roadAddress.get("building_name").asText());
                        }
                    }

                    // Get jibun address info
                    JsonNode address = firstResult.get("address");
                    if (address != null && !address.isNull()) {
                        builder.jibunAddress(address.get("address_name").asText());
                        builder.region1(address.get("region_1depth_name").asText()); // 시/도
                        builder.region2(address.get("region_2depth_name").asText()); // 구
                        builder.region3(address.get("region_3depth_name").asText()); // 동
                    }

                    AddressDetail detail = builder.build();
                    log.info("Address detail found: {}", detail);
                    return detail;
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Error searching detail by address: {}", e.getMessage());
            return null;
        }
    }

    private AddressDetail searchDetailByKeyword(String query) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl("https://dapi.kakao.com/v2/local/search/keyword.json")
                    .queryParam("query", query)
                    .queryParam("size", 1)
                    .build()
                    .toUriString();

            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "KakaoAK " + kakaoApiKey);
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    entity,
                    String.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                JsonNode root = objectMapper.readTree(response.getBody());
                JsonNode documents = root.get("documents");

                if (documents != null && documents.isArray() && documents.size() > 0) {
                    JsonNode firstResult = documents.get(0);

                    AddressDetail.AddressDetailBuilder builder = AddressDetail.builder();

                    // Get coordinates (x=longitude, y=latitude)
                    if (firstResult.has("x") && firstResult.has("y")) {
                        builder.longitude(Double.parseDouble(firstResult.get("x").asText()));
                        builder.latitude(Double.parseDouble(firstResult.get("y").asText()));
                    }

                    // Get road address
                    if (firstResult.has("road_address_name")) {
                        String roadAddr = firstResult.get("road_address_name").asText();
                        if (roadAddr != null && !roadAddr.isEmpty()) {
                            builder.roadAddress(roadAddr);
                        }
                    }

                    // Get jibun address
                    if (firstResult.has("address_name")) {
                        builder.jibunAddress(firstResult.get("address_name").asText());
                    }

                    // Get place name as building name
                    if (firstResult.has("place_name")) {
                        builder.buildingName(firstResult.get("place_name").asText());
                    }

                    AddressDetail detail = builder.build();
                    log.info("Address detail found via keyword: {}", detail);
                    return detail;
                }
            }

            return null;

        } catch (Exception e) {
            log.error("Error searching detail by keyword: {}", e.getMessage());
            return null;
        }
    }
}
