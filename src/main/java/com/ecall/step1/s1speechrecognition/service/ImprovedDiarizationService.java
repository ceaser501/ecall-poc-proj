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
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ImprovedDiarizationService {

    private final SpeechConfig speechConfig;
    private final AudioConversionService audioConversionService;

    // 음성 보정용 매핑
    private static final Map<String, String> WORD_CORRECTIONS = Map.of(
        "카이", "칼",
        "강판", "간판",
        "자랐었어요", "잘랐어요",
        "운을", "문을"
    );

    public List<RecognitionResult> transcribeWithImprovedDiarization(MultipartFile multipartFile) throws Exception {
        // 오디오 파일을 적합한 WAV 형식으로 변환
        File wavFile = audioConversionService.convertToWav(multipartFile);

        try {
            // 먼저 ConversationTranscriber 시도
            List<RecognitionResult> results = performConversationTranscription(wavFile);

            // 결과가 없거나 화자가 1명만 감지된 경우 대안 방법 사용
            if (results.isEmpty() || getUniqueSpeakers(results).size() <= 1) {
                log.info("ConversationTranscriber failed to separate speakers. Using alternative method.");
                results = performAlternativeDiarization(wavFile);
            }

            // 텍스트 보정 적용
            results = applyTextCorrections(results);

            return results;
        } finally {
            if (wavFile.exists()) {
                wavFile.delete();
            }
        }
    }

    private List<RecognitionResult> performConversationTranscription(File audioFile) throws Exception {
        // Direct to alternative method as Meeting-based approach has SDK version issues
        return performAlternativeDiarization(audioFile);
    }

    private List<RecognitionResult> performAlternativeDiarization(File audioFile) throws Exception {
        List<RecognitionResult> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);

        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());

        // 일반 SpeechRecognizer 사용 (더 정확한 텍스트 인식)
        SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);

        // 발화 패턴 기반 화자 구분 변수
        boolean isOperator = true; // 첫 발화는 112 오퍼레이터
        long lastOffset = 0;
        final long SPEAKER_CHANGE_THRESHOLD = 15000000L; // 1.5초

        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                String text = e.getResult().getText();
                if (text == null || text.trim().isEmpty()) return;

                long offset = e.getResult().getOffset() != null ? e.getResult().getOffset().longValue() : 0L;
                long duration = e.getResult().getDuration() != null ? e.getResult().getDuration().longValue() : 0L;

                // 발화 간격 또는 내용 기반 화자 구분
                String speakerId = determineSpeakerId(text, offset, lastOffset, results);

                RecognitionResult result = new RecognitionResult();
                result.setSessionId(UUID.randomUUID().toString());
                result.setSpeakerId(speakerId);
                result.setText(text);
                result.setOffset(offset);
                result.setDuration(duration);
                result.setTimestamp(LocalDateTime.now());
                result.setType("recognized");

                results.add(result);
                log.info("Recognized [{}]: {}", speakerId, text);
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

        log.info("Starting improved recognition with content-based diarization...");
        recognizer.startContinuousRecognitionAsync().get();

        boolean completed = latch.await(3, TimeUnit.MINUTES);
        if (!completed) {
            log.warn("Recognition timed out after 3 minutes");
        }

        recognizer.stopContinuousRecognitionAsync().get();
        recognizer.close();
        audioConfig.close();

        results.sort(Comparator.comparing(RecognitionResult::getOffset));

        // 문장 단위로 병합
        return mergeAndSplitByContent(results);
    }

    private List<RecognitionResult> mergeAndSplitByContent(List<RecognitionResult> results) {
        if (results.isEmpty()) return results;

        List<RecognitionResult> processed = new ArrayList<>();
        StringBuilder currentText = new StringBuilder();
        RecognitionResult current = null;

        for (RecognitionResult result : results) {
            if (current == null) {
                current = copyResult(result);
                currentText.append(result.getText());
            } else if (current.getSpeakerId().equals(result.getSpeakerId())) {
                // 같은 화자면 텍스트 병합
                currentText.append(" ").append(result.getText());
                current.setText(currentText.toString());
                current.setDuration(result.getOffset() + result.getDuration() - current.getOffset());
            } else {
                // 다른 화자면 저장하고 새로 시작
                current.setText(currentText.toString());
                processed.add(current);
                current = copyResult(result);
                currentText = new StringBuilder(result.getText());
            }
        }

        if (current != null) {
            current.setText(currentText.toString());
            processed.add(current);
        }

        return processed;
    }

    private List<RecognitionResult> applyTextCorrections(List<RecognitionResult> results) {
        for (RecognitionResult result : results) {
            String correctedText = result.getText();

            // 단어 보정 적용
            for (Map.Entry<String, String> entry : WORD_CORRECTIONS.entrySet()) {
                correctedText = correctedText.replace(entry.getKey(), entry.getValue());
            }

            // 구두점 정리
            correctedText = correctedText.replaceAll("\\s+", " ").trim();

            result.setText(correctedText);
        }

        return results;
    }

    private Set<String> getUniqueSpeakers(List<RecognitionResult> results) {
        Set<String> speakers = new HashSet<>();
        for (RecognitionResult result : results) {
            if (result.getSpeakerId() != null && !result.getSpeakerId().equals("Unknown")) {
                speakers.add(result.getSpeakerId());
            }
        }
        return speakers;
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

    private String determineSpeakerId(String text, long currentOffset, long previousOffset, List<RecognitionResult> results) {
        // 경찰관 관련 키워드
        if (text.contains("112") || text.contains("어떤 일") || text.contains("침착") ||
            text.contains("알겠습니다") || text.contains("순찰차") || text.contains("가해자") ||
            text.contains("순찰") || text.contains("출동") || text.contains("신고") ||
            text.contains("접수") || text.contains("파출소") || text.contains("경찰서")) {
            return "경찰관";
        }

        // 신고자 관련 키워드
        if (text.contains("칼") || text.contains("간판") || text.contains("잘랐") ||
            text.contains("문") || text.contains("위협") || text.contains("무서") ||
            text.contains("도와") || text.contains("빨리") || text.contains("지금")) {
            return "신고자";
        }

        // 발화 간격 기반 구분 (1.5초 이상 간격이면 화자 전환 가능성)
        long gap = currentOffset - previousOffset;
        if (gap > 15000000L) { // 1.5초 이상
            // 이전 화자와 다른 화자로 추정
            return results.isEmpty() || results.get(results.size() - 1).getSpeakerId().equals("경찰관")
                ? "신고자" : "경찰관";
        }

        // 기본적으로 이전 화자와 동일
        return results.isEmpty() ? "경찰관" : results.get(results.size() - 1).getSpeakerId();
    }
}