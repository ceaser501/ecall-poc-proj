package com.ecall.step1.s1speechrecognition.controller;

import com.ecall.step1.s1speechrecognition.dto.DiarizationResult;
import com.ecall.step1.s1speechrecognition.model.RecognitionResult;
import com.ecall.step1.s1speechrecognition.service.AudioFileRecognitionService;
import com.ecall.step1.s1speechrecognition.service.ClovaDiarizationService;
import com.ecall.step1.s1speechrecognition.service.OptimizedAudioFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/voice/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final OptimizedAudioFileService optimizedAudioFileService;
    private final ClovaDiarizationService clovaDiarizationService;

    @PostMapping("/legacy")
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

    @PostMapping("/clova")
    public ResponseEntity<Map<String, Object>> uploadAudioFileWithClova(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "minSpeakers", defaultValue = "1") int minSpeakers,
            @RequestParam(value = "maxSpeakers", defaultValue = "5") int maxSpeakers) {
        try {
            log.info("Clova API로 음성 파일 처리 시작 - 파일: {}, 크기: {} bytes, 화자 수: {}~{}", 
                file.getOriginalFilename(), file.getSize(), minSpeakers, maxSpeakers);

            // 파일 유효성 검사
            if (file.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "파일이 비어있습니다.",
                    "success", false
                ));
            }

            // 파일 타입 검사
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("audio/")) {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "오디오 파일만 업로드 가능합니다.",
                    "success", false
                ));
            }

            // 임시 파일로 저장
            Path tempFile = Files.createTempFile("clova_upload_", "_" + file.getOriginalFilename());
            file.transferTo(tempFile.toFile());

            try {
                // Clova API로 화자 분리 및 STT 수행
                DiarizationResult result = clovaDiarizationService.performDiarization(
                    tempFile.toFile(), minSpeakers, maxSpeakers);

                return ResponseEntity.ok(Map.of(
                    "success", true,
                    "filename", file.getOriginalFilename(),
                    "size", file.getSize(),
                    "fullText", result.getFullText(),
                    "speakerCount", result.getSpeakerCount(),
                    "speakerSegments", result.getSpeakerSegments(),
                    "transcript", formatTranscript(result)
                ));

            } finally {
                // 임시 파일 삭제
                Files.deleteIfExists(tempFile);
            }

        } catch (IOException e) {
            log.error("파일 처리 중 I/O 오류 발생", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "알 수 없는 I/O 오류";
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "파일 처리 중 오류가 발생했습니다: " + errorMsg,
                "success", false
            ));
        } catch (Exception e) {
            log.error("Clova API 처리 중 오류 발생", e);
            String errorMsg = e.getMessage() != null ? e.getMessage() : "알 수 없는 오류가 발생했습니다";
            
            // 원인 예외의 메시지도 확인
            if (e.getCause() != null && e.getCause().getMessage() != null) {
                errorMsg = e.getCause().getMessage();
            }
            
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "음성 인식 처리 중 오류가 발생했습니다: " + errorMsg,
                "success", false,
                "details", e.getClass().getSimpleName()
            ));
        }
    }

    /**
     * 화자 분리 결과를 대본 형식으로 포맷팅합니다.
     */
    private String formatTranscript(DiarizationResult result) {
        StringBuilder transcript = new StringBuilder();
        transcript.append("=== 음성 인식 대본 ===\n\n");
        
        for (DiarizationResult.SpeakerSegment segment : result.getSpeakerSegments()) {
            transcript.append(String.format("화자 %d (%.1f초 - %.1f초):\n", 
                segment.getSpeakerId() + 1, segment.getStartTime(), segment.getEndTime()));
            transcript.append(segment.getText()).append("\n\n");
        }
        
        return transcript.toString();
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