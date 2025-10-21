package com.ecall.step1.s1speechrecognition.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class RecognitionResult {
    private String sessionId;
    private String speakerId;
    private String text;
    private Long offset;
    private Long duration;
    private LocalDateTime timestamp;
    private String type;
    private boolean interim;
    private Integer channelIndex;
}