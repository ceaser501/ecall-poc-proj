package com.ecall.auth.config;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.lang.reflect.Field;
import java.net.HttpURLConnection;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;

@Configuration
@Getter
@Slf4j
public class SupabaseConfig {

    @Value("${supabase.url}")
    private String supabaseUrl;

    @Value("${supabase.key}")
    private String supabaseKey;

    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory() {
            @Override
            protected void prepareConnection(HttpURLConnection connection, String httpMethod) {
                try {
                    // Enable PATCH method via reflection
                    if (httpMethod.equals("PATCH")) {
                        Field methodsField = HttpURLConnection.class.getDeclaredField("methods");
                        methodsField.setAccessible(true);
                        String[] oldMethods = (String[]) methodsField.get(null);
                        Set<String> methodsSet = new LinkedHashSet<>(Arrays.asList(oldMethods));
                        methodsSet.add("PATCH");
                        methodsField.set(null, methodsSet.toArray(new String[0]));
                    }
                    super.prepareConnection(connection, httpMethod);
                } catch (Exception e) {
                    log.warn("Failed to enable PATCH method via reflection: {}", e.getMessage());
                    try {
                        super.prepareConnection(connection, httpMethod);
                    } catch (Exception ex) {
                        throw new RuntimeException(ex);
                    }
                }
            }
        };
        return new RestTemplate(requestFactory);
    }

    public String getApiUrl() {
        return supabaseUrl + "/rest/v1";
    }
}
