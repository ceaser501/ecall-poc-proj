package com.ecall.step1.s1speechrecognition.service;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.AudioConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
public class LanguageDetectionService {

    @Autowired
    @Qualifier("autoDetectSpeechConfig")
    private SpeechConfig autoDetectSpeechConfig;

    /**
     * 오디오 파일에서 자동으로 언어를 감지합니다.
     * @param audioFile WAV 형식의 오디오 파일
     * @return 감지된 언어 코드 (ko-KR, en-US 등)
     */
    public String detectLanguage(File audioFile) {
        try {
            log.info("오디오 파일 언어 감지 시작: {}", audioFile.getName());

            AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());

            // 자동 언어 감지를 위한 SourceLanguageConfig 생성
            AutoDetectSourceLanguageConfig autoDetectConfig = AutoDetectSourceLanguageConfig.fromLanguages(
                java.util.Arrays.asList("ko-KR", "en-US")
            );

            SpeechRecognizer recognizer = new SpeechRecognizer(
                autoDetectSpeechConfig,
                autoDetectConfig,
                audioConfig
            );

            AtomicReference<String> detectedLanguage = new AtomicReference<>("ko-KR"); // 기본값
            CountDownLatch stopLatch = new CountDownLatch(1);

            recognizer.recognized.addEventListener((s, e) -> {
                if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                    // 언어 감지 결과 추출
                    AutoDetectSourceLanguageResult languageResult =
                        AutoDetectSourceLanguageResult.fromResult(e.getResult());

                    if (languageResult != null) {
                        String language = languageResult.getLanguage();
                        if (language != null && !language.isEmpty()) {
                            detectedLanguage.set(language);
                            log.info("감지된 언어: {}", language);
                            stopLatch.countDown(); // 첫 번째 결과만 사용
                        }
                    }
                }
            });

            recognizer.sessionStopped.addEventListener((s, e) -> stopLatch.countDown());
            recognizer.canceled.addEventListener((s, e) -> {
                log.warn("언어 감지 취소됨: {}", e.getReason());
                stopLatch.countDown();
            });

            recognizer.startContinuousRecognitionAsync().get();

            // 최대 10초 대기 (짧은 샘플만 분석)
            boolean completed = stopLatch.await(10, TimeUnit.SECONDS);

            recognizer.stopContinuousRecognitionAsync().get();
            recognizer.close();
            audioConfig.close();

            String result = detectedLanguage.get();
            log.info("최종 감지된 언어: {}", result);

            return result;

        } catch (Exception e) {
            log.error("언어 감지 중 오류 발생, 기본값(ko-KR) 사용", e);
            return "ko-KR"; // 오류 시 한국어 기본값
        }
    }
}
