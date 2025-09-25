package com.ecall.voicerecognition.service;

import com.ecall.voicerecognition.model.RecognitionResult;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamingAudioFileService {

    private final SpeechConfig speechConfig;
    private final ExecutorService executorService = Executors.newCachedThreadPool();

    /**
     * 오디오 파일을 실시간처럼 스트리밍하며 처리
     * 전화통화 시뮬레이션을 위해 청크 단위로 처리
     */
    public void streamProcessAudioFile(MultipartFile multipartFile,
                                       Consumer<RecognitionResult> resultCallback,
                                       Consumer<String> statusCallback) throws Exception {

        // Save uploaded file temporarily
        File tempFile = File.createTempFile("audio-", getFileExtension(multipartFile.getOriginalFilename()));
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }

        log.info("Starting streaming process for: {}", tempFile.getName());
        statusCallback.accept("파일 스트리밍 시작...");

        // Use push stream for real-time-like processing
        PushAudioInputStream pushStream = com.microsoft.cognitiveservices.speech.audio.AudioInputStream.createPushStream();
        AudioConfig audioConfig = AudioConfig.fromStreamInput(pushStream);

        // Create recognizer with push stream
        SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);

        // Setup continuous recognition
        setupRecognizerEvents(recognizer, resultCallback, statusCallback);

        // Start recognition
        recognizer.startContinuousRecognitionAsync().get();

        // Stream file in chunks to simulate real-time
        executorService.submit(() -> {
            try {
                streamFileInChunks(tempFile, pushStream);
            } catch (Exception e) {
                log.error("Error streaming file", e);
                statusCallback.accept("스트리밍 오류: " + e.getMessage());
            }
        });
    }

    private void streamFileInChunks(File audioFile, PushAudioInputStream pushStream) throws Exception {
        try (FileInputStream fis = new FileInputStream(audioFile)) {
            byte[] buffer = new byte[3200]; // 100ms of audio at 16kHz
            int bytesRead;
            int totalBytes = 0;
            long startTime = System.currentTimeMillis();

            while ((bytesRead = fis.read(buffer)) != -1) {
                pushStream.write(Arrays.copyOf(buffer, bytesRead));
                totalBytes += bytesRead;

                // Simulate real-time by adding small delay
                // This makes it process at 2x speed (50ms delay for 100ms audio)
                Thread.sleep(50);

                // Log progress every second
                if (System.currentTimeMillis() - startTime > 1000) {
                    log.debug("Streamed {} KB", totalBytes / 1024);
                    startTime = System.currentTimeMillis();
                }
            }

            pushStream.close();
            log.info("Finished streaming file, total bytes: {}", totalBytes);

        } catch (Exception e) {
            pushStream.close();
            throw e;
        }
    }

    private void setupRecognizerEvents(SpeechRecognizer recognizer,
                                       Consumer<RecognitionResult> resultCallback,
                                       Consumer<String> statusCallback) {

        // Handle recognized speech (final results)
        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                RecognitionResult result = new RecognitionResult();
                result.setSessionId(UUID.randomUUID().toString());
                result.setSpeakerId("Speaker"); // Simple speaker ID
                result.setText(e.getResult().getText());
                result.setOffset(e.getResult().getOffset() != null ? e.getResult().getOffset().longValue() : 0L);
                result.setDuration(e.getResult().getDuration() != null ? e.getResult().getDuration().longValue() : 0L);
                result.setTimestamp(LocalDateTime.now());
                result.setType("recognized");
                result.setInterim(false);

                resultCallback.accept(result);
                log.info("Recognized: {}", result.getText());
            }
        });

        // Handle interim results (partial recognition)
        recognizer.recognizing.addEventListener((s, e) -> {
            RecognitionResult result = new RecognitionResult();
            result.setSessionId(UUID.randomUUID().toString());
            result.setSpeakerId("Speaker");
            result.setText(e.getResult().getText());
            result.setTimestamp(LocalDateTime.now());
            result.setType("recognizing");
            result.setInterim(true);

            resultCallback.accept(result);
            log.debug("Recognizing: {}", result.getText());
        });

        // Status updates
        recognizer.sessionStarted.addEventListener((s, e) -> {
            statusCallback.accept("음성 인식 세션 시작");
            log.info("Session started");
        });

        recognizer.sessionStopped.addEventListener((s, e) -> {
            statusCallback.accept("음성 인식 완료");
            log.info("Session stopped");
        });

        recognizer.canceled.addEventListener((s, e) -> {
            if (e.getReason() == CancellationReason.Error) {
                statusCallback.accept("오류 발생: " + e.getErrorDetails());
                log.error("Recognition error: {}", e.getErrorDetails());
            } else if (e.getReason() == CancellationReason.EndOfStream) {
                statusCallback.accept("스트림 종료");
                log.info("End of stream reached");
            }
        });
    }

    /**
     * 빠른 처리를 위한 병렬 처리 방식
     * 파일을 여러 세그먼트로 나누어 동시에 처리
     */
    public CompletableFuture<List<RecognitionResult>> processInParallel(MultipartFile file, int segments) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                log.info("Starting parallel processing with {} segments", segments);

                // Save file
                File tempFile = File.createTempFile("audio-", getFileExtension(file.getOriginalFilename()));
                tempFile.deleteOnExit();

                try (FileOutputStream fos = new FileOutputStream(tempFile)) {
                    fos.write(file.getBytes());
                }

                long fileSize = tempFile.length();
                long segmentSize = fileSize / segments;

                List<CompletableFuture<List<RecognitionResult>>> futures = new ArrayList<>();

                // Process each segment in parallel
                for (int i = 0; i < segments; i++) {
                    long start = i * segmentSize;
                    long end = (i == segments - 1) ? fileSize : (i + 1) * segmentSize;

                    CompletableFuture<List<RecognitionResult>> future =
                        processSegment(tempFile, start, end, i);
                    futures.add(future);
                }

                // Wait for all segments to complete
                CompletableFuture<Void> allFutures =
                    CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));

                return allFutures.thenApply(v -> {
                    List<RecognitionResult> allResults = new ArrayList<>();
                    futures.forEach(f -> {
                        try {
                            allResults.addAll(f.get());
                        } catch (Exception e) {
                            log.error("Error getting segment results", e);
                        }
                    });

                    // Sort by offset
                    allResults.sort(Comparator.comparing(RecognitionResult::getOffset));
                    return allResults;
                }).get();

            } catch (Exception e) {
                log.error("Parallel processing failed", e);
                throw new RuntimeException(e);
            }
        }, executorService);
    }

    private CompletableFuture<List<RecognitionResult>> processSegment(File file, long start, long end, int segmentId) {
        return CompletableFuture.supplyAsync(() -> {
            List<RecognitionResult> results = new ArrayList<>();
            log.info("Processing segment {} ({} - {})", segmentId, start, end);

            // TODO: Implement actual segment processing
            // This would involve extracting the audio segment and processing it

            return results;
        }, executorService);
    }

    private String getFileExtension(String filename) {
        if (filename == null) return ".wav";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : ".wav";
    }
}