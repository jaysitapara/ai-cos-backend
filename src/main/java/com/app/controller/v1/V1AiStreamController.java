package com.app.controller.v1;

import com.app.dto.agent.ExecutionProgressResponse;
import com.app.entity.AgentWorkspaceSessionEntity;
import com.app.repository.AgentWorkspaceSessionRepository;
import com.app.service.AgentWorkspaceService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping(com.app.common.ApiConstants.API_V1 + "/agent-workspace/sessions")
@RequiredArgsConstructor
public class V1AiStreamController {

    private final AgentWorkspaceSessionRepository sessionRepository;
    private final AgentWorkspaceService workspaceService;

    private final ScheduledExecutorService streamingScheduler = Executors.newScheduledThreadPool(4);

    @GetMapping(value = "/{publicId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSessionProgress(@PathVariable String publicId) {
        log.info("Client subscribed to SSE streaming events for session publicId: {}", publicId);

        // 30-minute timeout for live streaming connections
        SseEmitter emitter = new SseEmitter(1800000L);

        AgentWorkspaceSessionEntity session = sessionRepository.findByPublicId(UUID.fromString(publicId))
                .orElse(null);

        if (session == null) {
            try {
                emitter.send(SseEmitter.event().name("error").data(Map.of("message", "Session not found")));
                emitter.complete();
            } catch (Exception ignored) {}
            return emitter;
        }

        // Live Real-Time Progress Stream (every 1.5 seconds)
        var streamingTask = streamingScheduler.scheduleAtFixedRate(() -> {
            try {
                ExecutionProgressResponse progress = workspaceService.getProgress(UUID.fromString(publicId));
                emitter.send(SseEmitter.event()
                        .name("progress")
                        .data(progress));

                if ("COMPLETED".equals(progress.getSessionStatus()) || "FAILED".equals(progress.getSessionStatus())) {
                    emitter.send(SseEmitter.event().name("complete").data(progress));
                }
            } catch (Exception e) {
                log.debug("SSE client stream ended for session: {}", publicId);
            }
        }, 0, 1500, TimeUnit.MILLISECONDS);

        emitter.onCompletion(() -> streamingTask.cancel(true));
        emitter.onTimeout(() -> streamingTask.cancel(true));
        emitter.onError((e) -> streamingTask.cancel(true));

        // Send initial connection state event
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of("publicId", publicId, "status", session.getStatus())));
        } catch (Exception e) {
            log.warn("Failed to send initial SSE event: {}", e.getMessage());
        }

        return emitter;
    }
}
