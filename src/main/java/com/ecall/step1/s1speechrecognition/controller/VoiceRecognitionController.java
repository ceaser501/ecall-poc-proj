package com.ecall.step1.s1speechrecognition.controller;

import com.ecall.step1.s1speechrecognition.dto.VoiceUploadResponse;
import com.ecall.step1.s1speechrecognition.model.RecognitionResult;
import com.ecall.step1.s1speechrecognition.service.*;
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

    private final EnhancedDiarizationService enhancedDiarizationService;
    private final EnhancedMultichannelService enhancedMultichannelService;
    private final HybridDiarizationService hybridDiarizationService;

    @PostMapping("/upload")
    public ResponseEntity<VoiceUploadResponse> uploadWithDiarization(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "language", defaultValue = "ko-KR") String language) {
        long startTime = System.currentTimeMillis();

        try {
            log.info("Received audio file for diarization: {}, size: {} bytes, language: {}",
                     file.getOriginalFilename(), file.getSize(), language);

            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(
                    VoiceUploadResponse.builder()
                        .success(false)
                        .error("File is empty. / 파일이 비어있습니다.")
                        .build()
                );
            }

            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return ResponseEntity.badRequest().body(
                    VoiceUploadResponse.builder()
                        .success(false)
                        .error("Only audio files are allowed. / 오디오 파일만 업로드 가능합니다.")
                        .build()
                );
            }

            List<RecognitionResult> results;

            // Choose service based on language and method
            if (language.toLowerCase().startsWith("en")) {
                // Use hybrid diarization for English (better for single channel)
                log.info("Using hybrid diarization service for English");
                results = hybridDiarizationService.transcribeWithDiarization(file);
            } else {
                // Use Korean diarization service (default)
                log.info("Using Korean diarization service");
                results = enhancedDiarizationService.transcribeWithEnhancedDiarization(file);
            }

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
                    .error("Error processing file: " + e.getMessage())
                    .build()
            );
        }
    }

    @PostMapping("/upload/english")
    public ResponseEntity<VoiceUploadResponse> uploadEnglishWithDiarization(@RequestParam("file") MultipartFile file) {
        return uploadWithDiarization(file, "en-US");
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