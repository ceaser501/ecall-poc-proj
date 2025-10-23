package com.ecall.step1.s1speechrecognition.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DiarizationResult {
    private String fullText;
    private List<SpeakerSegment> speakerSegments;
    private int speakerCount;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SpeakerSegment {
        private int speakerId;
        private String text;
        private double startTime;
        private double endTime;
        private double confidence;
    }
}
