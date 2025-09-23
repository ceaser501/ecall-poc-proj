package com.ecall.web.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
public class HelloController {

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        // Logger 사용 예시 - 다양한 로그 레벨
        log.trace("TRACE 레벨: 가장 상세한 로그");
        log.debug("DEBUG 레벨: 디버깅용 로그");
        log.info("INFO 레벨: 일반 정보 로그 - health 체크 API 호출됨");
        log.warn("WARN 레벨: 경고 메시지");
        log.error("ERROR 레벨: 에러 메시지");

        return Map.of(
                "status", "OK",
                "service", "ecall-poc-proj",
                "timestamp", System.currentTimeMillis()
        );
    }

    @GetMapping("/api/test")
    public Map<String, Object> testLogger(@RequestParam(required = false) String name) {
        // 파라미터를 포함한 로그
        log.info("테스트 API 호출됨. name 파라미터: {}", name);

        if (name == null) {
            log.warn("name 파라미터가 전달되지 않았습니다.");
        }

        try {
            // 비즈니스 로직
            log.debug("비즈니스 로직 처리 시작");

            // 여러 값을 한번에 로깅
            String userId = "user123";
            String action = "test";
            log.info("사용자 활동: userId={}, action={}, name={}", userId, action, name);

        } catch (Exception e) {
            log.error("처리 중 오류 발생", e);
        }

        return Map.of(
                "message", "Hello " + (name != null ? name : "Guest"),
                "timestamp", System.currentTimeMillis()
        );
    }
}