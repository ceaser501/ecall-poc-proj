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
        File tempFile = File.createTempFile("audio-", getFileExtension(multipartFile.getOriginalFilename()));
        tempFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempFile)) {
            fos.write(multipartFile.getBytes());
        }

        return recognizeFromFile(tempFile);
    }

    public List<RecognitionResult> recognizeFromFile(File audioFile) throws Exception {
        List<RecognitionResult> results = new ArrayList<>();
        CountDownLatch latch = new CountDownLatch(1);

        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());
        SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);

        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                RecognitionResult result = new RecognitionResult();
                result.setSessionId(UUID.randomUUID().toString());
                result.setSpeakerId("Speaker1");
                result.setText(e.getResult().getText());
                result.setOffset(e.getResult().getOffset() != null ? e.getResult().getOffset().longValue() : 0L);
                result.setDuration(e.getResult().getDuration() != null ? e.getResult().getDuration().longValue() : 0L);
                result.setTimestamp(LocalDateTime.now());
                result.setType("transcribed");
                results.add(result);
            }
        });

        recognizer.canceled.addEventListener((s, e) -> latch.countDown());
        recognizer.sessionStopped.addEventListener((s, e) -> latch.countDown());

        recognizer.startContinuousRecognitionAsync().get();
        latch.await(5, TimeUnit.MINUTES);
        recognizer.stopContinuousRecognitionAsync().get();

        recognizer.close();
        audioConfig.close();

        results.sort(Comparator.comparing(RecognitionResult::getOffset));
        return results;
    }

    private String getFileExtension(String filename) {
        if (filename == null) return ".tmp";
        int lastDot = filename.lastIndexOf('.');
        return (lastDot > 0) ? filename.substring(lastDot) : ".tmp";
    }
}
