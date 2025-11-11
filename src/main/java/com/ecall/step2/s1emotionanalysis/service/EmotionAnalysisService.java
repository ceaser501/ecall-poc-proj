package com.ecall.step2.s1emotionanalysis.service;

import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.models.DocumentSentiment;
import com.azure.ai.textanalytics.models.SentimentConfidenceScores;
import com.ecall.step2.s1emotionanalysis.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EmotionAnalysisService {

    private final TextAnalyticsClient textAnalyticsClient;

    public EmotionAnalysisResponse analyzeEmotion(EmotionAnalysisRequest request) {
        return analyzeSingleText(request.getText());
    }

    /**
     * [로직 개선]
     * 화자 태그가 없는 자유 형식의 대화를 더 유연하게 분석하도록 로직을 개선합니다.
     * 1. 텍스트에 "화자" 같은 태그가 있는지 먼저 확인합니다.
     * 2. 태그가 없는 경우, 빈 줄을 기준으로 발화자를 나누고, 신고자와 접수자가 번갈아 말하는 것으로 가정하여 분석합니다.
     * 3. 태그가 있는 경우, 기존의 구조화된 스크립트 분석 로직을 사용합니다.
     */
    public ConversationEmotionResponse analyzeConversation(ConversationEmotionRequest request) {
        try {
            if (request == null || request.getFullConversationText() == null || request.getFullConversationText().trim().isEmpty()) {
                return new ConversationEmotionResponse(Collections.emptyList());
            }

            String conversationText = request.getFullConversationText();
            List<EmotionTimelinePoint> timeline = new ArrayList<>();

            Pattern structurePattern = Pattern.compile("^(화자|신고자|접수자|\\d{1,2}:\\d{2})", Pattern.MULTILINE);
            boolean isStructured = structurePattern.matcher(conversationText).find();

            if (isStructured) {
                // [기존 로직] 구조화된 스크립트 분석
                String scriptContent = conversationText.replaceAll("(?m)^={2,}.*|파일명:.*|분석 시간:.*|화자 수:.*\\n", "").trim();
                String[] lines = scriptContent.split("\\r?\\n");

                String currentSpeaker = null;
                String currentEndTime = "N/A";
                StringBuilder utteranceBuilder = new StringBuilder();

                Pattern speakerHeaderPattern = Pattern.compile(
                    "^(화자\\s*\\d+|신고자|접수자)" + 
                    "(?:\\s*\\[[0-9:.]+\\s*-\\s*([0-9:.]+)\\])?" + 
                    "\\s*[:]?\\s*(.*)" 
                );

                for (String line : lines) {
                    String trimmedLine = line.trim();
                    if (trimmedLine.isEmpty()) continue;

                    Matcher headerMatcher = speakerHeaderPattern.matcher(trimmedLine);

                    if (headerMatcher.matches()) {
                        if (currentSpeaker != null && utteranceBuilder.length() > 0) {
                            processUtterance(timeline, currentSpeaker, utteranceBuilder.toString().trim(), currentEndTime);
                        }

                        currentSpeaker = headerMatcher.group(1).trim();
                        currentEndTime = (headerMatcher.group(2) != null) ? headerMatcher.group(2).trim() : "N/A";
                        
                        utteranceBuilder.setLength(0);
                        String restOfLine = headerMatcher.group(3).trim();
                        if (!restOfLine.isEmpty()) {
                            utteranceBuilder.append(restOfLine);
                        }
                    } else if (currentSpeaker != null) {
                        if (utteranceBuilder.length() > 0) {
                            utteranceBuilder.append(" ");
                        }
                        utteranceBuilder.append(trimmedLine);
                    }
                }

                if (currentSpeaker != null && utteranceBuilder.length() > 0) {
                    processUtterance(timeline, currentSpeaker, utteranceBuilder.toString().trim(), currentEndTime);
                }
            } else {
                // [개선된 로직] 자유 형식 대화 분석
                // 빈 줄(두 번 이상의 개행)을 기준으로 발화 단위를 나눕니다.
                String[] utterances = conversationText.trim().split("(\\r?\\n){2,}");

                // 만약 빈 줄이 없다면, 단순 개행을 기준으로 나눕니다.
                if (utterances.length <= 1 && conversationText.contains("\n")) {
                    utterances = conversationText.trim().split("(\\r?\\n)+");
                }

                for (int i = 0; i < utterances.length; i++) {
                    String utterance = utterances[i].trim();
                    if (utterance.isEmpty()) continue;

                    // 홀수 번째 발화(첫 번째, 세 번째...)를 '신고자'의 발화로 간주합니다.
                    String speaker = (i % 2 == 0) ? "신고자" : "접수자";
                    processUtterance(timeline, speaker, utterance, "N/A");
                }
            }

            return new ConversationEmotionResponse(timeline);

        } catch (Exception e) {
            return new ConversationEmotionResponse(Collections.emptyList());
        }
    }

    private void processUtterance(List<EmotionTimelinePoint> timeline, String speakerIdentifier, String utterance, String endTime) {
        String lowerId = speakerIdentifier.toLowerCase();
        // '신고자' 또는 '화자 2'를 신고자로 간주합니다.
        boolean isCaller = lowerId.contains("신고자") || lowerId.equals("화자 2");

        if (isCaller && !utterance.isEmpty()) {
            EmotionAnalysisResponse analysis = analyzeSingleText(utterance);
            timeline.add(new EmotionTimelinePoint(endTime != null ? endTime : "N/A", utterance, analysis));
        }
    }

    public ConversationEmotionResponse analyzeEmotionFromUploadedFile(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return new ConversationEmotionResponse(Collections.emptyList());
        }
        String fileContent = new String(file.getBytes(), StandardCharsets.UTF_8);
        ConversationEmotionRequest request = new ConversationEmotionRequest();
        request.setFullConversationText(fileContent);
        return analyzeConversation(request);
    }

    public EmotionAnalysisResponse testApiConnection() {
        return analyzeSingleText("I am very happy");
    }

    private EmotionAnalysisResponse analyzeSingleText(String text) {
        if (text == null || text.trim().isEmpty()) {
            return new EmotionAnalysisResponse("neutral", 0.0, 1.0, 0.0);
        }
        try {
            boolean isKorean = text.matches(".*[\\p{IsHangul}].*");
            String language = isKorean ? "ko" : "en";

            DocumentSentiment documentSentiment = textAnalyticsClient.analyzeSentiment(text, language);

            if (documentSentiment == null) {
                return new EmotionAnalysisResponse("neutral", 0.0, 1.0, 0.0);
            }
            SentimentConfidenceScores scores = documentSentiment.getConfidenceScores();
            return new EmotionAnalysisResponse(
                    documentSentiment.getSentiment().toString(),
                    scores.getPositive(),
                    scores.getNeutral(),
                    scores.getNegative()
            );
        } catch (Exception e) {
            return new EmotionAnalysisResponse("error", 0.0, 0.0, 0.0);
        }
    }
}
