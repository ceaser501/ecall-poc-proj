package com.ecall.step2.s1emotionanalysis.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEmotionResponse {
    private List<EmotionTimelinePoint> emotionTimeline;
}
