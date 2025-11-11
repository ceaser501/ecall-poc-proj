package com.ecall.common.config;

import com.azure.ai.textanalytics.TextAnalyticsClient;
import com.azure.ai.textanalytics.TextAnalyticsClientBuilder;
import com.azure.core.credential.AzureKeyCredential;
import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class AzureTextAnalyticsConfig {

    @Bean
    public TextAnalyticsClient textAnalyticsClient() {
        Dotenv dotenv = Dotenv.load();
        String azureAiApiKey = dotenv.get("AZURE_AI_API_KEY");
        String azureAiApiEndpoint = dotenv.get("AZURE_AI_API_ENDPOINT");

        if (azureAiApiKey == null || azureAiApiEndpoint == null || azureAiApiKey.isEmpty() || azureAiApiEndpoint.isEmpty()) {
            throw new IllegalStateException("AZURE_AI_API_KEY and AZURE_AI_API_ENDPOINT must be configured in the .env file.");
        }

        // [ROLLBACK] 모든 serviceVersion 설정을 제거하고, 라이브러리의 기본 동작을 따르도록 초기화합니다.
        return new TextAnalyticsClientBuilder()
                .credential(new AzureKeyCredential(azureAiApiKey))
                .endpoint(azureAiApiEndpoint)
                .buildClient();
    }
}
