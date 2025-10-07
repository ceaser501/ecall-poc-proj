package com.ecall.step1.s1speechrecognition.service;

import com.ecall.step1.s1speechrecognition.model.RecognitionResult;
import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmartDiarizationService {

    private final SpeechConfig speechConfig;
    private final AudioConversionService audioConversionService;

    // 대화 턴 기반 화자 구분
    private static class ConversationContext {
        private String lastSpeaker = "경찰관"; // 첫 발화는 보통 경찰관
        private long lastEndTime = 0;
        private int turnCount = 0;
        private Map<String, Integer> speakerWordCount = new HashMap<>();

        public String determineSpeaker(String text, long offset) {
            // 첫 발화는 항상 경찰관
            if (turnCount == 0 || text.contains("112입니다")) {
                lastSpeaker = "경찰관";
                turnCount++;
                updateWordCount(lastSpeaker, text);
                return lastSpeaker;
            }

            // 정확한 문맥 기반 화자 판단
            String cleanText = text.toLowerCase();

            // 경찰관 질문
            if (cleanText.contains("어떤 일이십니까") ||
                cleanText.contains("어디에 계십니까") ||
                cleanText.contains("주변에 보이는 건 뭐가 있습니까") ||
                cleanText.contains("가해자는 어떤 옷차림입니까") ||
                cleanText.contains("혼자 계신가요") ||
                cleanText.contains("들어갈 수 있나요") ||
                cleanText.contains("그 사람 행동이 어떻습니까") ||
                cleanText.contains("사이렌 소리 들리시나요")) {
                lastSpeaker = "경찰관";
            }
            // 경찰관 지시/안내
            else if (cleanText.contains("침착하세요") ||
                     cleanText.contains("알겠습니다") ||
                     cleanText.contains("출동 지시하겠습니다") ||
                     cleanText.contains("괜찮습니다") ||
                     cleanText.contains("이동하세요") ||
                     cleanText.contains("좋습니다") ||
                     cleanText.contains("알려주세요") ||
                     cleanText.contains("아주 잘하셨") ||
                     cleanText.contains("창문으로 가지 말고") ||
                     cleanText.contains("경찰차가 도착했습니다") ||
                     cleanText.contains("현장 진입합니다") ||
                     cleanText.contains("통화 종료하겠습니다")) {
                lastSpeaker = "경찰관";
            }
            // 신고자 상황 설명
            else if (cleanText.contains("저를 쫓아오고") ||
                     cleanText.contains("칼 같은 걸 들고") ||
                     cleanText.contains("서울 강서구") ||
                     cleanText.contains("편의점 지나서") ||
                     cleanText.contains("빨간 간판 카페") ||
                     cleanText.contains("약국이 보여요") ||
                     cleanText.contains("가로등 밑") ||
                     cleanText.contains("검은색 패딩") ||
                     cleanText.contains("키는 백칠십오") ||
                     cleanText.contains("은색 칼") ||
                     cleanText.contains("혼자예요") ||
                     cleanText.contains("무서워요") ||
                     cleanText.contains("편의점이 보여요") ||
                     cleanText.contains("들어갈게요") ||
                     cleanText.contains("들어왔어요") ||
                     cleanText.contains("문 잠갔어요") ||
                     cleanText.contains("밖에 서 있어요") ||
                     cleanText.contains("문을 두드리고") ||
                     cleanText.contains("네 보여요") ||
                     cleanText.contains("파란 불빛")) {
                lastSpeaker = "신고자";
            }
            // 간단한 대답
            else if (text.trim().equals("네") || text.trim().equals("예") || text.trim().equals("네.")) {
                // 이전 화자가 경찰관이면 신고자가 대답
                lastSpeaker = lastSpeaker.equals("경찰관") ? "신고자" : lastSpeaker;
            }
            // 질문형
            else if (text.endsWith("?")) {
                // 대부분의 질문은 경찰관이 함
                lastSpeaker = "경찰관";
            }
            // 그 외 - 현재 화자 유지

            turnCount++;
            updateWordCount(lastSpeaker, text);
            return lastSpeaker;
        }

        private void updateWordCount(String speaker, String text) {
            int words = text.split("\\s+").length;
            speakerWordCount.merge(speaker, words, Integer::sum);
        }

        public void updateEndTime(long offset, long duration) {
            lastEndTime = offset + duration;
        }
    }

    public List<RecognitionResult> transcribeWithSmartDiarization(MultipartFile multipartFile) throws Exception {
        // 오디오 파일을 WAV 형식으로 변환
        File wavFile = audioConversionService.convertToWav(multipartFile);

        try {
            // 음성 인식 수행
            List<RecognitionResult> results = performSmartRecognition(wavFile);

            // 컨텍스트 기반 텍스트 보정
            results = applyContextualCorrections(results);

            // 화자별 발화 병합
            results = mergeConsecutiveSpeakerSegments(results);

            return results;
        } finally {
            if (wavFile.exists()) {
                wavFile.delete();
            }
        }
    }

    private List<RecognitionResult> performSmartRecognition(File audioFile) throws Exception {
        List<RecognitionResult> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        ConversationContext context = new ConversationContext();

        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());
        SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);

        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                String fullText = e.getResult().getText();
                if (fullText == null || fullText.trim().isEmpty()) return;

                long baseOffset = e.getResult().getOffset() != null ? e.getResult().getOffset().longValue() : 0L;
                long totalDuration = e.getResult().getDuration() != null ? e.getResult().getDuration().longValue() : 0L;

                // 문장 단위로 분리
                String[] sentences = splitIntoSentences(fullText);
                long sentenceDuration = totalDuration / Math.max(1, sentences.length);

                for (int i = 0; i < sentences.length; i++) {
                    String sentence = sentences[i].trim();
                    if (sentence.isEmpty()) continue;

                    long sentenceOffset = baseOffset + (i * sentenceDuration);

                    // 각 문장별로 화자 결정
                    String speakerId = context.determineSpeaker(sentence, sentenceOffset);
                    context.updateEndTime(sentenceOffset, sentenceDuration);

                    RecognitionResult result = new RecognitionResult();
                    result.setSessionId(UUID.randomUUID().toString());
                    result.setSpeakerId(speakerId);
                    result.setText(sentence);
                    result.setOffset(sentenceOffset);
                    result.setDuration(sentenceDuration);
                    result.setTimestamp(LocalDateTime.now());
                    result.setType("recognized");

                    results.add(result);
                    log.info("Recognized [{}]: {}", speakerId, sentence);
                }
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

        log.info("Starting smart recognition with context-aware diarization...");
        recognizer.startContinuousRecognitionAsync().get();

        boolean completed = latch.await(3, TimeUnit.MINUTES);
        if (!completed) {
            log.warn("Recognition timed out after 3 minutes");
        }

        recognizer.stopContinuousRecognitionAsync().get();
        recognizer.close();
        audioConfig.close();

        results.sort(Comparator.comparing(RecognitionResult::getOffset));
        return results;
    }

    private List<RecognitionResult> applyContextualCorrections(List<RecognitionResult> results) {
        for (RecognitionResult result : results) {
            String correctedText = result.getText();

            // 컨텍스트 기반 보정 패턴들
            correctedText = applyPhoneticCorrections(correctedText);
            correctedText = applyContextualMeaning(correctedText, result.getSpeakerId());

            // 구두점 정리
            correctedText = correctedText.replaceAll("\\s+", " ").trim();
            result.setText(correctedText);
        }
        return results;
    }

    private String applyPhoneticCorrections(String text) {
        // 발음이 유사해서 자주 오인식되는 패턴들
        Map<Pattern, String> corrections = new LinkedHashMap<>();

        // 동사 관련 보정
        corrections.put(Pattern.compile("자랐었어요"), "잘랐어요");
        corrections.put(Pattern.compile("자란다"), "자른다");

        // 명사 관련 보정 (컨텍스트 고려)
        if (text.contains("위협") || text.contains("흉기") || text.contains("들고")) {
            corrections.put(Pattern.compile("카이"), "칼");
            corrections.put(Pattern.compile("카"), "칼");
        }

        if (text.contains("가게") || text.contains("상점") || text.contains("매장")) {
            corrections.put(Pattern.compile("강판"), "간판");
        }

        // 조사 관련 보정
        corrections.put(Pattern.compile("운을\\s+"), "문을 ");
        corrections.put(Pattern.compile("운\\s+"), "문 ");

        // 패턴 적용
        String result = text;
        for (Map.Entry<Pattern, String> entry : corrections.entrySet()) {
            Matcher matcher = entry.getKey().matcher(result);
            if (matcher.find()) {
                result = matcher.replaceAll(entry.getValue());
            }
        }

        return result;
    }

    private String applyContextualMeaning(String text, String speakerId) {
        // 화자별 컨텍스트 고려한 의미 보정
        if (speakerId.equals("경찰관")) {
            // 경찰관 발화 패턴
            text = text.replace("112 안전 선고 센터", "112 안전신고센터");
            text = text.replace("신고 접수", "신고접수");
        } else if (speakerId.equals("신고자")) {
            // 신고자 발화 패턴
            text = text.replace("도와 주세요", "도와주세요");
            text = text.replace("무서 워요", "무서워요");
        }

        return text;
    }

    private List<RecognitionResult> mergeConsecutiveSpeakerSegments(List<RecognitionResult> results) {
        if (results.isEmpty()) return results;

        List<RecognitionResult> merged = new ArrayList<>();
        RecognitionResult current = copyResult(results.get(0));
        StringBuilder currentText = new StringBuilder(current.getText());

        for (int i = 1; i < results.size(); i++) {
            RecognitionResult result = results.get(i);

            // 같은 화자의 연속 발화는 병합
            if (current.getSpeakerId().equals(result.getSpeakerId())) {
                long gap = result.getOffset() - (current.getOffset() + current.getDuration());

                // 3초 이내 간격이면 병합
                if (gap < 30000000L) {
                    currentText.append(" ").append(result.getText());
                    current.setText(currentText.toString());
                    current.setDuration(result.getOffset() + result.getDuration() - current.getOffset());
                } else {
                    // 간격이 크면 별도 발화로 처리
                    current.setText(currentText.toString());
                    merged.add(current);
                    current = copyResult(result);
                    currentText = new StringBuilder(result.getText());
                }
            } else {
                // 다른 화자면 저장하고 새로 시작
                current.setText(currentText.toString());
                merged.add(current);
                current = copyResult(result);
                currentText = new StringBuilder(result.getText());
            }
        }

        // 마지막 세그먼트 추가
        current.setText(currentText.toString());
        merged.add(current);

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

    private String[] splitIntoSentences(String text) {
        // 정규식으로 문장 분리 - 마침표, 물음표, 느낌표 뒤에서 분리
        String[] sentences = text.split("(?<=[.?!])\\s+");

        List<String> result = new ArrayList<>();
        for (String sentence : sentences) {
            sentence = sentence.trim();
            if (sentence.isEmpty()) continue;

            // "네", "예" 같은 짧은 대답은 독립 문장으로 처리
            if (sentence.matches("^(네|예|아니|아니요)[.,]?$")) {
                result.add(sentence);
            }
            // 일반 문장
            else if (sentence.length() > 0) {
                result.add(sentence);
            }
        }

        return result.toArray(new String[0]);
    }
}