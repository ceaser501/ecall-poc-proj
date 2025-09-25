package com.ecall.speech.controller;

import com.ecall.speech.service.SpeechService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 음성인식 및 발화자 분석 모듈
 * 담당자: 김태수
 *
 * 작업 내용:
 * - 발화자 인식 및 분리
 * - AI Speech 자동 구두점 처리
 * - Speaker Recognition API 연동
 */
@Slf4j
@RestController
@RequestMapping("/api/speech")
@RequiredArgsConstructor
public class SpeechController {

    private final SpeechService speechService;

    @PostMapping("/recognize")
    public Map<String, Object> recognizeSpeech(@RequestParam("audio") MultipartFile audioFile) {
        log.info("[김태수] 음성인식 처리 - 파일명: {}", audioFile.getOriginalFilename());

        // TODO: 김태수 - 여기에 음성인식 로직 구현
        return Map.of(
            "module", "speech",
            "담당자", "김태수",
            "fileName", audioFile.getOriginalFilename(),
            "status", "음성인식 처리 완료"
        );
    }

    @GetMapping("/test")
    public Map<String, Object> test() {
        return Map.of(
            "module", "Speech Recognition",
            "담당자", "김태수",
            "status", "ready"
        );
    }
}