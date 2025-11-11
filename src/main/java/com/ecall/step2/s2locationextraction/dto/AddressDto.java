package com.ecall.step2.s2locationextraction.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AddressDto {
    private String roadAddress;
    private String jibunAddress;
    private String englishAddress;
    private double x; // longitude
    private double y; // latitude
}
