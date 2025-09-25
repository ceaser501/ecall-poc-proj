package com.ecall.step1.s1speechrecognition.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/voice")
public class VoiceRecognitionController {

    @GetMapping("/status")
    public Map<String, Object> getStatus() {
        log.info("Voice recognition status check");
        return Map.of(
            "status", "ready",
            "module", "Voice Recognition with Diarization",
            "features", Map.of(
                "stt", "Speech-to-Text",
                "diarization", "Speaker Separation",
                "realtime", "WebSocket Streaming"
            ),
            "websocket", "/ws/voice"
        );
    }

    @GetMapping("/config")
    public Map<String, Object> getConfiguration() {
        return Map.of(
            "provider", "Azure Cognitive Services",
            "region", "koreacentral",
            "language", "ko-KR",
            "features", Map.of(
                "continuousRecognition", true,
                "speakerDiarization", true,
                "interimResults", true
            )
        );
    }
}