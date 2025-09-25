package com.ecall.voicerecognition.controller;

import com.ecall.voicerecognition.model.RecognitionResult;
import com.ecall.voicerecognition.service.AudioFileRecognitionService;
import com.ecall.voicerecognition.service.OptimizedAudioFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/voice/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final AudioFileRecognitionService audioFileRecognitionService;
    private final OptimizedAudioFileService optimizedAudioFileService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> uploadAudioFile(@RequestParam("file") MultipartFile file) {
        try {
            log.info("Received audio file: {}, size: {} bytes", file.getOriginalFilename(), file.getSize());

            // Validate file
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "파일이 비어있습니다.",
                    "success", false
                ));
            }

            // Check file type
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "오디오 파일만 업로드 가능합니다.",
                    "success", false
                ));
            }

            // Process audio file with optimized service for better speed
            List<RecognitionResult> results = optimizedAudioFileService.processFileOptimized(file);

            return ResponseEntity.ok(Map.of(
                "success", true,
                "filename", file.getOriginalFilename(),
                "size", file.getSize(),
                "results", results,
                "speakerCount", results.stream()
                    .map(RecognitionResult::getSpeakerId)
                    .distinct()
                    .count()
            ));

        } catch (Exception e) {
            log.error("Error processing audio file", e);
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "파일 처리 중 오류가 발생했습니다: " + e.getMessage(),
                "success", false
            ));
        }
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