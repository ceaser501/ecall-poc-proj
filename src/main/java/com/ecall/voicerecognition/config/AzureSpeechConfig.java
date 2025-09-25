package com.ecall.voicerecognition.config;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class AzureSpeechConfig {

    @Value("${azure.speech.subscription-key:YOUR_AZURE_SPEECH_KEY}")
    private String subscriptionKey;

    @Value("${azure.speech.region:koreacentral}")
    private String region;

    @Value("${azure.speech.language:ko-KR}")
    private String language;

    @Bean
    public SpeechConfig speechConfig() {
        log.info("Initializing Azure Speech Config - Region: {}, Language: {}", region, language);

        SpeechConfig config = SpeechConfig.fromSubscription(subscriptionKey, region);
        config.setSpeechRecognitionLanguage(language);

        // Enable detailed output format
        config.setOutputFormat(OutputFormat.Detailed);

        // Enable profanity filtering (optional)
        config.setProfanity(ProfanityOption.Masked);

        return config;
    }

    @Bean
    public AudioConfig audioConfig() {
        // Default audio input from microphone
        // Will be overridden for WebSocket streaming
        return AudioConfig.fromDefaultMicrophoneInput();
    }
}