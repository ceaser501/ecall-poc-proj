package com.ecall.step1.s1speechrecognition.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.sound.sampled.*;
import java.io.*;

@Slf4j
@Service
public class AudioConversionService {

    @Value("${audio.conversion.ffmpeg-path:ffmpeg}")
    private String ffmpegPath;

    private static final AudioFormat TARGET_FORMAT = new AudioFormat(
            16000,      // Sample rate
            16,         // Sample size in bits
            1,          // Channels (mono)
            true,       // Signed
            false       // Big endian
    );

    public File convertToWavFromFile(File sourceFile) throws IOException, UnsupportedAudioFileException {
        String originalFilename = sourceFile.getName();
        log.info("Converting audio file to WAV format: {}", originalFilename);

        return convertFileToWav(sourceFile, originalFilename);
    }

    public File convertToWav(MultipartFile multipartFile) throws IOException, UnsupportedAudioFileException {
        String originalFilename = multipartFile.getOriginalFilename();
        log.info("Converting audio file to WAV format: {}", originalFilename);

        // Save the original file temporarily
        File tempInputFile = File.createTempFile("input-", getFileExtension(originalFilename));
        tempInputFile.deleteOnExit();

        try (FileOutputStream fos = new FileOutputStream(tempInputFile)) {
            fos.write(multipartFile.getBytes());
        }

        return convertFileToWav(tempInputFile, originalFilename);
    }

    private File convertFileToWav(File tempInputFile, String originalFilename) throws IOException, UnsupportedAudioFileException {

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
        log.info("Using fallback conversion method with FFmpeg");

        // Try to convert using FFmpeg
        File tempOutputFile = File.createTempFile("ffmpeg-converted-", ".wav");
        tempOutputFile.deleteOnExit();

        try {
            log.info("Using FFmpeg path: {}", ffmpegPath);
            
            // Check if FFmpeg is available
            ProcessBuilder checkBuilder = new ProcessBuilder(ffmpegPath, "-version");
            Process checkProcess = checkBuilder.start();
            int checkExitCode = checkProcess.waitFor();
            
            if (checkExitCode != 0) {
                log.error("FFmpeg is not available on this system");
                throw new IOException("FFmpeg is required to convert this audio format but is not installed");
            }

            // Convert using FFmpeg: input -> 16kHz, 16-bit, mono PCM WAV
            ProcessBuilder builder = new ProcessBuilder(
                    ffmpegPath,
                    "-i", inputFile.getAbsolutePath(),
                    "-ar", "16000",           // Sample rate: 16kHz
                    "-ac", "1",               // Channels: mono
                    "-sample_fmt", "s16",     // Sample format: 16-bit signed
                    "-y",                     // Overwrite output file
                    tempOutputFile.getAbsolutePath()
            );

            builder.redirectErrorStream(true);
            Process process = builder.start();

            // Log FFmpeg output
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("FFmpeg: {}", line);
                }
            }

            int exitCode = process.waitFor();
            
            if (exitCode != 0) {
                log.error("FFmpeg conversion failed with exit code: {}", exitCode);
                throw new IOException("FFmpeg conversion failed");
            }

            log.info("Successfully converted using FFmpeg: {} bytes", tempOutputFile.length());
            
            // Clean up input file
            inputFile.delete();
            
            return tempOutputFile;

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("FFmpeg conversion was interrupted", e);
            tempOutputFile.delete();
            throw new IOException("Audio conversion was interrupted", e);
        } catch (IOException e) {
            log.error("FFmpeg conversion failed", e);
            tempOutputFile.delete();
            
            // If FFmpeg fails, throw a clear error
            throw new IOException(
                "이 오디오 파일 형식은 지원되지 않습니다. " +
                "WAV, MP3 형식을 사용하거나, FFmpeg를 시스템에 설치해주세요. " +
                "FFmpeg 경로: " + ffmpegPath + " " +
                "(application.yml에서 audio.conversion.ffmpeg-path를 전체 경로로 설정하세요) " +
                "원본 파일: " + inputFile.getName() + ", 오류: " + e.getMessage(),
                e
            );
        }
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