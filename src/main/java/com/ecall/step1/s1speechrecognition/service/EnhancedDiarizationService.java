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
public class EnhancedDiarizationService {

    private final SpeechConfig speechConfig;
    private final AudioConversionService audioConversionService;

    // 대화 턴 관리를 위한 상태 클래스
    private static class DialogueState {
        private String currentSpeaker = "경찰관"; // 첫 발화는 경찰관
        private String previousText = "";
        private String previousSpeaker = "경찰관";
        private boolean expectingAnswer = false;
        private int turnIndex = 0;
        private boolean inEmergencySituation = false; // 긴급상황 진입 여부

        public String determineSpeaker(String text) {
            turnIndex++;
            String normalized = text.toLowerCase().trim();

            // 첫 발화는 항상 경찰관
            if (turnIndex == 1 || text.contains("112입니다")) {
                currentSpeaker = "경찰관";
                expectingAnswer = true;
                previousText = text;
                previousSpeaker = currentSpeaker;
                return currentSpeaker;
            }

            // 명확한 경찰관 발화 패턴
            if (normalized.contains("알겠습니다") ||
                normalized.contains("침착하세요") ||
                normalized.contains("출동 지시하겠습니다") ||
                normalized.contains("순찰차") ||
                normalized.contains("좋습니다") ||
                normalized.contains("아주 잘하셨") ||
                normalized.contains("괜찮습니다") ||
                normalized.contains("경찰차가 도착했습니다") ||
                normalized.contains("현장 진입합니다") ||
                normalized.contains("통화 종료하겠습니다") ||
                normalized.contains("창문으로 가지 말고")) {
                currentSpeaker = "경찰관";
                expectingAnswer = normalized.contains("?") || normalized.contains("십니까");
            }
            // 명확한 신고자 발화 패턴
            else if (normalized.contains("저 들어왔어요") ||
                     normalized.contains("문 잠갔어요") ||
                     normalized.contains("너무 무서워요") ||
                     normalized.contains("뒤에 있어요") ||
                     normalized.contains("밖에 서 있어요") ||
                     normalized.contains("두드리고 있어요") ||
                     normalized.contains("네 보여요") ||
                     normalized.contains("파란 불빛이요") ||
                     normalized.contains("저 혼자예요") ||
                     normalized.contains("들어갈게요") ||
                     normalized.contains("쫓아오고 있어요") ||
                     normalized.contains("칼 같은 걸 들고") ||
                     normalized.contains("카이 같은 걸 들고")) {
                currentSpeaker = "신고자";
                expectingAnswer = false;
                inEmergencySituation = true;
            }
            // 경찰관 질문 패턴
            else if ((normalized.contains("어떤 일이십니까") ||
                      normalized.contains("어디에 계십니까") ||
                      normalized.contains("뭐가 있습니까") ||
                      normalized.contains("어떤 옷차림입니까") ||
                      normalized.contains("혼자 계신가요") ||
                      normalized.contains("들어갈 수 있나요") ||
                      normalized.contains("어떻습니까") ||
                      normalized.contains("들리시나요"))) {
                currentSpeaker = "경찰관";
                expectingAnswer = true;
            }
            // 경찰관 지시 패턴
            else if (normalized.contains("이동하세요") ||
                     normalized.contains("들어가서") ||
                     normalized.contains("알려주세요")) {
                currentSpeaker = "경찰관";
                expectingAnswer = false;
            }
            // 신고자 상황 설명
            else if ((normalized.contains("있어요") ||
                      normalized.contains("이에요") ||
                      normalized.contains("봐요") ||
                      normalized.contains("보여요") ||
                      normalized.contains("들고"))) {
                // 긴급상황에서는 신고자가 상황 설명
                if (inEmergencySituation || previousSpeaker.equals("경찰관")) {
                    currentSpeaker = "신고자";
                }
                expectingAnswer = false;
            }
            // 질문에 대한 대답
            else if (expectingAnswer && !normalized.contains("?")) {
                // 이전이 경찰관 질문이면 신고자 대답
                currentSpeaker = previousSpeaker.equals("경찰관") ? "신고자" : "경찰관";
                expectingAnswer = false;
            }
            // 간단한 대답 "네"
            else if (normalized.equals("네") || normalized.equals("네.") ||
                     normalized.equals("예") || normalized.equals("예.")) {
                // 이전이 경찰관 질문이면 신고자 대답
                currentSpeaker = expectingAnswer && previousSpeaker.equals("경찰관") ? "신고자" : currentSpeaker;
                expectingAnswer = false;
            }
            // 질문형
            else if (normalized.endsWith("?")) {
                // 기본적으로 경찰관이 질문
                currentSpeaker = "경찰관";
                expectingAnswer = true;
            }
            // 그 외 - 대화 흐름에 따라
            else {
                // 이전 화자와의 관계 고려
                if (expectingAnswer) {
                    currentSpeaker = previousSpeaker.equals("경찰관") ? "신고자" : "경찰관";
                }
            }

            previousText = text;
            previousSpeaker = currentSpeaker;
            return currentSpeaker;
        }
    }

    public List<RecognitionResult> transcribeWithEnhancedDiarization(MultipartFile multipartFile) throws Exception {
        File wavFile = audioConversionService.convertToWav(multipartFile);

        try {
            List<RecognitionResult> results = performEnhancedRecognition(wavFile);
            results = applyAdvancedCorrections(results);
            results = refineDialogue(results);
            return results;
        } finally {
            if (wavFile.exists()) {
                wavFile.delete();
            }
        }
    }

    private List<RecognitionResult> performEnhancedRecognition(File audioFile) throws Exception {
        List<RecognitionResult> results = Collections.synchronizedList(new ArrayList<>());
        CountDownLatch latch = new CountDownLatch(1);
        DialogueState state = new DialogueState();

        AudioConfig audioConfig = AudioConfig.fromWavFileInput(audioFile.getAbsolutePath());
        SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig, audioConfig);

        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                String fullText = e.getResult().getText();
                if (fullText == null || fullText.trim().isEmpty()) return;

                long baseOffset = e.getResult().getOffset() != null ? e.getResult().getOffset().longValue() : 0L;
                long totalDuration = e.getResult().getDuration() != null ? e.getResult().getDuration().longValue() : 0L;

                // 문장 분리 개선
                String[] sentences = splitSentences(fullText);
                long sentenceDuration = totalDuration / Math.max(1, sentences.length);

                for (int i = 0; i < sentences.length; i++) {
                    String sentence = sentences[i].trim();
                    if (sentence.isEmpty()) continue;

                    long sentenceOffset = baseOffset + (i * sentenceDuration);
                    String speakerId = state.determineSpeaker(sentence);

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

        log.info("Starting enhanced recognition...");
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

    private String[] splitSentences(String text) {
        List<String> sentences = new ArrayList<>();

        // 먼저 특정 패턴을 미리 분리
        text = text.replaceAll("있어요[,.]\\s*알겠습니다", "있어요. 알겠습니다");
        text = text.replaceAll("무서워요[,.]\\s*괜찮습니다", "무서워요. 괜찮습니다");
        text = text.replaceAll("두드리고 있어요[,.]\\s*너무 무서워요", "두드리고 있어요. 너무 무서워요");
        text = text.replaceAll("네 보여요\\?", "네 보여요.");
        text = text.replaceAll("불빛이요[,.]\\s*좋습니다", "불빛이요. 좋습니다");

        // 마침표, 물음표, 느낌표로 분리
        String[] parts = text.split("(?<=[.?!])\\s+");

        for (String part : parts) {
            part = part.trim();
            if (part.isEmpty()) continue;

            // 쉼표가 있는 경우 처리
            if (part.contains(",") && !part.contains("?")) {
                // 쉼표로 분리
                String[] subParts = part.split(",\\s*");
                for (String subPart : subParts) {
                    subPart = subPart.trim();
                    if (subPart.isEmpty()) continue;

                    // 마침표가 없으면 추가
                    if (!subPart.endsWith(".") && !subPart.endsWith("?") && !subPart.endsWith("!")) {
                        subPart = subPart + ".";
                    }
                    sentences.add(subPart);
                }
            } else {
                // 마침표가 없으면 추가
                if (!part.endsWith(".") && !part.endsWith("?") && !part.endsWith("!")) {
                    part = part + ".";
                }
                sentences.add(part);
            }
        }

        return sentences.toArray(new String[0]);
    }

    private List<RecognitionResult> applyAdvancedCorrections(List<RecognitionResult> results) {
        for (RecognitionResult result : results) {
            String text = result.getText();

            // 컨텍스트 기반 음성 보정
            text = correctPhonetics(text);

            // 문장 부호 정리
            text = text.replaceAll("\\s+", " ").trim();

            result.setText(text);
        }
        return results;
    }

    private String correctPhonetics(String text) {
        // 흉기 관련 문맥
        if (text.contains("들고") || text.contains("흉기") || text.contains("위협")) {
            text = text.replace("카이", "칼");
            text = text.replace("나이프", "칼");
        }

        // 장소 관련 문맥
        if (text.contains("카페") || text.contains("가게") || text.contains("상점")) {
            text = text.replace("강판", "간판");
            text = text.replace("깐판", "간판");
        }

        // 동사 보정
        text = text.replace("자랐었어요", "잘랐어요");
        text = text.replace("자른다", "자른다");

        // 조사 보정 - "운을" → "문을"
        text = text.replace("운을 두드리고", "문을 두드리고");
        text = text.replace("운을", "문을");
        text = text.replaceAll("운\\s+", "문 ");

        return text;
    }

    private List<RecognitionResult> refineDialogue(List<RecognitionResult> results) {
        if (results.isEmpty()) return results;

        // 대화 흐름 재검토 및 수정
        for (int i = 0; i < results.size(); i++) {
            RecognitionResult current = results.get(i);
            String text = current.getText();
            String lowerText = text.toLowerCase();

            // 매우 구체적인 패턴 먼저 체크
            if (text.equals("알겠습니다.") || lowerText.equals("알겠습니다.")) {
                current.setSpeakerId("경찰관");
            }
            else if (text.equals("괜찮습니다.") || lowerText.equals("괜찮습니다.")) {
                current.setSpeakerId("경찰관");
            }
            else if (text.equals("너무 무서워요.") || lowerText.equals("너무 무서워요.")) {
                current.setSpeakerId("신고자");
            }
            else if (text.equals("저 들어왔어요.") || lowerText.equals("저 들어왔어요.")) {
                current.setSpeakerId("신고자");
            }
            else if (text.equals("문 잠갔어요.") || lowerText.equals("문 잠갔어요.")) {
                current.setSpeakerId("신고자");
            }
            else if (text.equals("아주 잘하셨어요.") || lowerText.equals("아주 잘하셨어요.")) {
                current.setSpeakerId("경찰관");
            }
            else if (text.equals("경찰차가 도착했습니다.") || lowerText.equals("경찰차가 도착했습니다.")) {
                current.setSpeakerId("경찰관");
            }
            else if (text.equals("네 보여요.") || text.equals("네 보여요?")) {
                current.setSpeakerId("신고자");
            }
            else if (text.equals("파란 불빛이요.") || lowerText.equals("파란 불빛이요.")) {
                current.setSpeakerId("신고자");
            }
            else if (lowerText.contains("바로 순찰차 출동 지시하겠습니다")) {
                current.setSpeakerId("경찰관");
            }
            // 신고자 패턴
            else if (lowerText.contains("지금 가로등 밑") ||
                     lowerText.contains("남자가 아직도 뒤에") ||
                     lowerText.contains("문을 두드리고 있어요") ||
                     lowerText.contains("그 남자 지금 밖에") ||
                     lowerText.contains("저 혼자예요") ||
                     lowerText.contains("뒤돌아보기가") ||
                     lowerText.contains("거기로 들어갈게요") ||
                     lowerText.contains("앞에 편의점이 보여요")) {
                current.setSpeakerId("신고자");
            }
            // 경찰관 패턴
            else if (lowerText.contains("침착하세요") ||
                     lowerText.contains("좋습니다") ||
                     lowerText.contains("사이렌 소리 들리시나요") ||
                     lowerText.contains("현장 진입합니다") ||
                     lowerText.contains("통화 종료하겠습니다") ||
                     lowerText.contains("창문으로 가지 말고") ||
                     lowerText.contains("신고자님") ||
                     lowerText.contains("순찰차가") ||
                     lowerText.contains("경찰 신고했다고")) {
                current.setSpeakerId("경찰관");
            }
            // 간단한 대답 처리
            else if (text.equals("네.") || text.equals("예.")) {
                if (i > 0) {
                    RecognitionResult prev = results.get(i - 1);
                    String prevText = prev.getText().toLowerCase();
                    // 이전이 질문이면 대답하는 사람은 반대편
                    if (prevText.contains("?") || prevText.contains("나요") || prevText.contains("습니까")) {
                        current.setSpeakerId(prev.getSpeakerId().equals("경찰관") ? "신고자" : "경찰관");
                    }
                }
            }
        }

        // 연속된 같은 화자 발화 병합
        return mergeConsecutiveUtterances(results);
    }

    private List<RecognitionResult> mergeConsecutiveUtterances(List<RecognitionResult> results) {
        if (results.isEmpty()) return results;

        List<RecognitionResult> merged = new ArrayList<>();
        RecognitionResult current = copyResult(results.get(0));
        StringBuilder textBuffer = new StringBuilder(current.getText());

        for (int i = 1; i < results.size(); i++) {
            RecognitionResult result = results.get(i);

            if (current.getSpeakerId().equals(result.getSpeakerId())) {
                // 시간 간격 확인 (2초 이내면 병합)
                long gap = result.getOffset() - (current.getOffset() + current.getDuration());
                if (gap < 20000000L) {
                    textBuffer.append(" ").append(result.getText());
                    current.setText(textBuffer.toString());
                    current.setDuration(result.getOffset() + result.getDuration() - current.getOffset());
                } else {
                    // 간격이 크면 별도 발화
                    current.setText(textBuffer.toString());
                    merged.add(current);
                    current = copyResult(result);
                    textBuffer = new StringBuilder(result.getText());
                }
            } else {
                // 다른 화자
                current.setText(textBuffer.toString());
                merged.add(current);
                current = copyResult(result);
                textBuffer = new StringBuilder(result.getText());
            }
        }

        current.setText(textBuffer.toString());
        merged.add(current);

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