package com.ecall.location.controller;

import com.ecall.location.service.LocationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 위치 추출 모듈
 * 담당자: 임송은
 *
 * 작업 내용:
 * - 발화자 위치/주소 추출
 * - Azure Maps, Kakao Maps API 연동
 * - 좌표 변환 및 지도 표시
 */
@Slf4j
@RestController
@RequestMapping("/api/location")
@RequiredArgsConstructor
public class LocationController {

    private final LocationService locationService;

    @PostMapping("/extract")
    public Map<String, Object> extractLocation(@RequestBody Map<String, String> request) {
        log.info("[임송은] 위치 추출 처리");

        // TODO: 임송은 - 여기에 위치 추출 로직 구현
        return Map.of(
            "module", "location",
            "담당자", "임송은",
            "location", "서울특별시 강남구",
            "coordinates", Map.of(
                "lat", 37.5048,
                "lng", 127.0415
            ),
            "status", "위치 추출 완료"
        );
    }

    @GetMapping("/test")
    public Map<String, Object> test() {
        return Map.of(
            "module", "Location Extraction",
            "담당자", "임송은",
            "status", "ready"
        );
    }
}