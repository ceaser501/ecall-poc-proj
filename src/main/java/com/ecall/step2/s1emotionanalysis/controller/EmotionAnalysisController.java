package com.ecall.step2.s1emotionanalysis.controller;

import com.ecall.step2.s1emotionanalysis.dto.ConversationEmotionRequest;
import com.ecall.step2.s1emotionanalysis.dto.ConversationEmotionResponse;
import com.ecall.step2.s1emotionanalysis.dto.EmotionAnalysisRequest;
import com.ecall.step2.s1emotionanalysis.dto.EmotionAnalysisResponse;
import com.ecall.step2.s1emotionanalysis.service.EmotionAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/step2/emotion-analysis")
@RequiredArgsConstructor
public class EmotionAnalysisController {

    private final EmotionAnalysisService emotionAnalysisService;

    /**
     * [추가] API 연결 상태를 직접 확인하기 위한 간단한 테스트 API.
     * 브라우저에서 GET 요청으로 바로 확인할 수 있습니다.
     */
    @GetMapping("/test-connection")
    public ResponseEntity<EmotionAnalysisResponse> testApiConnection() {
        EmotionAnalysisResponse response = emotionAnalysisService.testApiConnection();
        return ResponseEntity.ok(response);
    }

    @PostMapping
    public ResponseEntity<EmotionAnalysisResponse> analyzeEmotion(@RequestBody EmotionAnalysisRequest request) {
        EmotionAnalysisResponse response = emotionAnalysisService.analyzeEmotion(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/conversation")
    public ResponseEntity<ConversationEmotionResponse> analyzeConversationEmotion(@RequestBody ConversationEmotionRequest request) {
        ConversationEmotionResponse response = emotionAnalysisService.analyzeConversation(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/upload")
    public ResponseEntity<ConversationEmotionResponse> uploadFile(@RequestParam("file") MultipartFile file) {
        try {
            ConversationEmotionResponse response = emotionAnalysisService.analyzeEmotionFromUploadedFile(file);
            return ResponseEntity.ok(response);
        } catch (IOException e) {
            // 필요하다면 여기에서 IO 예외에 대한 별도의 처리를 할 수 있습니다.
            // 예를 들어, 특정 오류 메시지를 클라이언트에게 반환할 수 있습니다.
            return ResponseEntity.status(500).build(); // 간단한 500 에러 반환
        }
    }
}
