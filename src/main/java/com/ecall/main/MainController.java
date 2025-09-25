package com.ecall.main;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Map;

/**
 * 통합 메인 컨트롤러
 * E-CALL POC 프로젝트
 */
@Slf4j
@Controller
public class MainController {

    @GetMapping("/")
    public String index() {
        log.info("메인 페이지 접속");
        return "redirect:/index.html";
    }

    @GetMapping("/api/health")
    @ResponseBody
    public Map<String, Object> health() {
        log.info("Health check API 호출");

        return Map.of(
                "status", "OK",
                "service", "ecall-poc-proj",
                "timestamp", System.currentTimeMillis(),
                "modules", Map.of(
                        "speech", "김태수",
                        "emotion", "임송은",
                        "location", "임송은",
                        "text", "전선민"
                )
        );
    }

    @GetMapping("/api/modules")
    @ResponseBody
    public Map<String, Object> getModuleStatus() {
        return Map.of(
                "speech", Map.of("path", "/api/speech", "담당자", "김태수", "status", "ready"),
                "emotion", Map.of("path", "/api/emotion", "담당자", "임송은", "status", "ready"),
                "location", Map.of("path", "/api/location", "담당자", "임송은", "status", "ready"),
                "text", Map.of("path", "/api/text", "담당자", "전선민", "status", "ready")
        );
    }
}