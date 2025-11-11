package com.ecall.step2.s1emotionanalysis.dto;

import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class EmotionAnalysisRequest {
    private String text;
}
