package com.ecall.step2.s2locationextraction.controller;

import com.ecall.step2.s2locationextraction.dto.LocationExtractionResponse;
import com.ecall.step2.s2locationextraction.service.LocationExtractionService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/step2/location-extraction")
public class LocationExtractionController {

    private final LocationExtractionService locationExtractionService;

    public LocationExtractionController(LocationExtractionService locationExtractionService) {
        this.locationExtractionService = locationExtractionService;
    }

    @PostMapping("/extract-from-text")
    public ResponseEntity<LocationExtractionResponse> extractLocationFromText(@RequestParam("text") String text) {
        try {
            LocationExtractionResponse response = locationExtractionService.extractLocationFromText(text);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            // Consider creating a proper error response object
            return ResponseEntity.internalServerError().build();
        }
    }

    @PostMapping("/extract-from-file")
    public ResponseEntity<LocationExtractionResponse> extractLocationFromFile(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            LocationExtractionResponse response = locationExtractionService.extractLocationFromFile(file);
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.internalServerError().build();
        }
    }
}
