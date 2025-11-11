package com.ecall.step2.s1emotionanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmotionTimelinePoint {
    private String timestamp;
    private String utterance;
    private EmotionAnalysisResponse analysis;
}
