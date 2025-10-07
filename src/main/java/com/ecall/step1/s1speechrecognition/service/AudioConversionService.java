package com.ecall.step1.s1speechrecognition.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.*;

@Slf4j
@Service
public class AudioConversionService {

    private static final AudioFormat TARGET_FORMAT = new AudioFormat(
            16000,      // Sample rate
            16,         // Sample size in bits
            1,          // Channels (mono)
            true,       // Signed
            false       // Big endian
    );

    public File convertToWav(MultipartFile multipartFile) throws IOException, UnsupportedAudioFileException {
        String originalFilename = multipartFile.getOriginalFilename();
        log.info("Converting audio file to WAV format: {}", originalFilename);

        // Save the original file temporarily
        File tempInputFile = File.createTempFile("input-", getFileExtension(originalFilename));
        tempInputFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempInputFile)) {
            fos.write(multipartFile.getBytes());
        }

        // Check if it's already a WAV file with correct format
        if (isWavFile(originalFilename)) {
            try {
                AudioInputStream audioInputStream = AudioSystem.getAudioInputStream(tempInputFile);
                AudioFormat format = audioInputStream.getFormat();

                // Check if format is already compatible
                if (isFormatCompatible(format)) {
                    log.info("File is already in compatible WAV format");
                    audioInputStream.close();
                    return tempInputFile;
                }
                audioInputStream.close();
            } catch (Exception e) {
                log.warn("Error checking WAV format, will convert: {}", e.getMessage());
            }
        }

        // Convert to standard WAV format
        File tempOutputFile = File.createTempFile("converted-", ".wav");
        tempOutputFile.deleteOnExit();

        try {
            // Read the audio file
            AudioInputStream originalStream = AudioSystem.getAudioInputStream(tempInputFile);
            AudioFormat originalFormat = originalStream.getFormat();

            log.info("Original format: {} Hz, {} bits, {} channels",
                    originalFormat.getSampleRate(),
                    originalFormat.getSampleSizeInBits(),
                    originalFormat.getChannels());

            // Convert to target format
            AudioInputStream convertedStream;

            // First convert to PCM if needed
            if (originalFormat.getEncoding() != AudioFormat.Encoding.PCM_SIGNED) {
                AudioFormat decodedFormat = new AudioFormat(
                        AudioFormat.Encoding.PCM_SIGNED,
                        originalFormat.getSampleRate(),
                        16,
                        originalFormat.getChannels(),
                        originalFormat.getChannels() * 2,
                        originalFormat.getSampleRate(),
                        false
                );
                convertedStream = AudioSystem.getAudioInputStream(decodedFormat, originalStream);
            } else {
                convertedStream = originalStream;
            }

            // Then convert to target format (16kHz, mono)
            if (!isFormatCompatible(convertedStream.getFormat())) {
                convertedStream = AudioSystem.getAudioInputStream(TARGET_FORMAT, convertedStream);
            }

            // Write the converted audio to file
            AudioSystem.write(convertedStream, AudioFileFormat.Type.WAVE, tempOutputFile);

            convertedStream.close();
            originalStream.close();

            log.info("Successfully converted to WAV format: {} bytes", tempOutputFile.length());

            // Clean up input file
            tempInputFile.delete();

            return tempOutputFile;

        } catch (Exception e) {
            log.error("Error converting audio file", e);
            tempOutputFile.delete();

            // If conversion fails, try a simpler approach
            return convertWithFallback(tempInputFile);
        }
    }

    private File convertWithFallback(File inputFile) throws IOException {
        log.info("Using fallback conversion method");

        // For now, just return the original file and let Azure handle it
        // In production, you might want to use external tools like FFmpeg
        return inputFile;
    }

    private boolean isWavFile(String filename) {
        if (filename == null) return false;
        return filename.toLowerCase().endsWith(".wav");
    }

    private boolean isFormatCompatible(AudioFormat format) {
        // Azure Speech SDK works best with 16kHz, 16-bit, mono PCM WAV
        return format.getSampleRate() == 16000 &&
               format.getSampleSizeInBits() == 16 &&
               format.getChannels() == 1 &&
               format.getEncoding() == AudioFormat.Encoding.PCM_SIGNED;
    }

    private String getFileExtension(String filename) {
        if (filename == null) return "";
        int lastDot = filename.lastIndexOf('.');
        return lastDot > 0 ? filename.substring(lastDot) : "";
    }
}