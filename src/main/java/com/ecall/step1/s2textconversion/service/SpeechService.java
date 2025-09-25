package com.ecall.step1.s2textconversion.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 음성인식 서비스
 * 담당자: 김태수
 */
@Slf4j
@Service
public class SpeechService {

    public String processAudio(MultipartFile audioFile) {
        log.info("[김태수] 음성 처리 로직");

        // TODO: 김태수 - 음성인식 비즈니스 로직 구현
        // 1. Azure Speech SDK 연동
        // 2. Speaker Recognition API 호출
        // 3. 발화자 분리 처리

        return "processed";
    }
}