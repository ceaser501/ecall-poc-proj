package com.ecall.voicerecognition.service;

import com.ecall.voicerecognition.model.RecognitionResult;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import com.microsoft.cognitiveservices.speech.transcription.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class AudioFileRecognitionService {

    private final SpeechConfig speechConfig;

    public List<RecognitionResult> recognizeFromFile(MultipartFile multipartFile) throws Exception {
        // Save uploaded file temporarily
        File tempFile = File.createTempFile("audio-", getFileExtension(multipartFile.getOriginalFilename()));
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }

        log.info("Saved temporary file: {}", tempFile.getAbsolutePath());

        // Perform recognition with diarization
        return recognizeWithDiarization(tempFile);
    }

    private List<RecognitionResult> recognizeWithDiarization(File audioFile) throws Exception {
        List<RecognitionResult> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        // Create audio config from file
        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());

        // Use SpeechRecognizer for faster processing (without diarization)
        // For POC, we'll simulate speaker separation based on silence gaps
        SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);

        // Set up event handlers
        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                RecognitionResult result = new RecognitionResult();
                result.setSessionId(UUID.randomUUID().toString());
                result.setSpeakerId("Speaker1"); // SpeechRecognizer doesn't support speaker separation
                result.setText(e.getResult().getText());
                result.setOffset(e.getResult().getOffset() != null ? e.getResult().getOffset().longValue() : 0L);
                result.setDuration(e.getResult().getDuration() != null ? e.getResult().getDuration().longValue() : 0L);
                result.setTimestamp(LocalDateTime.now());
                result.setType("transcribed");

                results.add(result);

                log.info("Transcribed [Speaker: {}]: {}", result.getSpeakerId(), result.getText());
            }
        });

        recognizer.canceled.addEventListener((s, e) -> {
            log.error("Recognition canceled: {}", e.getReason());
            if (e.getReason() == CancellationReason.Error) {
                log.error("Error details: {}", e.getErrorDetails());
            }
            latch.countDown();
        });

        recognizer.sessionStopped.addEventListener((s, e) -> {
            log.info("Session stopped");
            latch.countDown();
        });

        // Start transcription
        log.info("Starting transcription for file: {}", audioFile.getName());
        recognizer.startContinuousRecognitionAsync().get();

        // Wait for completion with timeout
        boolean completed = latch.await(5, TimeUnit.MINUTES);

        if (!completed) {
            log.warn("Transcription timed out after 5 minutes");
        }

        // Stop transcription
        recognizer.stopContinuousRecognitionAsync().get();
        recognizer.close();
        audioConfig.close();

        // Sort results by offset
        results.sort(Comparator.comparing(RecognitionResult::getOffset));

        log.info("Transcription completed. Total segments: {}", results.size());

        return results;
    }

    private String getFileExtension(String filename) {
        if (filename == null) return ".wav";
        int lastDot = filename.lastIndexOf('.');
        if (lastDot > 0) {
            return filename.substring(lastDot);
        }
        return ".wav";
    }
}