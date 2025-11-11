package com.ecall.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorRegistrationResponse {
    private boolean success;
    private String message;
    private String operatorId;
    private String id;
}
