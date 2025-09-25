package com.ecall.step1.s1speechrecognition.service;

import com.ecall.step1.s1speechrecognition.model.RecognitionResult;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
@Service
@RequiredArgsConstructor
public class OptimizedAudioFileService {

    private final SpeechConfig speechConfig;

    /**
     * 최적화된 파일 처리 - 빠른 속도 우선
     */
    public List<RecognitionResult> processFileOptimized(MultipartFile multipartFile) throws Exception {
        // Save uploaded file temporarily
        File tempFile = File.createTempFile("audio-", getFileExtension(multipartFile.getOriginalFilename()));
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }

        log.info("Processing file with optimized recognizer: {}", tempFile.getName());

        List<RecognitionResult> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        AtomicInteger speakerCounter = new AtomicInteger(0);
        String[] currentSpeaker = {"1"}; // Track current speaker
        long[] lastSpeechEnd = {0}; // Track last speech end time

        // Create audio config from file
        AudioConfig audioConfig = AudioConfig.fromWavFileInput(tempFile.getAbsolutePath());

        // Use the existing config - clone would be ideal but not available
        // So we'll reuse with optimized settings
        SpeechConfig fastConfig = speechConfig;

        // Optimize for speed
        fastConfig.setSpeechRecognitionLanguage("ko-KR");
        fastConfig.setProperty(PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "5000");
        fastConfig.setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "1000");
        fastConfig.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "1000");

        // Enable continuous recognition for better performance
        fastConfig.setProperty(PropertyId.SpeechServiceConnection_RecoLanguage, "ko-KR");

        // Create recognizer
        SpeechRecognizer recognizer = new SpeechRecognizer(fastConfig, audioConfig);

        // Set up event handlers
        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                String text = e.getResult().getText();
                if (text == null || text.trim().isEmpty()) return;

                long offset = e.getResult().getOffset() != null ? e.getResult().getOffset().longValue() : 0L;
                long duration = e.getResult().getDuration() != null ? e.getResult().getDuration().longValue() : 0L;

                // Simple speaker change detection based on silence gaps
                // If there's more than 3 seconds gap, consider it a speaker change
                if (lastSpeechEnd[0] > 0 && offset - lastSpeechEnd[0] > 30000000L) { // 3 seconds in 100-nanosecond units
                    currentSpeaker[0] = String.valueOf((Integer.parseInt(currentSpeaker[0]) % 2) + 1);
                    log.debug("Speaker change detected at offset {}", offset);
                }
                lastSpeechEnd[0] = offset + duration;

                RecognitionResult result = new RecognitionResult();
                result.setSessionId(UUID.randomUUID().toString());
                result.setSpeakerId(currentSpeaker[0]);
                result.setText(text);
                result.setOffset(offset);
                result.setDuration(duration);
                result.setTimestamp(LocalDateTime.now());
                result.setType("recognized");

                results.add(result);
                log.info("Recognized [Speaker {}]: {}", currentSpeaker[0], text);
            }
        });

        recognizer.canceled.addEventListener((s, e) -> {
            if (e.getReason() == CancellationReason.Error) {
                log.error("Recognition error: {}", e.getErrorDetails());
            }
            latch.countDown();
        });

        recognizer.sessionStopped.addEventListener((s, e) -> {
            log.info("Recognition session stopped");
            latch.countDown();
        });

        // Start recognition
        log.info("Starting optimized recognition...");
        long startTime = System.currentTimeMillis();

        recognizer.startContinuousRecognitionAsync().get();

        // Wait for completion with timeout
        boolean completed = latch.await(2, TimeUnit.MINUTES); // Reduced timeout for faster response

        if (!completed) {
            log.warn("Recognition timed out after 2 minutes");
        }

        // Stop recognition
        recognizer.stopContinuousRecognitionAsync().get();

        long processingTime = System.currentTimeMillis() - startTime;
        log.info("Recognition completed in {} seconds for {} segments",
                processingTime / 1000.0, results.size());

        recognizer.close();
        audioConfig.close();

        // Sort results by offset
        results.sort(Comparator.comparing(RecognitionResult::getOffset));

        // Post-process to merge consecutive segments from same speaker
        List<RecognitionResult> mergedResults = mergeConsecutiveSegments(results);

        return mergedResults;
    }

    /**
     * 같은 화자의 연속된 세그먼트를 병합하여 결과를 단순화
     */
    private List<RecognitionResult> mergeConsecutiveSegments(List<RecognitionResult> results) {
        if (results.isEmpty()) return results;

        List<RecognitionResult> merged = new ArrayList<>();
        RecognitionResult current = null;

        for (RecognitionResult result : results) {
            if (current == null) {
                current = copyResult(result);
            } else if (current.getSpeakerId().equals(result.getSpeakerId()) &&
                      (result.getOffset() - (current.getOffset() + current.getDuration())) < 20000000L) { // 2 seconds gap
                // Merge with current
                current.setText(current.getText() + " " + result.getText());
                current.setDuration(result.getOffset() + result.getDuration() - current.getOffset());
            } else {
                // Different speaker or large gap - save current and start new
                merged.add(current);
                current = copyResult(result);
            }
        }

        if (current != null) {
            merged.add(current);
        }

        log.info("Merged {} segments into {} segments", results.size(), merged.size());
        return merged;
    }

    private RecognitionResult copyResult(RecognitionResult original) {
        RecognitionResult copy = new RecognitionResult();
        copy.setSessionId(original.getSessionId());
        copy.setSpeakerId(original.getSpeakerId());
        copy.setText(original.getText());
        copy.setOffset(original.getOffset());
        copy.setDuration(original.getDuration());
        copy.setTimestamp(original.getTimestamp());
        copy.setType(original.getType());
        copy.setInterim(original.isInterim());
        return copy;
    }

    private String getFileExtension(String filename) {
        if (filename == null) return ".wav";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : ".wav";
    }
}