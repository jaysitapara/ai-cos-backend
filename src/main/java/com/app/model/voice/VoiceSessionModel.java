package com.app.model.voice;

import com.app.enums.VoiceSessionState;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSessionModel {
    private String sessionId;
    private String userId;
    private String language;
    private VoiceSessionState state;
    private boolean micActive;
    private double audioLevel;
    private String activeCommand;
    private Instant createdAt;
    private Instant updatedAt;
}
