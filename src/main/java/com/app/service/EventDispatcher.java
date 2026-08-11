package com.app.service;

import com.app.enums.ExecutionState;
import com.app.model.orchestrator.OrchestratorEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class EventDispatcher {

    private static final Logger log = LoggerFactory.getLogger(EventDispatcher.class);
    private final Map<UUID, List<OrchestratorEvent>> eventLog = new ConcurrentHashMap<>();

    public OrchestratorEvent dispatchEvent(UUID executionId, String eventType, ExecutionState state, String message) {
        OrchestratorEvent event = OrchestratorEvent.builder()
            .eventId(UUID.randomUUID().toString())
            .executionId(executionId)
            .eventType(eventType)
            .currentState(state)
            .message(message)
            .timestamp(Instant.now())
            .build();

        eventLog.computeIfAbsent(executionId, k -> Collections.synchronizedList(new ArrayList<>())).add(event);
        log.info("Orchestrator Event [{}]: ExecutionId={}, State={}, Message={}", eventType, executionId, state, message);
        return event;
    }

    public List<OrchestratorEvent> getEvents(UUID executionId) {
        return eventLog.getOrDefault(executionId, List.of());
    }
}
