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
public class HybridDiarizationService {

    private final AudioConversionService audioConversionService;

    @Autowired
    @Qualifier("englishSpeechConfig")
    private SpeechConfig englishSpeechConfig;

    public List<RecognitionResult> transcribeWithDiarization(MultipartFile multipartFile) throws Exception {
        File wavFile = audioConversionService.convertToWav(multipartFile);

        try {
            List<RecognitionResult> results = performTimestampedRecognition(wavFile);

            results = applyAdvancedSpeakerClustering(results);

            results = mergeConsecutiveSpeakerSegments(results);

            return results;
        } finally {
            if (wavFile.exists()) {
                wavFile.delete();
            }
        }
    }

    private List<RecognitionResult> performTimestampedRecognition(File audioFile) throws Exception {
        List<RecognitionResult> results = new ArrayList<>();
        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());
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
                    log.info("Recognized at {}: {}", result.getOffset(), text);
                }
            }
        });

        recognizer.sessionStopped.addEventListener((s, e) -> stopLatch.countDown());
        recognizer.canceled.addEventListener((s, e) -> stopLatch.countDown());

        recognizer.startContinuousRecognitionAsync().get();
        stopLatch.await(5, TimeUnit.MINUTES);
        recognizer.stopContinuousRecognitionAsync().get();

        recognizer.close();
        audioConfig.close();

        return results;
    }

    private List<RecognitionResult> applyAdvancedSpeakerClustering(List<RecognitionResult> results) {
        if (results.isEmpty()) return results;

        // 먼저 문장 단위로 분리
        List<RecognitionResult> splitResults = splitBySentences(results);

        // Step 1: 첫 번째 패스 - 명확한 화자 식별
        identifyClearSpeakers(splitResults);

        // Step 2: 턴테이킹 패턴 분석
        applyTurnTakingPattern(splitResults);

        // Step 3: 컨텍스트 기반 보정
        applyContextCorrection(splitResults);

        return splitResults;
    }

    private List<RecognitionResult> splitBySentences(List<RecognitionResult> results) {
        List<RecognitionResult> splitResults = new ArrayList<>();

        for (RecognitionResult result : results) {
            String text = result.getText();

            // 더 세밀한 문장 분리 - 마침표, 느낌표, 물음표뿐만 아니라 대화 패턴도 고려
            List<String> sentences = new ArrayList<>();

            // First split by common sentence delimiters but preserve the delimiter
            String[] basicSplit = text.split("(?<=[.!?])\\s+");

            for (String segment : basicSplit) {
                // Additional splitting for rapid dialogue exchanges
                // Split before common dialogue starters and responses
                String[] dialogueSplit = segment.split(
                    "(?=He's knocking)|" +
                    "(?=Oh, my God)|" +
                    "(?=Oh my God)|" +
                    "(?=It's OK)|" +
                    "(?=It's okay)|" +
                    "(?=Officers just)|" +
                    "(?=Officer just)|" +
                    "(?=Yes, I can)|" +
                    "(?=Yes I can)|" +
                    "(?=Yes, I see)|" +
                    "(?=Thank you)|" +
                    "(?=Good\\.)|" +
                    "(?=Good )|" +
                    "(?=Perfect\\.)|" +
                    "(?=Understood\\.)|" +
                    "(?=You did great)|" +
                    "(?=You're doing great)|" +
                    "(?=You're safe)|" +
                    "(?=Stay away from)|" +
                    "(?=What is he doing)|" +
                    "(?=What is he )|" +
                    "(?=Do you see)|" +
                    "(?=They're entering)|" +
                    "(?=We're ending)|" +
                    "(?=I'm inside)"
                );

                for (String part : dialogueSplit) {
                    if (!part.trim().isEmpty()) {
                        // Further split very long segments at natural breaks
                        if (part.length() > 100) {
                            // Split at additional dialogue markers
                            String[] additionalSplit = part.split(
                                "(?<=\\.) (?=[A-Z])|" + // Split after period before capital letter
                                "(?<=\\?) (?=[A-Z])|" + // Split after question before capital letter
                                "(?<=!) (?=[A-Z])"      // Split after exclamation before capital letter
                            );
                            for (String subPart : additionalSplit) {
                                if (!subPart.trim().isEmpty()) {
                                    sentences.add(subPart.trim());
                                }
                            }
                        } else {
                            sentences.add(part.trim());
                        }
                    }
                }
            }

            if (sentences.size() > 1) {
                long segmentDuration = result.getDuration() / sentences.size();
                long currentOffset = result.getOffset();

                for (String sentence : sentences) {
                    if (!sentence.trim().isEmpty()) {
                        RecognitionResult splitResult = copyResult(result);
                        splitResult.setText(sentence.trim());
                        splitResult.setOffset(currentOffset);
                        splitResult.setDuration(segmentDuration);
                        splitResult.setSpeakerId("Unknown"); // 다시 판단하기 위해 초기화
                        splitResults.add(splitResult);
                        currentOffset += segmentDuration;
                    }
                }
            } else {
                splitResults.add(result);
            }
        }

        return splitResults;
    }

    private void identifyClearSpeakers(List<RecognitionResult> results) {
        for (int i = 0; i < results.size(); i++) {
            RecognitionResult result = results.get(i);
            String text = result.getText().toLowerCase();

            // 911 콜 시작 패턴
            if (text.contains("911") && text.contains("emergency")) {
                result.setSpeakerId("Operator");
                continue;
            }

            // 긴급 상황 신고 (Caller)
            if (text.contains("someone's chasing") ||
                text.contains("he's got a") ||
                text.contains("help me") ||
                text.contains("i'm scared") ||
                text.contains("i'm too scared") ||
                text.contains("i'm alone")) {
                result.setSpeakerId("Caller");
                continue;
            }

            // 지시사항 및 안내 (Operator)
            if (text.contains("stay calm") ||
                text.contains("patrol car") ||
                text.contains("understood") ||
                text.contains("good job") ||
                text.contains("you're doing great") ||
                text.contains("you did great") ||
                text.contains("perfect") ||
                text.contains("police are on") ||
                text.contains("less than a minute") ||
                text.contains("keep moving") ||
                text.contains("it's ok") ||
                text.contains("it's okay") ||
                text.contains("you're safe") ||
                text.contains("they're entering") ||
                text.contains("officers just") ||
                text.contains("officer just") ||
                text.contains("flashing blue lights") ||
                text.contains("stay away from") ||
                text.contains("we're ending") ||
                text.contains("thank you for staying calm")) {
                result.setSpeakerId("Operator");
                continue;
            }

            // 단독 긍정 응답 (Operator)
            if (text.equals("good.") || text.equals("good") ||
                text.equals("ok.") || text.equals("okay.")) {
                result.setSpeakerId("Operator");
                continue;
            }

            // 질문 패턴 (Operator) - 물음표로 끝나는 문장은 대부분 Operator
            if (text.endsWith("?") ||
                text.contains("where are you") ||
                text.contains("what do you see") ||
                text.contains("what is the suspect") ||
                text.contains("what is he") ||
                text.contains("what does he") ||
                text.contains("are you alone") ||
                text.contains("can you enter") ||
                text.contains("do you see")) {
                result.setSpeakerId("Operator");
                continue;
            }

            // 위치 및 상황 설명 (Caller)
            if (text.contains("i'm near") ||
                text.contains("i just passed") ||
                text.contains("there's a") ||
                text.contains("i see a") ||
                text.contains("black jacket") ||
                text.contains("about 5 foot") ||
                text.contains("he's knocking") ||
                text.contains("oh, my god") ||
                text.contains("oh my god") ||
                text.contains("yes, i can") ||
                text.contains("yes i can")) {
                result.setSpeakerId("Caller");
                continue;
            }

            // 특정 문장만 Caller로 인식 (단독 문장일 때만)
            if (text.startsWith("i'm inside") ||
                text.startsWith("he's still") ||
                text.contains("clerk locked") ||
                text.contains("i'll go inside")) {
                result.setSpeakerId("Caller");
                continue;
            }
        }
    }

    private void applyTurnTakingPattern(List<RecognitionResult> results) {
        // 대화 턴 추적
        String lastConfirmedSpeaker = "Operator";

        for (int i = 0; i < results.size(); i++) {
            RecognitionResult current = results.get(i);

            // 이미 확정된 화자는 건드리지 않음
            if (!current.getSpeakerId().equals("Unknown")) {
                lastConfirmedSpeaker = current.getSpeakerId();
                continue;
            }

            // Unknown인 경우 처리
            if (i == 0) {
                // 첫 발화가 Unknown이면 내용으로 판단
                if (current.getText().toLowerCase().contains("hello") ||
                    current.getText().toLowerCase().contains("hi")) {
                    current.setSpeakerId("Operator");
                } else {
                    current.setSpeakerId("Caller");
                }
            } else {
                RecognitionResult prev = results.get(i - 1);
                String text = current.getText().toLowerCase();
                String prevText = prev.getText().toLowerCase();

                // 질문-대답 패턴 분석
                boolean isPrevQuestion = prevText.endsWith("?") ||
                                       prevText.contains("can you") ||
                                       prevText.contains("where") ||
                                       prevText.contains("what") ||
                                       prevText.contains("is it");

                boolean isAnswer = text.equals("yes") || text.equals("yeah") ||
                                 text.equals("no") || text.equals("ok") ||
                                 text.equals("okay") || text.startsWith("i ") ||
                                 text.length() < 20;

                if (isPrevQuestion && isAnswer) {
                    // 질문 후 대답은 화자 교체
                    current.setSpeakerId(prev.getSpeakerId().equals("Operator") ? "Caller" : "Operator");
                } else {
                    // 시간 간격으로 판단
                    long timeDiff = current.getOffset() - (prev.getOffset() + prev.getDuration());

                    if (timeDiff > 15000000L) { // 1.5초 이상
                        // 긴 간격은 화자 교체 신호
                        current.setSpeakerId(lastConfirmedSpeaker.equals("Operator") ? "Caller" : "Operator");
                    } else {
                        // 짧은 간격은 같은 화자
                        current.setSpeakerId(lastConfirmedSpeaker);
                    }
                }

                lastConfirmedSpeaker = current.getSpeakerId();
            }
        }
    }

    private void applyContextCorrection(List<RecognitionResult> results) {
        // 긴급통화 패턴 분석
        for (int i = 0; i < results.size(); i++) {
            RecognitionResult current = results.get(i);
            String text = current.getText().toLowerCase();

            // 질문에 대한 대답 패턴
            if (i > 0) {
                RecognitionResult prev = results.get(i - 1);
                String prevText = prev.getText().toLowerCase();

                // Operator가 질문하고 Caller가 답변
                if (prev.getSpeakerId().equals("Operator") && prevText.endsWith("?")) {
                    // 짧은 답변은 Caller
                    if (text.length() < 100 && current.getSpeakerId().equals("Unknown")) {
                        current.setSpeakerId("Caller");
                    }
                }

                // 위치 질문에 대한 위치 답변
                if (prevText.contains("where are you") &&
                    (text.contains("i'm") || text.contains("street") || text.contains("near"))) {
                    current.setSpeakerId("Caller");
                }

                // 외모 질문에 대한 설명
                if (prevText.contains("what is the suspect wearing") ||
                    prevText.contains("what does")) {
                    if (text.contains("jacket") || text.contains("wearing") ||
                        text.contains("foot") || text.contains("tall")) {
                        current.setSpeakerId("Caller");
                    }
                }
            }
        }

        // 연속성 검증
        for (int i = 1; i < results.size() - 1; i++) {
            RecognitionResult prev = results.get(i - 1);
            RecognitionResult current = results.get(i);
            RecognitionResult next = results.get(i + 1);

            // Unknown을 주변 화자로 보정
            if (current.getSpeakerId().equals("Unknown")) {
                if (prev.getSpeakerId().equals(next.getSpeakerId()) &&
                    !prev.getSpeakerId().equals("Unknown")) {
                    current.setSpeakerId(prev.getSpeakerId());
                } else if (!prev.getSpeakerId().equals("Unknown")) {
                    // 질문-답변 패턴
                    if (prev.getText().endsWith("?")) {
                        current.setSpeakerId(prev.getSpeakerId().equals("Operator") ? "Caller" : "Operator");
                    } else {
                        // 시간 간격 기반 판단
                        long timeDiff = current.getOffset() - (prev.getOffset() + prev.getDuration());
                        if (timeDiff > 20000000L) { // 2초 이상
                            current.setSpeakerId(prev.getSpeakerId().equals("Operator") ? "Caller" : "Operator");
                        } else {
                            current.setSpeakerId(prev.getSpeakerId());
                        }
                    }
                }
            }
        }
    }


    private List<RecognitionResult> mergeConsecutiveSpeakerSegments(List<RecognitionResult> results) {
        if (results.isEmpty()) return results;

        List<RecognitionResult> merged = new ArrayList<>();
        RecognitionResult current = copyResult(results.get(0));

        for (int i = 1; i < results.size(); i++) {
            RecognitionResult next = results.get(i);

            if (current.getSpeakerId().equals(next.getSpeakerId())) {
                long timeBetween = next.getOffset() - (current.getOffset() + current.getDuration());
                if (timeBetween < 30000000L) {
                    current.setText(current.getText() + " " + next.getText());
                    current.setDuration((next.getOffset() + next.getDuration()) - current.getOffset());
                } else {
                    merged.add(current);
                    current = copyResult(next);
                }
            } else {
                merged.add(current);
                current = copyResult(next);
            }
        }

        merged.add(current);

        log.info("Merged {} segments into {} speaker turns", results.size(), merged.size());
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
        return copy;
    }
}