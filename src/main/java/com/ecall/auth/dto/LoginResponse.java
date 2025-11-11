package com.ecall.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private boolean success;
    private String message;
    private String operatorId;
    private String name;
    private String id;
    private String role;
    private String organizationName;
    private String joinDate;
    private String photoUrl;
}
