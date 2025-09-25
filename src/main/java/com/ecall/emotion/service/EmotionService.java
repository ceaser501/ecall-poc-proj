package com.ecall.emotion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 감정 분석 서비스
 * 담당자: 임송은
 */
@Slf4j
@Service
public class EmotionService {

    public String analyzeEmotion(String text) {
        log.info("[임송은] 감정 분석 로직");

        // TODO: 임송은 - 감정 분석 비즈니스 로직 구현
        // 1. 텍스트에서 감정 키워드 추출
        // 2. 긴급도 판단 알고리즘
        // 3. 심리 상태 분류

        return "긴급";
    }
}