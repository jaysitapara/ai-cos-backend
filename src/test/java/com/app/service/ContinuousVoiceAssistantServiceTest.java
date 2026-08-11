package com.app.service;

import com.app.enums.VoiceSessionState;
import com.app.model.voice.VoiceSessionModel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ContinuousVoiceAssistantServiceTest {

    private ContinuousVoiceAssistantService service;

    @BeforeEach
    void setUp() {
        service = new ContinuousVoiceAssistantService();
    }

    @Test
    void testStartAndStopVoiceSession() {
        VoiceSessionModel session = service.startSession("user-1", "en-US");

        assertNotNull(session);
        assertEquals("user-1", session.getUserId());
        assertEquals(VoiceSessionState.LISTENING, session.getState());
        assertTrue(session.isMicActive());

        VoiceSessionModel stopped = service.stopSession(session.getSessionId());
        assertEquals(VoiceSessionState.IDLE, stopped.getState());
        assertFalse(stopped.isMicActive());
    }
}
