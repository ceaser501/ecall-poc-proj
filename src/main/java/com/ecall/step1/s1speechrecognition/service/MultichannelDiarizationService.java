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
public class MultichannelDiarizationService {

    private final AudioConversionService audioConversionService;

    @Autowired
    @Qualifier("englishSpeechConfig")
    private SpeechConfig englishSpeechConfig;

    public List<RecognitionResult> transcribeWithMultichannelDiarization(MultipartFile multipartFile) throws Exception {
        File wavFile = audioConversionService.convertToWav(multipartFile);

        try {
            // Check if the audio is stereo/multichannel
            AudioFormat audioFormat = getAudioFormat(wavFile);
            int channels = audioFormat.getChannels();

            if (channels < 2) {
                log.warn("Audio file has only {} channel(s). Multichannel diarization requires at least 2 channels.", channels);
                throw new IllegalArgumentException("Multichannel diarization requires stereo or multichannel audio file");
            }

            log.info("Processing {}-channel audio file for diarization", channels);

            // Split channels and process each separately
            List<List<RecognitionResult>> channelResults = new ArrayList<>();

            for (int channel = 0; channel < channels; channel++) {
                log.info("Processing channel {}", channel + 1);
                File channelFile = extractChannel(wavFile, channel, channels);

                try {
                    List<RecognitionResult> results = transcribeChannel(channelFile, channel);
                    channelResults.add(results);
                } finally {
                    if (channelFile.exists()) {
                        channelFile.delete();
                    }
                }
            }

            // Merge results from all channels chronologically
            List<RecognitionResult> mergedResults = mergeChannelResults(channelResults);

            return mergedResults;
        } finally {
            if (wavFile.exists()) {
                wavFile.delete();
            }
        }
    }

    private AudioFormat getAudioFormat(File audioFile) throws Exception {
        try (javax.sound.sampled.AudioInputStream audioStream = AudioSystem.getAudioInputStream(audioFile)) {
            return audioStream.getFormat();
        }
    }

    private File extractChannel(File inputFile, int channelIndex, int totalChannels) throws Exception {
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

            // Read all audio data
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                baos.write(buffer, 0, bytesRead);
            }
            byte[] audioData = baos.toByteArray();

            // Extract specific channel
            byte[] channelData = extractChannelData(audioData, channelIndex, totalChannels,
                                                   sourceFormat.getSampleSizeInBits() / 8);

            // Write to new file
            ByteArrayInputStream bais = new ByteArrayInputStream(channelData);
            javax.sound.sampled.AudioInputStream channelStream = new javax.sound.sampled.AudioInputStream(bais, targetFormat,
                                                                 channelData.length / targetFormat.getFrameSize());

            AudioSystem.write(channelStream, AudioFileFormat.Type.WAVE, outputFile);
        }

        return outputFile;
    }

    private byte[] extractChannelData(byte[] audioData, int channelIndex, int totalChannels, int bytesPerSample) {
        ByteArrayOutputStream output = new ByteArrayOutputStream();

        int frameSize = bytesPerSample * totalChannels;
        int channelOffset = channelIndex * bytesPerSample;

        for (int i = channelOffset; i < audioData.length; i += frameSize) {
            // Extract bytes for this channel from this frame
            for (int b = 0; b < bytesPerSample && (i + b) < audioData.length; b++) {
                output.write(audioData[i + b]);
            }
        }

        return output.toByteArray();
    }

    private List<RecognitionResult> transcribeChannel(File audioFile, int channelIndex) throws Exception {
        List<RecognitionResult> results = new ArrayList<>();

        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());
        SpeechRecognizer recognizer = new SpeechRecognizer(englishSpeechConfig, audioConfig);

        CountDownLatch stopLatch = new CountDownLatch(1);

        // Determine speaker based on channel (common convention: channel 0 = caller, channel 1 = operator)
        String speakerId = (channelIndex == 0) ? "Caller" : "Operator";

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
                    log.info("Channel {} ({}): {}", channelIndex + 1, speakerId, text);
                }
            }
        });

        recognizer.sessionStopped.addEventListener((s, e) -> {
            log.info("Recognition session stopped for channel {}", channelIndex + 1);
            stopLatch.countDown();
        });

        recognizer.canceled.addEventListener((s, e) -> {
            if (e.getReason() == CancellationReason.Error) {
                log.error("Recognition error for channel {}: {}", channelIndex + 1, e.getErrorDetails());
            }
            stopLatch.countDown();
        });

        log.info("Starting continuous recognition for channel {}...", channelIndex + 1);
        recognizer.startContinuousRecognitionAsync().get();

        boolean completed = stopLatch.await(5, TimeUnit.MINUTES);
        if (!completed) {
            log.warn("Recognition timed out for channel {}", channelIndex + 1);
        }

        recognizer.stopContinuousRecognitionAsync().get();
        recognizer.close();
        audioConfig.close();

        return results;
    }

    private List<RecognitionResult> mergeChannelResults(List<List<RecognitionResult>> channelResults) {
        List<RecognitionResult> allResults = new ArrayList<>();

        // Collect all results from all channels
        for (List<RecognitionResult> channelResult : channelResults) {
            allResults.addAll(channelResult);
        }

        // Sort by offset (timestamp) to maintain chronological order
        allResults.sort((a, b) -> Long.compare(a.getOffset(), b.getOffset()));

        // Merge consecutive utterances from the same speaker
        List<RecognitionResult> mergedResults = new ArrayList<>();
        if (!allResults.isEmpty()) {
            RecognitionResult current = copyResult(allResults.get(0));

            for (int i = 1; i < allResults.size(); i++) {
                RecognitionResult next = allResults.get(i);

                // If same speaker and close in time (within 2 seconds), merge
                if (current.getSpeakerId().equals(next.getSpeakerId())) {
                    long timeBetween = next.getOffset() - (current.getOffset() + current.getDuration());
                    if (timeBetween < 20000000L) { // Less than 2 seconds
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

        log.info("Merged {} utterances from {} channels into {} speaker segments",
                allResults.size(), channelResults.size(), mergedResults.size());

        return mergedResults;
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