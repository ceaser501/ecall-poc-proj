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
public class ClovaSpeechResponse {
    private String text;
    private List<Segment> segments;
    private List<Speaker> speakers;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Segment {
        private String text;
        private Double start;
        private Double end;
        private Integer speaker;
        private Double confidence;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Speaker {
        private Integer speaker;
        private Double start;
        private Double end;
        private String text;
    }
}
