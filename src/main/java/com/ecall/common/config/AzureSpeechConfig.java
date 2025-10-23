package com.ecall.common.config;

import com.microsoft.cognitiveservices.speech.*;
import com.microsoft.cognitiveservices.speech.audio.*;
import io.github.cdimascio.dotenv.Dotenv;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.ArrayList;

@Slf4j
@Configuration
public class AzureSpeechConfig {

    private final Dotenv dotenv;

    @Value("${azure.speech.subscription-key}")
    private String subscriptionKey;

    @Value("${azure.speech.region}")
    private String region;

    @Value("${azure.speech.language:ko-KR}")
    private String language;

    public AzureSpeechConfig() {
        // Load .env file
        this.dotenv = Dotenv.configure()
            .ignoreIfMissing()
            .load();
    }

    @Bean
    public SpeechConfig speechConfig() {
        // Try to get from .env file first, then fall back to application properties
        String key = dotenv.get("AZURE_SPEECH_SUBSCRIPTION_KEY");
        if (key == null || key.equals("YOUR_AZURE_SPEECH_KEY")) {
            key = subscriptionKey;
        }

        String reg = dotenv.get("AZURE_SPEECH_REGION");
        if (reg == null) {
            reg = region;
        }

        log.info("Initializing Azure Speech Config - Region: {}, Language: {}", reg, language);
        log.debug("Using API Key: {}...", key != null && key.length() > 10 ? key.substring(0, 10) : "NOT SET");

        SpeechConfig config = SpeechConfig.fromSubscription(key, reg);
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

    @Bean(name = "englishSpeechConfig")
    public SpeechConfig englishSpeechConfig() {
        // Try to get from .env file first, then fall back to application properties
        String key = dotenv.get("AZURE_SPEECH_SUBSCRIPTION_KEY");
        if (key == null || key.equals("YOUR_AZURE_SPEECH_KEY")) {
            key = subscriptionKey;
        }

        String reg = dotenv.get("AZURE_SPEECH_REGION");
        if (reg == null) {
            reg = region;
        }

        log.info("Initializing Azure English Speech Config - Region: {}, Language: en-US", reg);

        SpeechConfig config = SpeechConfig.fromSubscription(key, reg);
        config.setSpeechRecognitionLanguage("en-US");

        // Enable detailed output format for better diarization
        config.setOutputFormat(OutputFormat.Detailed);

        // Enable speaker diarization
        config.setProperty("DiarizationEnabled", "true");
        config.setProperty("DiarizationSpeakerCount", "2");

        // Enable profanity filtering (optional)
        config.setProfanity(ProfanityOption.Masked);

        return config;
    }

    @Bean
    public AutoDetectSourceLanguageConfig autoDetectSourceLanguageConfig() {
        // Try to get from .env file first, then fall back to application properties
        String key = dotenv.get("AZURE_SPEECH_SUBSCRIPTION_KEY");
        if (key == null || key.equals("YOUR_AZURE_SPEECH_KEY")) {
            key = subscriptionKey;
        }

        String reg = dotenv.get("AZURE_SPEECH_REGION");
        if (reg == null) {
            reg = region;
        }

        log.info("Initializing Azure Auto Language Detection - Region: {}, Languages: ko-KR, en-US", reg);

        // Create list of candidate languages
        ArrayList<String> candidateLanguages = new ArrayList<>();
        candidateLanguages.add("ko-KR");  // Korean
        candidateLanguages.add("en-US");  // English

        // Create auto detect config
        return AutoDetectSourceLanguageConfig.fromLanguages(candidateLanguages);
    }

    @Bean(name = "multiLangSpeechConfig")
    public SpeechConfig multiLangSpeechConfig() {
        // Try to get from .env file first, then fall back to application properties
        String key = dotenv.get("AZURE_SPEECH_SUBSCRIPTION_KEY");
        if (key == null || key.equals("YOUR_AZURE_SPEECH_KEY")) {
            key = subscriptionKey;
        }

        String reg = dotenv.get("AZURE_SPEECH_REGION");
        if (reg == null) {
            reg = region;
        }

        log.info("Initializing Azure Multi-Language Speech Config - Region: {}", reg);

        SpeechConfig config = SpeechConfig.fromSubscription(key, reg);
        // Don't set a specific language - let auto-detect handle it

        // Enable detailed output format
        config.setOutputFormat(OutputFormat.Detailed);

        // Enable profanity filtering (optional)
        config.setProfanity(ProfanityOption.Masked);

        return config;
    }
}