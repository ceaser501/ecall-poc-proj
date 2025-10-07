package com.ecall.step1.s1speechrecognition.dto;

import com.ecall.step1.s1speechrecognition.model.RecognitionResult;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class VoiceUploadResponse {
    private boolean success;
    private String filename;
    private Long size;
    private List<RecognitionResult> results;
    private Long speakerCount;
    private String error;
    private Long processingTimeMs;
}