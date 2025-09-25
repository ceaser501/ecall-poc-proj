package com.ecall.step1.s3textcorrection.controller;

import com.ecall.step1.s3textcorrection.service.TextService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 텍스트 처리 모듈
 * 담당자: 전선민
 *
 * 작업 내용:
 * - 음성 → 텍스트 변환 (STT)
 * - 텍스트 요약 (Azure OpenAI)
 * - 문장 교정 및 핵심 정보 추출
 */
@Slf4j
@RestController
@RequestMapping("/api/text")
@RequiredArgsConstructor
public class TextController {

    private final TextService textService;

    @PostMapping("/stt")
    public Map<String, Object> speechToText(@RequestBody Map<String, Object> request) {
        log.info("[전선민] 음성→텍스트 변환 처리");

        // TODO: 전선민 - 여기에 STT 로직 구현
        return Map.of(
            "module", "text",
            "담당자", "전선민",
            "transcription", "긴급 상황입니다. 도움이 필요합니다.",
            "status", "STT 처리 완료"
        );
    }

    @PostMapping("/summarize")
    public Map<String, Object> summarize(@RequestBody Map<String, String> request) {
        log.info("[전선민] 텍스트 요약 처리");

        // TODO: 전선민 - 여기에 요약 로직 구현
        return Map.of(
            "module", "text",
            "담당자", "전선민",
            "summary", "긴급 구조 요청",
            "status", "요약 완료"
        );
    }

    @GetMapping("/test")
    public Map<String, Object> test() {
        return Map.of(
            "module", "Text Processing",
            "담당자", "전선민",
            "status", "ready"
        );
    }
}