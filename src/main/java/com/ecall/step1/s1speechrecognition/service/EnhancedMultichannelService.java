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

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioFormat;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class EnhancedMultichannelService {

    private final AudioConversionService audioConversionService;

    @Autowired
    @Qualifier("englishSpeechConfig")
    private SpeechConfig englishSpeechConfig;

    public List<RecognitionResult> transcribeWithEnhancedMultichannel(MultipartFile multipartFile) throws Exception {
        File wavFile = audioConversionService.convertToWav(multipartFile);

        try {
            // Check if the audio is stereo/multichannel
            AudioFormat audioFormat = getAudioFormat(wavFile);
            int channels = audioFormat.getChannels();

            log.info("Audio format: {} channels, {} Hz, {} bits",
                    channels, audioFormat.getSampleRate(), audioFormat.getSampleSizeInBits());

            if (channels < 2) {
                log.warn("Audio file has only {} channel(s). Multichannel diarization requires at least 2 channels.", channels);
                // For mono files, try to process as single channel
                return processSingleChannel(wavFile);
            }

            // Process each channel separately with enhanced settings
            List<List<RecognitionResult>> channelResults = new ArrayList<>();

            for (int channel = 0; channel < Math.min(channels, 2); channel++) {
                log.info("Processing channel {} of {}", channel + 1, channels);
                File channelFile = extractChannelEnhanced(wavFile, channel, channels);

                try {
                    List<RecognitionResult> results = transcribeChannelEnhanced(channelFile, channel);
                    if (!results.isEmpty()) {
                        channelResults.add(results);
                        log.info("Channel {} produced {} segments", channel + 1, results.size());
                    }
                } finally {
                    if (channelFile.exists()) {
                        channelFile.delete();
                    }
                }
            }

            // Merge results from all channels
            return mergeChannelResultsEnhanced(channelResults);

        } finally {
            if (wavFile.exists()) {
                wavFile.delete();
            }
        }
    }

    private List<RecognitionResult> processSingleChannel(File wavFile) throws Exception {
        log.info("Processing single channel audio as fallback");
        List<RecognitionResult> results = new ArrayList<>();

        AudioConfig audioConfig = AudioConfig.fromWavFileInput(wavFile.getAbsolutePath());

        // Enhanced config for noisy phone audio
        englishSpeechConfig.setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "3000");
        englishSpeechConfig.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "1000");
        englishSpeechConfig.setProperty("DiarizationEnabled", "true");
        englishSpeechConfig.setProperty("DiarizationSpeakerCount", "2");

        SpeechRecognizer recognizer = new SpeechRecognizer(englishSpeechConfig, audioConfig);

        CountDownLatch stopLatch = new CountDownLatch(1);

        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                String text = e.getResult().getText();
                if (text != null && !text.trim().isEmpty()) {
                    RecognitionResult result = new RecognitionResult();
                    result.setSessionId(UUID.randomUUID().toString());
                    result.setSpeakerId("Unknown");
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

        recognizer.sessionStopped.addEventListener((s, e) -> stopLatch.countDown());
        recognizer.canceled.addEventListener((s, e) -> {
            if (e.getReason() == CancellationReason.Error) {
                log.error("Recognition error: {}", e.getErrorDetails());
            }
            stopLatch.countDown();
        });

        recognizer.startContinuousRecognitionAsync().get();
        stopLatch.await(10, TimeUnit.MINUTES);
        recognizer.stopContinuousRecognitionAsync().get();

        recognizer.close();
        audioConfig.close();

        // Apply pattern-based speaker identification
        return identifySpeakersInResults(results);
    }

    private AudioFormat getAudioFormat(File audioFile) throws Exception {
        try (javax.sound.sampled.AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile)) {
            return audioStream.getFormat();
        }
    }

    private File extractChannelEnhanced(File inputFile, int channelIndex, int totalChannels) throws Exception {
        File outputFile = File.createTempFile("channel_" + channelIndex + "_", ".wav");

        try (javax.sound.sampled.AudioInputStream inputStream = AudioSystem.getAudioInputStream(inputFile)) {
            AudioFormat sourceFormat = inputStream.getFormat();

            // Create mono format for the extracted channel
            AudioFormat targetFormat = new AudioFormat(
                sourceFormat.getEncoding(),
                sourceFormat.getSampleRate(),
                sourceFormat.getSampleSizeInBits(),
                1, // Mono
                sourceFormat.getSampleSizeInBits() / 8,
                sourceFormat.getFrameRate(),
                sourceFormat.isBigEndian()
            );

            // Read and extract channel data
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int bytesRead;

            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }

            byte[] audioData = baos.toByteArray();
            byte[] channelData = extractChannelDataEnhanced(audioData, channelIndex, totalChannels,
                                                           sourceFormat.getSampleSizeInBits() / 8);

            // Write extracted channel to file
            ByteArrayInputStream bais = new ByteArrayInputStream(channelData);
            javax.sound.sampled.AudioInputStream channelStream = new javax.sound.sampled.AudioInputStream(bais, targetFormat,
                                                                 channelData.length / targetFormat.getFrameSize());

            AudioSystem.write(channelStream, AudioFileFormat.Type.WAVE, outputFile);
            log.info("Extracted channel {} to file: {}", channelIndex, outputFile.getAbsolutePath());
        }

        return outputFile;
    }

    private byte[] extractChannelDataEnhanced(byte[] audioData, int channelIndex, int totalChannels, int bytesPerSample) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int frameSize = bytesPerSample * totalChannels;
        int channelOffset = channelIndex * bytesPerSample;

        for (int i = 0; i < audioData.length - frameSize + 1; i += frameSize) {
            // Extract bytes for this channel
            try {
                output.write(audioData, i + channelOffset, bytesPerSample);
            } catch (Exception e) {
                log.warn("Error extracting channel data at position {}: {}", i, e.getMessage());
            }
        }

        log.info("Extracted {} bytes for channel {}", output.size(), channelIndex);
        return output.toByteArray();
    }

    private List<RecognitionResult> transcribeChannelEnhanced(File audioFile, int channelIndex) throws Exception {
        List<RecognitionResult> results = new ArrayList<>();

        // Configure for noisy phone audio - reuse existing config
        SpeechConfig config = englishSpeechConfig;

        config.setSpeechRecognitionLanguage("en-US");
        config.setOutputFormat(OutputFormat.Detailed);

        // Enhanced settings for phone audio
        config.setProperty(PropertyId.SpeechServiceConnection_InitialSilenceTimeoutMs, "10000");
        config.setProperty(PropertyId.SpeechServiceConnection_EndSilenceTimeoutMs, "2000");
        config.setProperty(PropertyId.Speech_SegmentationSilenceTimeoutMs, "800");
        config.setProfanity(ProfanityOption.Raw);

        // Enable noise suppression
        config.setProperty("SpeechServiceConnection_RecoBackend", "conversation");

        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());
        SpeechRecognizer recognizer = new SpeechRecognizer(config, audioConfig);

        CountDownLatch stopLatch = new CountDownLatch(1);

        // Determine speaker based on channel
        String speakerId = (channelIndex == 0) ? "Caller" : "Operator";

        recognizer.recognizing.addEventListener((s, e) -> {
            log.debug("Recognizing channel {} ({}): {}", channelIndex, speakerId, e.getResult().getText());
        });

        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                String text = e.getResult().getText();
                if (text != null && !text.trim().isEmpty()) {
                    RecognitionResult result = new RecognitionResult();
                    result.setSessionId(UUID.randomUUID().toString());
                    result.setSpeakerId(speakerId);
                    result.setText(text);
                    result.setOffset(e.getResult().getOffset() != null ? e.getResult().getOffset().longValue() : 0L);
                    result.setDuration(e.getResult().getDuration() != null ? e.getResult().getDuration().longValue() : 0L);
                    result.setTimestamp(LocalDateTime.now());
                    result.setType("recognized");
                    result.setChannelIndex(channelIndex);

                    results.add(result);
                    log.info("Channel {} ({}): {}", channelIndex, speakerId, text);
                }
            } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                log.debug("No match for channel {}", channelIndex);
            }
        });

        recognizer.sessionStarted.addEventListener((s, e) -> {
            log.info("Session started for channel {}", channelIndex);
        });

        recognizer.sessionStopped.addEventListener((s, e) -> {
            log.info("Session stopped for channel {}", channelIndex);
            stopLatch.countDown();
        });

        recognizer.canceled.addEventListener((s, e) -> {
            if (e.getReason() == CancellationReason.Error) {
                log.error("Recognition error for channel {}: {} (Code: {})",
                         channelIndex, e.getErrorDetails(), e.getErrorCode());
            }
            stopLatch.countDown();
        });

        log.info("Starting recognition for channel {}...", channelIndex);
        recognizer.startContinuousRecognitionAsync().get(30, TimeUnit.SECONDS);

        // Wait for recognition to complete (with timeout)
        boolean completed = stopLatch.await(10, TimeUnit.MINUTES);
        if (!completed) {
            log.warn("Recognition timed out for channel {}", channelIndex);
        }

        recognizer.stopContinuousRecognitionAsync().get(30, TimeUnit.SECONDS);
        recognizer.close();
        audioConfig.close();
        config.close();

        log.info("Channel {} recognition complete with {} results", channelIndex, results.size());
        return results;
    }

    private List<RecognitionResult> mergeChannelResultsEnhanced(List<List<RecognitionResult>> channelResults) {
        if (channelResults.isEmpty()) {
            return new ArrayList<>();
        }

        List<RecognitionResult> allResults = new ArrayList<>();

        // Collect all results
        for (List<RecognitionResult> channelResult : channelResults) {
            allResults.addAll(channelResult);
        }

        // Sort by timestamp
        allResults.sort((a, b) -> Long.compare(a.getOffset(), b.getOffset()));

        // Merge consecutive utterances from same speaker
        List<RecognitionResult> mergedResults = new ArrayList<>();
        if (!allResults.isEmpty()) {
            RecognitionResult current = copyResult(allResults.get(0));

            for (int i = 1; i < allResults.size(); i++) {
                RecognitionResult next = allResults.get(i);

                // Merge if same speaker and within 2 seconds
                if (current.getSpeakerId().equals(next.getSpeakerId())) {
                    long timeBetween = next.getOffset() - (current.getOffset() + current.getDuration());
                    if (timeBetween < 20000000L) { // 2 seconds in 100ns units
                        current.setText(current.getText() + " " + next.getText());
                        current.setDuration((next.getOffset() + next.getDuration()) - current.getOffset());
                    } else {
                        mergedResults.add(current);
                        current = copyResult(next);
                    }
                } else {
                    mergedResults.add(current);
                    current = copyResult(next);
                }
            }
            mergedResults.add(current);
        }

        log.info("Merged {} segments from {} channels into {} final segments",
                allResults.size(), channelResults.size(), mergedResults.size());

        return mergedResults;
    }

    private List<RecognitionResult> identifySpeakersInResults(List<RecognitionResult> results) {
        // Simple pattern-based identification for emergency calls
        for (int i = 0; i < results.size(); i++) {
            RecognitionResult result = results.get(i);
            String text = result.getText().toLowerCase();

            // First utterance heuristic
            if (i == 0 && (text.contains("911") || text.contains("emergency"))) {
                result.setSpeakerId("Operator");
            } else if (text.contains("please help") || text.contains("help us") ||
                      text.contains("somebody") || text.contains("broke") ||
                      text.contains("scared") || text.contains("i'm") ||
                      text.contains("my brother")) {
                result.setSpeakerId("Caller");
            } else if (text.contains("stay on") || text.contains("officers") ||
                      text.contains("police") || text.contains("we're sending") ||
                      text.contains("calm") || text.contains("where are you")) {
                result.setSpeakerId("Operator");
            } else {
                // Alternate based on previous
                if (i > 0) {
                    String prevSpeaker = results.get(i - 1).getSpeakerId();
                    result.setSpeakerId(prevSpeaker.equals("Operator") ? "Caller" : "Operator");
                } else {
                    result.setSpeakerId("Unknown");
                }
            }
        }

        return results;
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
        if (original.getChannelIndex() != null) {
            copy.setChannelIndex(original.getChannelIndex());
        }
        return copy;
    }
}