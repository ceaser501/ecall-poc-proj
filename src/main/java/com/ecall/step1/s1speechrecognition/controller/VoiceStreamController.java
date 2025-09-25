package com.ecall.step1.s1speechrecognition.controller;

import com.ecall.step1.s1speechrecognition.service.VoiceRecognitionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.CloseStatus;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class VoiceStreamController extends TextWebSocketHandler {

    private final VoiceRecognitionService voiceRecognitionService;
    private final Map<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        String sessionId = session.getId();
        sessions.put(sessionId, session);
        log.info("WebSocket connection established: {}", sessionId);

        // Start voice recognition for this session
        voiceRecognitionService.startRecognition(sessionId, session);

        // Send initial message
        session.sendMessage(new TextMessage("{\"type\":\"connected\",\"sessionId\":\"" + sessionId + "\"}"));
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String sessionId = session.getId();
        String payload = message.getPayload();
        log.debug("Received message from {}: {}", sessionId, payload);

        // Handle control messages (start/stop)
        if (payload.contains("\"action\":\"start\"")) {
            voiceRecognitionService.startRecognition(sessionId, session);
            session.sendMessage(new TextMessage("{\"type\":\"status\",\"message\":\"Recognition started\"}"));
        } else if (payload.contains("\"action\":\"stop\"")) {
            voiceRecognitionService.stopRecognition(sessionId);
            session.sendMessage(new TextMessage("{\"type\":\"status\",\"message\":\"Recognition stopped\"}"));
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        String sessionId = session.getId();
        sessions.remove(sessionId);
        voiceRecognitionService.stopRecognition(sessionId);
        log.info("WebSocket connection closed: {} - {}", sessionId, status);
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) throws Exception {
        String sessionId = session.getId();
        log.error("WebSocket transport error for session {}: {}", sessionId, exception.getMessage());
        sessions.remove(sessionId);
        voiceRecognitionService.stopRecognition(sessionId);
    }
}