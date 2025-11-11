package com.ecall.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OperatorRegistrationRequest {
    private String operatorId;
    private String password;
    private String confirmPassword;
    private String name;
    private Integer age;
    private String gender;
    private String role;
    private String phoneNumber;
    private String address;
    private String addressDetail;
}
