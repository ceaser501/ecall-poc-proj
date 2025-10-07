package com.ecall.step1.s1speechrecognition.controller;

import com.ecall.step1.s1speechrecognition.dto.VoiceUploadResponse;
import com.ecall.step1.s1speechrecognition.model.RecognitionResult;
import com.ecall.step1.s1speechrecognition.service.DiarizationService;
import com.ecall.step1.s1speechrecognition.service.ImprovedDiarizationService;
import com.ecall.step1.s1speechrecognition.service.SmartDiarizationService;
import com.ecall.step1.s1speechrecognition.service.EnhancedDiarizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/voice")
@RequiredArgsConstructor
public class VoiceRecognitionController {

    private final DiarizationService diarizationService;
    private final ImprovedDiarizationService improvedDiarizationService;
    private final SmartDiarizationService smartDiarizationService;
    private final EnhancedDiarizationService enhancedDiarizationService;

    @PostMapping("/upload")
    public ResponseEntity<VoiceUploadResponse> uploadWithDiarization(@RequestParam("file") MultipartFile file) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Received audio file for diarization: {}, size: {} bytes",
                     file.getOriginalFilename(), file.getSize());

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    VoiceUploadResponse.builder()
                        .success(false)
                        .error("파일이 비어있습니다.")
                        .build()
                );
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return ResponseEntity.badRequest().body(
                    VoiceUploadResponse.builder()
                        .success(false)
                        .error("오디오 파일만 업로드 가능합니다.")
                        .build()
                );
            }

            // 향상된 화자분리 서비스 사용 (대화 턴 기반)
            List<RecognitionResult> results = enhancedDiarizationService.transcribeWithEnhancedDiarization(file);

            long processingTime = System.currentTimeMillis() - startTime;

            return ResponseEntity.ok(
                VoiceUploadResponse.builder()
                    .success(true)
                    .filename(file.getOriginalFilename())
                    .size(file.getSize())
                    .results(results)
                    .speakerCount(results.stream()
                        .map(RecognitionResult::getSpeakerId)
                        .distinct()
                        .count())
                    .processingTimeMs(processingTime)
                    .build()
            );

        } catch (Exception e) {
            log.error("Error processing audio file with diarization", e);
            return ResponseEntity.internalServerError().body(
                VoiceUploadResponse.builder()
                    .success(false)
                    .error("파일 처리 중 오류가 발생했습니다: " + e.getMessage())
                    .build()
            );
        }
    }

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

    @GetMapping("/supported-formats")
    public ResponseEntity<Map<String, Object>> getSupportedFormats() {
        return ResponseEntity.ok(Map.of(
            "formats", List.of("wav", "mp3", "ogg", "m4a", "flac"),
            "maxSize", "100MB",
            "recommendations", Map.of(
                "format", "WAV",
                "sampleRate", "16000 Hz",
                "channels", "Mono or Stereo",
                "encoding", "PCM"
            )
        ));
    }
}