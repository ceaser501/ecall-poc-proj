package com.ecall.step1.s1speechrecognition.service;

import com.ecall.step1.s1speechrecognition.model.RecognitionResult;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnglishDiarizationService {

    private final AudioConversionService audioConversionService;

    @Autowired
    @Qualifier("englishSpeechConfig")
    private SpeechConfig englishSpeechConfig;

    public List<RecognitionResult> transcribeWithDiarization(MultipartFile multipartFile) throws Exception {
        File wavFile = audioConversionService.convertToWav(multipartFile);

        try {
            // Use regular speech recognizer with silence-based segmentation
            List<RecognitionResult> results = performSegmentedRecognition(wavFile);

            // Analyze patterns to identify speakers
            results = identifySpeakersByPattern(results);

            // Merge consecutive utterances from the same speaker
            results = mergeConsecutiveSpeakerUtterances(results);

            return results;
        } finally {
            if (wavFile.exists()) {
                wavFile.delete();
            }
        }
    }

    private List<RecognitionResult> performSegmentedRecognition(File audioFile) throws Exception {
        List<RecognitionResult> results = new ArrayList<>();

        // Configure audio for speech recognizer
        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());

        // Create speech recognizer
        SpeechRecognizer recognizer = new SpeechRecognizer(englishSpeechConfig, audioConfig);

        // Configure for continuous recognition
        CountDownLatch stopLatch = new CountDownLatch(1);

        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                String text = e.getResult().getText();
                if (text != null && !text.trim().isEmpty()) {
                    RecognitionResult result = new RecognitionResult();
                    result.setSessionId(UUID.randomUUID().toString());
                    result.setSpeakerId("Unknown"); // Will be identified later
                    result.setText(text);
                    result.setOffset(e.getResult().getOffset() != null ? e.getResult().getOffset().longValue() : 0L);
                    result.setDuration(e.getResult().getDuration() != null ? e.getResult().getDuration().longValue() : 0L);
                    result.setTimestamp(LocalDateTime.now());
                    result.setType("recognized");

                    results.add(result);
                    log.info("Recognized: {}", text);
                }
            }
        });

        recognizer.sessionStopped.addEventListener((s, e) -> {
            log.info("Recognition session stopped");
            stopLatch.countDown();
        });

        recognizer.canceled.addEventListener((s, e) -> {
            if (e.getReason() == CancellationReason.Error) {
                log.error("Recognition error: {}", e.getErrorDetails());
            }
            stopLatch.countDown();
        });

        // Start continuous recognition
        log.info("Starting continuous recognition...");
        recognizer.startContinuousRecognitionAsync().get();

        // Wait for recognition to complete
        boolean completed = stopLatch.await(5, TimeUnit.MINUTES);
        if (!completed) {
            log.warn("Recognition timed out after 5 minutes");
        }

        recognizer.stopContinuousRecognitionAsync().get();
        recognizer.close();
        audioConfig.close();

        return results;
    }

    private List<RecognitionResult> identifySpeakersByPattern(List<RecognitionResult> results) {
        if (results.isEmpty()) return results;

        // Simple pattern-based speaker identification for 911 calls
        // This is a heuristic approach - not perfect but better than nothing

        boolean currentSpeakerIsOperator = false;
        String lastSpeaker = "Unknown";

        for (int i = 0; i < results.size(); i++) {
            RecognitionResult result = results.get(i);
            String text = result.getText().toLowerCase();

            // First utterance check
            if (i == 0) {
                if (text.contains("911") || text.contains("emergency")) {
                    result.setSpeakerId("Operator");
                    currentSpeakerIsOperator = true;
                } else {
                    result.setSpeakerId("Caller");
                    currentSpeakerIsOperator = false;
                }
                lastSpeaker = result.getSpeakerId();
                continue;
            }

            // Check for clear operator phrases
            if (text.contains("calm down") || text.contains("where are you") ||
                text.contains("officers") || text.contains("on the way") ||
                text.contains("stay on the") || text.contains("you're doing") ||
                text.contains("can you describe") || text.contains("do you see") ||
                text.contains("you're safe") || text.contains("police have")) {
                result.setSpeakerId("Operator");
                currentSpeakerIsOperator = true;
            }
            // Check for clear caller phrases
            else if (text.contains("please help") || text.contains("i'm scared") ||
                     text.contains("he's") || text.contains("i can") ||
                     text.contains("i see") || text.contains("i'm") ||
                     text.contains("thank you") || text.contains("following me")) {
                result.setSpeakerId("Caller");
                currentSpeakerIsOperator = false;
            }
            // Check for questions (often from operator)
            else if (text.endsWith("?")) {
                result.setSpeakerId("Operator");
                currentSpeakerIsOperator = true;
            }
            // For ambiguous cases, alternate speakers
            else {
                if (lastSpeaker.equals("Operator")) {
                    result.setSpeakerId("Caller");
                    currentSpeakerIsOperator = false;
                } else {
                    result.setSpeakerId("Operator");
                    currentSpeakerIsOperator = true;
                }
            }

            lastSpeaker = result.getSpeakerId();
        }

        return results;
    }

    private List<RecognitionResult> mergeConsecutiveSpeakerUtterances(List<RecognitionResult> results) {
        if (results.isEmpty()) return results;

        List<RecognitionResult> merged = new ArrayList<>();
        RecognitionResult current = copyResult(results.get(0));

        for (int i = 1; i < results.size(); i++) {
            RecognitionResult next = results.get(i);

            // If same speaker and close in time (within 2 seconds), merge
            if (current.getSpeakerId().equals(next.getSpeakerId())) {
                long timeBetween = next.getOffset() - (current.getOffset() + current.getDuration());
                if (timeBetween < 20000000L) { // Less than 2 seconds (in 100-nanosecond units)
                    // Merge the text
                    current.setText(current.getText() + " " + next.getText());
                    // Extend duration to include this utterance
                    current.setDuration((next.getOffset() + next.getDuration()) - current.getOffset());
                } else {
                    // Too much time between, keep as separate
                    merged.add(current);
                    current = copyResult(next);
                }
            } else {
                // Different speaker, save current and switch
                merged.add(current);
                current = copyResult(next);
            }
        }

        // Add the last one
        merged.add(current);

        log.info("Merged {} utterances into {} speaker segments", results.size(), merged.size());
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
}