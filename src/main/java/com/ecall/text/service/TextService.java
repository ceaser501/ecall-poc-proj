package com.ecall.text.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 텍스트 처리 서비스
 * 담당자: 전선민
 */
@Slf4j
@Service
public class TextService {

    public String convertSpeechToText(byte[] audioData) {
        log.info("[전선민] STT 변환 로직");

        // TODO: 전선민 - STT 비즈니스 로직 구현
        // 1. Azure Speech STT API 호출
        // 2. 텍스트 변환 처리

        return "변환된 텍스트";
    }

    public String summarizeText(String originalText) {
        log.info("[전선민] 텍스트 요약 로직");

        // TODO: 전선민 - 요약 비즈니스 로직 구현
        // 1. Azure OpenAI API 호출
        // 2. 핵심 정보 추출
        // 3. 문장 교정

        return "요약된 텍스트";
    }
}