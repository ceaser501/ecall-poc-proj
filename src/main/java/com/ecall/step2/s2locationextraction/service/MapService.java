package com.ecall.step2.s2locationextraction.service;

import com.ecall.step2.s2locationextraction.dto.LocationDto;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Service
public class MapService {

    private final WebClient kakaoNaviWebClient;
    private final WebClient kakaoLocalWebClient;
    private final String kakaoRestApiKey;

    public MapService(WebClient.Builder webClientBuilder, @Value("${KAKAO_REST_API_KEY}") String kakaoRestApiKey) {
        this.kakaoNaviWebClient = webClientBuilder.baseUrl("https://apis-navi.kakaomobility.com").build();
        this.kakaoLocalWebClient = webClientBuilder.baseUrl("https://dapi.kakao.com").build();
        this.kakaoRestApiKey = kakaoRestApiKey;
    }

    public LocationDto getGeocode(String address) {
        try {
            Map response = kakaoLocalWebClient.get()
                    .uri(uriBuilder -> uriBuilder.path("/v2/local/search/address.json")
                            .queryParam("query", address)
                            .build())
                    .header("Authorization", "KakaoAK " + kakaoRestApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map> documents = (List<Map>) response.get("documents");
            if (documents != null && !documents.isEmpty()) {
                Map addressInfo = (Map) documents.get(0).get("address");
                if (addressInfo != null) {
                    double longitude = Double.parseDouble(addressInfo.get("x").toString());
                    double latitude = Double.parseDouble(addressInfo.get("y").toString());
                    return new LocationDto(latitude, longitude);
                }
            }
        } catch (Exception e) {
            System.err.println("Error in geocoding: " + e.getMessage());
        }
        return new LocationDto(0, 0); // Return default if not found or error
    }

    public List<LocationDto> getRoute(double startLat, double startLng, double endLat, double endLng) {
        String url = String.format("/v1/directions?origin=%f,%f&destination=%f,%f", startLng, startLat, endLng, endLat);
        List<LocationDto> pathCoordinates = new ArrayList<>();

        try {
            Map response = kakaoNaviWebClient.get()
                    .uri(url)
                    .header("Authorization", "KakaoAK " + kakaoRestApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            List<Map> routes = (List<Map>) response.get("routes");
            if (routes != null && !routes.isEmpty()) {
                List<Map> sections = (List<Map>) routes.get(0).get("sections");
                if (sections != null && !sections.isEmpty()) {
                    for (Map section : sections) {
                        List<Map> roads = (List<Map>) section.get("roads");
                        if (roads != null) {
                            for (Map road : roads) {
                                List<Double> vertexes = (List<Double>) road.get("vertexes");
                                for (int i = 0; i < vertexes.size(); i += 2) {
                                    double longitude = vertexes.get(i);
                                    double latitude = vertexes.get(i + 1);
                                    pathCoordinates.add(new LocationDto(latitude, longitude));
                                }
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error in route analysis: " + e.getMessage());
        }
        return pathCoordinates;
    }
}
