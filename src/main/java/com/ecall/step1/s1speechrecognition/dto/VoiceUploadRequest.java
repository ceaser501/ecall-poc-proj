package com.ecall.step1.s1speechrecognition.dto;

import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class VoiceUploadRequest {
    private MultipartFile file;
    private boolean enableDiarization = true;
    private String language = "ko-KR";
    private Integer maxSpeakers = 4;
}