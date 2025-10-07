package com.ecall.step1.s1speechrecognition.service;

import com.ecall.step1.s1speechrecognition.model.RecognitionResult;
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
public class DiarizationService {

    private final SpeechConfig speechConfig;
    private final AudioConversionService audioConversionService;

    public List<RecognitionResult> transcribeWithDiarization(MultipartFile multipartFile) throws Exception {
        // Convert audio file to compatible WAV format
        File wavFile = audioConversionService.convertToWav(multipartFile);

        try {
            return performDiarization(wavFile);
        } finally {
            if (wavFile.exists()) {
                wavFile.delete();
            }
        }
    }


    private List<RecognitionResult> performDiarization(File audioFile) throws Exception {
        List<RecognitionResult> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);

        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());

        ConversationTranscriber transcriber = new ConversationTranscriber(speechConfig, audioConfig);

        transcriber.transcribed.addEventListener((s, e) -> {
            String speakerId = e.getResult().getSpeakerId();
            if (speakerId == null || speakerId.isEmpty()) {
                speakerId = "Unknown";
            }

            RecognitionResult result = new RecognitionResult();
            result.setSessionId(UUID.randomUUID().toString());
            result.setSpeakerId(speakerId);
            result.setText(e.getResult().getText());
            result.setOffset(e.getResult().getOffset().longValue());
            result.setDuration(e.getResult().getDuration().longValue());
            result.setTimestamp(LocalDateTime.now());
            result.setType("transcribed");
            result.setInterim(false);

            results.add(result);
            log.info("Transcribed [Speaker: {}]: {}", speakerId, e.getResult().getText());
        });

        transcriber.transcribing.addEventListener((s, e) -> {
            log.debug("Transcribing interim result: {}", e.getResult().getText());
        });

        transcriber.canceled.addEventListener((s, e) -> {
            if (e.getReason() == CancellationReason.Error) {
                log.error("Transcription error: {}", e.getErrorDetails());
            }
            latch.countDown();
        });

        transcriber.sessionStopped.addEventListener((s, e) -> {
            log.info("Transcription session stopped");
            latch.countDown();
        });

        log.info("Starting transcription with speaker diarization for file: {}", audioFile.getName());
        transcriber.startTranscribingAsync().get();

        boolean completed = latch.await(5, TimeUnit.MINUTES);

        if (!completed) {
            log.warn("Transcription timed out after 5 minutes");
        }

        transcriber.stopTranscribingAsync().get();
        transcriber.close();
        audioConfig.close();

        results.sort(Comparator.comparing(RecognitionResult::getOffset));

        List<RecognitionResult> processedResults = postProcessResults(results);

        log.info("Transcription completed. Total segments: {}, Speakers identified: {}",
                processedResults.size(),
                processedResults.stream().map(RecognitionResult::getSpeakerId).distinct().count());

        return processedResults;
    }

    private List<RecognitionResult> postProcessResults(List<RecognitionResult> results) {
        if (results.isEmpty()) return results;

        Map<String, String> speakerMapping = new HashMap<>();
        int speakerCounter = 1;

        for (RecognitionResult result : results) {
            String originalSpeaker = result.getSpeakerId();

            if (!speakerMapping.containsKey(originalSpeaker)) {
                if ("Unknown".equals(originalSpeaker)) {
                    speakerMapping.put(originalSpeaker, "Unknown");
                } else {
                    speakerMapping.put(originalSpeaker, String.valueOf(speakerCounter++));
                }
            }

            result.setSpeakerId(speakerMapping.get(originalSpeaker));
        }

        return mergeAdjacentSegments(results);
    }

    private List<RecognitionResult> mergeAdjacentSegments(List<RecognitionResult> results) {
        if (results.isEmpty()) return results;

        List<RecognitionResult> merged = new ArrayList<>();
        RecognitionResult current = null;

        for (RecognitionResult result : results) {
            if (current == null) {
                current = copyResult(result);
            } else if (current.getSpeakerId().equals(result.getSpeakerId()) &&
                      (result.getOffset() - (current.getOffset() + current.getDuration())) < 10000000L) {
                current.setText(current.getText() + " " + result.getText());
                current.setDuration(result.getOffset() + result.getDuration() - current.getOffset());
            } else {
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

}