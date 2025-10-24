package com.ecall.step1.s1speechrecognition.service;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ecall.step1.s1speechrecognition.dto.ClovaSpeechResponse;
import com.ecall.step1.s1speechrecognition.dto.DiarizationResult;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ClovaDiarizationService {

    @Autowired
    private ClovaSpeechClient clovaSpeechClient;
    
    private final Gson gson = new Gson();

    /**
     * 음성 파일을 업로드하고 화자 분리 및 STT를 수행합니다.
     */
    public DiarizationResult performDiarization(File audioFile, int minSpeakers, int maxSpeakers) {
        return performDiarization(audioFile, minSpeakers, maxSpeakers, "ko-KR");
    }

    /**
     * 음성 파일을 업로드하고 화자 분리 및 STT를 수행합니다.
     * @param language 언어 코드 (ko-KR: 한국어, en-US: 영어, ja: 일본어, zh-CN: 중국어)
     */
    public DiarizationResult performDiarization(File audioFile, int minSpeakers, int maxSpeakers, String language) {
        try {
            log.info("화자 분리 시작 - 파일: {}, 최소 화자: {}, 최대 화자: {}, 언어: {}",
                audioFile.getName(), minSpeakers, maxSpeakers, language);

            // Clova Speech API 요청 설정
            ClovaSpeechClient.NestRequestEntity requestEntity = new ClovaSpeechClient.NestRequestEntity();
            // 언어 설정
            requestEntity.setLanguage(language);
            requestEntity.setCompletion("sync");
            
            // 화자 분리를 위한 필수 설정
            requestEntity.setWordAlignment(true);  // 단어 단위 타임스탬프
            requestEntity.setFullText(true);        // 전체 텍스트 반환
            
            // 화자 분리 설정 (Diarization)
            ClovaSpeechClient.Diarization diarization = new ClovaSpeechClient.Diarization();
            diarization.setEnable(true);  // 화자 분리 활성화
            diarization.setSpeakerCountMin(minSpeakers);
            diarization.setSpeakerCountMax(maxSpeakers);
            requestEntity.setDiarization(diarization);
            
            log.info("화자 분리 요청 설정 - enable: true, minSpeakers: {}, maxSpeakers: {}", minSpeakers, maxSpeakers);

            // Clova Speech API 호출
            String responseJson = clovaSpeechClient.upload(audioFile, requestEntity);
            log.info("Clova Speech API 응답 수신 완료");
            log.debug("API 응답 JSON: {}", responseJson);

            // 응답 파싱
            Map<String, Object> responseMap = gson.fromJson(responseJson, new TypeToken<Map<String, Object>>(){}.getType());
            
            return parseResponse(responseMap);
            
        } catch (Exception e) {
            log.error("화자 분리 처리 중 오류 발생", e);
            throw new RuntimeException("화자 분리 처리 실패", e);
        }
    }

    /**
     * Clova Speech API 응답을 파싱하여 DiarizationResult로 변환합니다.
     */
    private DiarizationResult parseResponse(Map<String, Object> responseMap) {
        try {
            // 전체 텍스트 추출
            String fullText = (String) responseMap.get("text");
            
            // 세그먼트 정보 추출
            List<Map<String, Object>> segments = (List<Map<String, Object>>) responseMap.get("segments");
            List<DiarizationResult.SpeakerSegment> speakerSegments = new ArrayList<>();
            
            int maxSpeakerId = 0;
            
            if (segments != null) {
                log.info("총 {}개의 세그먼트 파싱 시작", segments.size());
                for (Map<String, Object> segment : segments) {
                    // 화자 정보 추출 - speaker 객체에서 label 가져오기
                    int speakerId = 0;
                    if (segment.containsKey("speaker")) {
                        Map<String, Object> speaker = (Map<String, Object>) segment.get("speaker");
                        if (speaker != null && speaker.containsKey("label")) {
                            String label = (String) speaker.get("label");
                            // "1", "2" 등을 0, 1로 변환
                            try {
                                speakerId = Integer.parseInt(label) - 1;
                            } catch (NumberFormatException e) {
                                speakerId = 0;
                            }
                        }
                    } else if (segment.containsKey("diarization")) {
                        // diarization 필드에서도 확인
                        Map<String, Object> diarization = (Map<String, Object>) segment.get("diarization");
                        if (diarization != null && diarization.containsKey("label")) {
                            String label = (String) diarization.get("label");
                            try {
                                speakerId = Integer.parseInt(label) - 1;
                            } catch (NumberFormatException e) {
                                speakerId = 0;
                            }
                        }
                    }
                    
                    String text = parseString(segment.get("text"));
                    // 밀리초를 초로 변환
                    double startTime = parseDouble(segment.get("start")) / 1000.0;
                    double endTime = parseDouble(segment.get("end")) / 1000.0;
                    
                    log.debug("세그먼트 - 화자: {}, 시간: {:.2f}초-{:.2f}초, 텍스트: {}", 
                        speakerId, startTime, endTime, text);
                    
                    DiarizationResult.SpeakerSegment speakerSegment = DiarizationResult.SpeakerSegment.builder()
                        .speakerId(speakerId)
                        .text(text)
                        .startTime(startTime)
                        .endTime(endTime)
                        .confidence(parseDouble(segment.get("confidence")))
                        .build();
                    
                    speakerSegments.add(speakerSegment);
                    maxSpeakerId = Math.max(maxSpeakerId, speakerSegment.getSpeakerId());
                }
            } else {
                log.warn("세그먼트 정보가 없습니다. 화자 분리가 수행되지 않았을 수 있습니다.");
            }
            
            // 연속된 같은 화자의 세그먼트를 병합
            List<DiarizationResult.SpeakerSegment> mergedSegments = mergeConsecutiveSpeakerSegments(speakerSegments);
            
            // 실제 화자 수 계산
            int actualSpeakerCount = (int) mergedSegments.stream()
                .map(DiarizationResult.SpeakerSegment::getSpeakerId)
                .distinct()
                .count();
            
            log.info("화자 분리 완료 - 총 화자 수: {}, 세그먼트 수: {}", actualSpeakerCount, mergedSegments.size());
            
            return DiarizationResult.builder()
                .fullText(fullText)
                .speakerSegments(mergedSegments)
                .speakerCount(actualSpeakerCount)
                .build();
                
        } catch (Exception e) {
            log.error("응답 파싱 중 오류 발생", e);
            throw new RuntimeException("응답 파싱 실패", e);
        }
    }

    /**
     * 연속된 같은 화자의 세그먼트를 병합합니다.
     * 화자가 바뀔 때마다 새로운 세그먼트를 생성하여 화자 전환을 명확히 표시합니다.
     */
    private List<DiarizationResult.SpeakerSegment> mergeConsecutiveSpeakerSegments(List<DiarizationResult.SpeakerSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return new ArrayList<>();
        }
        
        // 시간순으로 정렬
        segments.sort((a, b) -> Double.compare(a.getStartTime(), b.getStartTime()));
        
        List<DiarizationResult.SpeakerSegment> result = new ArrayList<>();
        
        DiarizationResult.SpeakerSegment currentSegment = null;
        StringBuilder currentText = new StringBuilder();
        double currentStartTime = 0;
        double currentEndTime = 0;
        double totalConfidence = 0;
        int segmentCount = 0;
        
        for (DiarizationResult.SpeakerSegment segment : segments) {
            if (currentSegment == null) {
                // 첫 번째 세그먼트
                currentSegment = segment;
                currentText.append(segment.getText());
                currentStartTime = segment.getStartTime();
                currentEndTime = segment.getEndTime();
                totalConfidence = segment.getConfidence();
                segmentCount = 1;
            } else if (currentSegment.getSpeakerId() == segment.getSpeakerId()) {
                // 같은 화자의 연속된 발화 - 병합
                currentText.append(" ").append(segment.getText());
                currentEndTime = segment.getEndTime();
                totalConfidence += segment.getConfidence();
                segmentCount++;
            } else {
                // 화자가 바뀜 - 현재 세그먼트를 결과에 추가하고 새로운 세그먼트 시작
                result.add(DiarizationResult.SpeakerSegment.builder()
                    .speakerId(currentSegment.getSpeakerId())
                    .text(currentText.toString().trim())
                    .startTime(currentStartTime)
                    .endTime(currentEndTime)
                    .confidence(totalConfidence / segmentCount)
                    .build());
                
                // 새로운 화자의 세그먼트 시작
                currentSegment = segment;
                currentText = new StringBuilder(segment.getText());
                currentStartTime = segment.getStartTime();
                currentEndTime = segment.getEndTime();
                totalConfidence = segment.getConfidence();
                segmentCount = 1;
            }
        }
        
        // 마지막 세그먼트 추가
        if (currentSegment != null) {
            result.add(DiarizationResult.SpeakerSegment.builder()
                .speakerId(currentSegment.getSpeakerId())
                .text(currentText.toString().trim())
                .startTime(currentStartTime)
                .endTime(currentEndTime)
                .confidence(totalConfidence / segmentCount)
                .build());
        }
        
        log.debug("세그먼트 병합 완료 - 원본: {}개 -> 병합 후: {}개", segments.size(), result.size());
        
        return result;
    }

    /**
     * 안전한 정수 변환
     */
    private int parseInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException e) {
                return 0;
            }
        }
        return 0;
    }

    /**
     * 안전한 문자열 변환
     */
    private String parseString(Object value) {
        if (value == null) return "";
        return value.toString();
    }

    /**
     * 안전한 실수 변환
     */
    private double parseDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) {
            return ((Number) value).doubleValue();
        }
        if (value instanceof String) {
            try {
                return Double.parseDouble((String) value);
            } catch (NumberFormatException e) {
                return 0.0;
            }
        }
        return 0.0;
    }
}
