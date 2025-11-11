package com.ecall.step2.s2locationextraction.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class LocationExtractionResponse {
    private List<AddressInfo> addresses;
    private List<String> mentionedLocations;
}
