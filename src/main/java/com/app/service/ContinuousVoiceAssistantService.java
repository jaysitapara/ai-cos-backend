package com.app.service;

import com.app.enums.VoiceSessionState;
import com.app.model.voice.VoiceSessionModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ContinuousVoiceAssistantService {

    private final Map<String, VoiceSessionModel> sessionStore = new ConcurrentHashMap<>();

    public VoiceSessionModel startSession(String userId, String language) {
        String sessionId = "vses-" + UUID.randomUUID();

        VoiceSessionModel session = VoiceSessionModel.builder()
            .sessionId(sessionId)
            .userId(userId != null ? userId : "user-default")
            .language(language != null ? language : "en-US")
            .state(VoiceSessionState.LISTENING)
            .micActive(true)
            .audioLevel(0.75)
            .activeCommand("Awaiting voice command...")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        sessionStore.put(sessionId, session);
        return session;
    }

    public Optional<VoiceSessionModel> getSession(String sessionId) {
        return Optional.ofNullable(sessionStore.get(sessionId));
    }

    public VoiceSessionModel stopSession(String sessionId) {
        VoiceSessionModel session = sessionStore.get(sessionId);
        if (session != null) {
            session.setState(VoiceSessionState.IDLE);
            session.setMicActive(false);
            session.setAudioLevel(0.0);
            session.setUpdatedAt(Instant.now());
        }
        return session;
    }
}
