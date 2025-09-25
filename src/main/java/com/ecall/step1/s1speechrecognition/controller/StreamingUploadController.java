package com.ecall.step1.s1speechrecognition.controller;

import com.ecall.step1.s1speechrecognition.model.RecognitionResult;
import com.ecall.step1.s1speechrecognition.service.StreamingAudioFileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Slf4j
@RestController
@RequestMapping("/api/voice/stream")
@RequiredArgsConstructor
public class StreamingUploadController {

    private final StreamingAudioFileService streamingService;
    private final ExecutorService executorService = Executors.newCachedThreadPool();
    private final Map<String, SseEmitter> activeEmitters = new ConcurrentHashMap<>();

    /**
     * Server-Sent Events를 사용한 실시간 스트리밍 결과 전송
     * 전화통화처럼 실시간으로 결과를 받을 수 있음
     */
    @GetMapping(value = "/sse/{sessionId}", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamResults(@PathVariable String sessionId) {
        SseEmitter emitter = new SseEmitter(300000L); // 5분 타임아웃

        activeEmitters.put(sessionId, emitter);

        emitter.onCompletion(() -> {
            activeEmitters.remove(sessionId);
            log.info("SSE connection completed for session: {}", sessionId);
        });

        emitter.onTimeout(() -> {
            activeEmitters.remove(sessionId);
            log.warn("SSE connection timeout for session: {}", sessionId);
        });

        return emitter;
    }

    /**
     * 파일을 업로드하면 실시간처럼 스트리밍하며 처리
     * SSE를 통해 결과를 실시간으로 전송
     */
    @PostMapping("/upload")
    public Map<String, Object> uploadAndStream(@RequestParam("file") MultipartFile file,
                                               @RequestParam("sessionId") String sessionId) {
        try {
            log.info("Starting streaming process for file: {} ({})",
                    file.getOriginalFilename(), formatFileSize(file.getSize()));

            SseEmitter emitter = activeEmitters.get(sessionId);
            if (emitter == null) {
                return Map.of(
                    "success", false,
                    "error", "SSE 연결을 먼저 수립해주세요"
                );
            }

            // Start streaming process in background
            executorService.submit(() -> {
                try {
                    streamingService.streamProcessAudioFile(
                        file,
                        // Result callback - send each result via SSE
                        result -> sendSseEvent(emitter, "result", result),
                        // Status callback - send status updates
                        status -> sendSseEvent(emitter, "status", Map.of("message", status))
                    );

                    // Send completion event
                    sendSseEvent(emitter, "complete", Map.of("message", "처리 완료"));
                    emitter.complete();

                } catch (Exception e) {
                    log.error("Streaming process error", e);
                    sendSseEvent(emitter, "error", Map.of("error", e.getMessage()));
                    emitter.completeWithError(e);
                }
            });

            return Map.of(
                "success", true,
                "sessionId", sessionId,
                "filename", file.getOriginalFilename(),
                "size", file.getSize(),
                "message", "스트리밍 처리 시작됨"
            );

        } catch (Exception e) {
            log.error("Upload error", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    /**
     * 빠른 병렬 처리 (실시간이 아닌 빠른 일괄 처리)
     */
    @PostMapping("/parallel")
    public Map<String, Object> uploadParallel(@RequestParam("file") MultipartFile file,
                                              @RequestParam(defaultValue = "4") int segments) {
        try {
            log.info("Starting parallel processing with {} segments", segments);

            var future = streamingService.processInParallel(file, segments);

            return Map.of(
                "success", true,
                "message", "병렬 처리 시작됨",
                "segments", segments,
                "note", "결과는 SSE 또는 폴링으로 확인"
            );

        } catch (Exception e) {
            log.error("Parallel processing error", e);
            return Map.of(
                "success", false,
                "error", e.getMessage()
            );
        }
    }

    private void sendSseEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                .name(eventName)
                .data(data, MediaType.APPLICATION_JSON));
        } catch (IOException e) {
            log.error("Failed to send SSE event", e);
            emitter.completeWithError(e);
        }
    }

    private String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024 * 1024) return String.format("%.1f KB", bytes / 1024.0);
        return String.format("%.1f MB", bytes / (1024.0 * 1024));
    }
}