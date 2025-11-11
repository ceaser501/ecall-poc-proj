package com.ecall.step2.s1emotionanalysis.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmotionAnalysisResponse {
    private String overallSentiment; // positive, neutral, negative, mixed
    private double positiveScore;
    private double neutralScore;
    private double negativeScore;
    // 추가적으로 필요한 정보가 있다면 여기에 추가할 수 있습니다.
}
