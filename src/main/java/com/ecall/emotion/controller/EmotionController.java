package com.ecall.emotion.controller;

import com.ecall.emotion.service.EmotionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 감정 분석 모듈
 * 담당자: 임송은
 *
 * 작업 내용:
 * - 발화자 감정 분석
 * - 긴급도 판단
 * - 심리 상태 파악
 */
@Slf4j
@RestController
@RequestMapping("/api/emotion")
@RequiredArgsConstructor
public class EmotionController {

    private final EmotionService emotionService;

    @PostMapping("/analyze")
    public Map<String, Object> analyzeEmotion(@RequestBody Map<String, String> request) {
        log.info("[임송은] 감정 분석 처리");

        // TODO: 임송은 - 여기에 감정 분석 로직 구현
        return Map.of(
            "module", "emotion",
            "담당자", "임송은",
            "emotion", "긴급",
            "confidence", 0.85,
            "status", "감정 분석 완료"
        );
    }

    @GetMapping("/test")
    public Map<String, Object> test() {
        return Map.of(
            "module", "Emotion Analysis",
            "담당자", "임송은",
            "status", "ready"
        );
    }
}