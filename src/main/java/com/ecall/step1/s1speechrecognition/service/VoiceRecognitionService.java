package com.ecall.step1.s1speechrecognition.service;

import com.ecall.step1.s1speechrecognition.model.RecognitionResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.microsoft.cognitiveservices.speech.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Future;

@Slf4j
@Service
@RequiredArgsConstructor
public class VoiceRecognitionService {

    private final SpeechConfig speechConfig;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, SpeechRecognizer> activeRecognizers = new ConcurrentHashMap<>();
    private final Map<String, WebSocketSession> activeSessions = new ConcurrentHashMap<>();

    public void startRecognition(String sessionId, WebSocketSession session) {
        try {
            log.info("Starting voice recognition for session: {}", sessionId);

            // Store active session
            activeSessions.put(sessionId, session);

            // Create speech recognizer
            SpeechRecognizer recognizer = new SpeechRecognizer(speechConfig);

            // Set up event handlers
            setupRecognizerEvents(recognizer, sessionId);

            // Start continuous recognition
            Future<Void> task = recognizer.startContinuousRecognitionAsync();
            task.get();

            // Store the recognizer
            activeRecognizers.put(sessionId, recognizer);

            log.info("Voice recognition started successfully for session: {}", sessionId);

        } catch (Exception e) {
            log.error("Failed to start recognition for session {}: {}", sessionId, e.getMessage(), e);
            sendErrorToClient(sessionId, "Failed to start voice recognition: " + e.getMessage());
        }
    }

    private void setupRecognizerEvents(SpeechRecognizer recognizer, String sessionId) {
        // Handle recognized speech
        recognizer.recognized.addEventListener((s, e) -> {
            if (e.getResult().getReason() == ResultReason.RecognizedSpeech) {
                String text = e.getResult().getText();
                Long offset = e.getResult().getOffset() != null ? e.getResult().getOffset().longValue() : 0L;
                Long duration = e.getResult().getDuration() != null ? e.getResult().getDuration().longValue() : 0L;

                log.info("Recognized: {}", text);

                RecognitionResult result = new RecognitionResult();
                result.setSessionId(sessionId);
                result.setSpeakerId("Speaker"); // Simple speaker identification
                result.setText(text);
                result.setOffset(offset);
                result.setDuration(duration);
                result.setTimestamp(LocalDateTime.now());
                result.setType("recognized");

                sendResultToClient(sessionId, result);
            } else if (e.getResult().getReason() == ResultReason.NoMatch) {
                log.debug("No speech could be recognized for session: {}", sessionId);
            }
        });

        // Handle interim results (partial recognition)
        recognizer.recognizing.addEventListener((s, e) -> {
            String text = e.getResult().getText();

            log.debug("Recognizing: {}", text);

            RecognitionResult result = new RecognitionResult();
            result.setSessionId(sessionId);
            result.setSpeakerId("Speaker");
            result.setText(text);
            result.setTimestamp(LocalDateTime.now());
            result.setType("recognizing");
            result.setInterim(true);

            sendResultToClient(sessionId, result);
        });

        // Handle session started
        recognizer.sessionStarted.addEventListener((s, e) -> {
            log.info("Session started for: {}", sessionId);
            sendStatusToClient(sessionId, "Session started");
        });

        // Handle session stopped
        recognizer.sessionStopped.addEventListener((s, e) -> {
            log.info("Session stopped for: {}", sessionId);
            sendStatusToClient(sessionId, "Session stopped");
        });

        // Handle cancellation
        recognizer.canceled.addEventListener((s, e) -> {
            log.warn("Recognition canceled for session {}: {}", sessionId, e.getReason());
            if (e.getReason() == CancellationReason.Error) {
                sendErrorToClient(sessionId, "Recognition error: " + e.getErrorDetails());
            }
        });
    }

    public void stopRecognition(String sessionId) {
        SpeechRecognizer recognizer = activeRecognizers.remove(sessionId);
        if (recognizer != null) {
            try {
                Future<Void> task = recognizer.stopContinuousRecognitionAsync();
                task.get();
                recognizer.close();
                log.info("Stopped recognition for session: {}", sessionId);
            } catch (Exception e) {
                log.error("Error stopping recognition for session {}: {}", sessionId, e.getMessage());
            }
        }
        activeSessions.remove(sessionId);
    }

    private void sendResultToClient(String sessionId, RecognitionResult result) {
        WebSocketSession session = activeSessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                String json = objectMapper.writeValueAsString(result);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.error("Failed to send result to client {}: {}", sessionId, e.getMessage());
            }
        }
    }

    private void sendStatusToClient(String sessionId, String status) {
        WebSocketSession session = activeSessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = Map.of(
                    "type", "status",
                    "sessionId", sessionId,
                    "message", status,
                    "timestamp", LocalDateTime.now()
                );
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.error("Failed to send status to client {}: {}", sessionId, e.getMessage());
            }
        }
    }

    private void sendErrorToClient(String sessionId, String error) {
        WebSocketSession session = activeSessions.get(sessionId);
        if (session != null && session.isOpen()) {
            try {
                Map<String, Object> message = Map.of(
                    "type", "error",
                    "sessionId", sessionId,
                    "error", error,
                    "timestamp", LocalDateTime.now()
                );
                String json = objectMapper.writeValueAsString(message);
                session.sendMessage(new TextMessage(json));
            } catch (Exception e) {
                log.error("Failed to send error to client {}: {}", sessionId, e.getMessage());
            }
        }
    }
}