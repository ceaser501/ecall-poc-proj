package com.ecall.step2.s2locationextraction.service;

import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.models.CategorizedEntity;
import com.azure.ai.textanalytics.models.PiiEntityCategory;
import com.ecall.step1.s1speechrecognition.service.AudioConversionService;
import com.ecall.step1.s1speechrecognition.service.AudioFileRecognitionService;
import com.ecall.step2.s2locationextraction.dto.AddressInfo;
import com.ecall.step2.s2locationextraction.dto.LocationExtractionResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class LocationExtractionService {

    private final TextAnalyticsClient textAnalyticsClient;
    private final AudioFileRecognitionService audioFileRecognitionService;
    private final AudioConversionService audioConversionService;

    public LocationExtractionService(TextAnalyticsClient textAnalyticsClient, AudioFileRecognitionService audioFileRecognitionService, AudioConversionService audioConversionService) {
        this.textAnalyticsClient = textAnalyticsClient;
        this.audioFileRecognitionService = audioFileRecognitionService;
        this.audioConversionService = audioConversionService;
    }

    public LocationExtractionResponse extractLocationFromFile(MultipartFile file) throws Exception {
        String text;
        String fileName = file.getOriginalFilename();

        if (fileName != null && fileName.toLowerCase().endsWith(".txt")) {
            text = new String(file.getBytes(), StandardCharsets.UTF_8);
        } else {
            File wavFile = audioConversionService.convertToWav(file);
            text = audioFileRecognitionService.recognizeFromFile(wavFile)
                    .stream()
                    .map(result -> result.getText())
                    .collect(Collectors.joining(" "));
            wavFile.delete();
        }

        return extractLocationFromText(text);
    }

    public LocationExtractionResponse extractLocationFromText(String text) {
        List<AddressInfo> extractedAddresses = new ArrayList<>();
        Set<String> addressTexts = new HashSet<>();

        textAnalyticsClient.recognizePiiEntities(text).forEach(entity -> {
            if (entity.getCategory() == PiiEntityCategory.ADDRESS) {
                String address = entity.getText();
                addressTexts.add(address);
                String context = findContextSentence(text, entity.getOffset(), entity.getLength());
                extractedAddresses.add(new AddressInfo(address, context));
            }
        });

        List<String> genericLocations = textAnalyticsClient.recognizeEntities(text).stream()
                .filter(entity -> "Location".equals(entity.getCategory().toString()))
                .map(CategorizedEntity::getText)
                .filter(locationText -> !addressTexts.contains(locationText))
                .distinct()
                .collect(Collectors.toList());

        return new LocationExtractionResponse(extractedAddresses, genericLocations);
    }

    private String findContextSentence(String text, int offset, int length) {
        int start = offset;
        while (start > 0 && text.charAt(start - 1) != '.' && text.charAt(start - 1) != '?' && text.charAt(start - 1) != '!' && text.charAt(start - 1) != '\n') {
            start--;
        }
        while (start < text.length() && Character.isWhitespace(text.charAt(start))) {
            start++;
        }

        int end = offset + length;
        while (end < text.length() && text.charAt(end) != '.' && text.charAt(end) != '?' && text.charAt(end) != '!' && text.charAt(end) != '\n') {
            end++;
        }
        if (end < text.length() && (text.charAt(end) == '.' || text.charAt(end) == '?' || text.charAt(end) == '!')) {
            end++;
        }

        return text.substring(start, end).trim();
    }
}
