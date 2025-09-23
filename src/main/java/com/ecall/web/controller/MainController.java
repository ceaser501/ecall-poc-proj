package com.ecall.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 메인 컨트롤러 - API Health Check
 * E-CALL POC 프로젝트
 */
@Slf4j
@RestController
public class MainController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        log.info("Health check API 호출");

        return Map.of(
                "status", "OK",
                "service", "ecall-poc-proj",
                "timestamp", System.currentTimeMillis(),
                "team", "금웅섭, 김태수, 손장원, 전선민, 임송은"
        );
    }

    @GetMapping("/api/info")
    public Map<String, Object> projectInfo() {
        log.info("프로젝트 정보 조회");

        return Map.of(
                "project", "E-CALL POC",
                "description", "응급구조콜 자동화 시스템",
                "version", "1.0.0",
                "team", Map.of(
                        "공통", "금웅섭, 김태수, 손장원, 전선민, 임송은",
                        "음성인식", "전선민",
                        "발화자분석", "김태수",
                        "감정분석", "임송은",
                        "위치추출", "임송은",
                        "텍스트요약", "전선민"
                )
        );
    }
}