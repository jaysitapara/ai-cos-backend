package com.app.service;

import com.app.enums.RuntimeState;
import com.app.model.runtime.RuntimeContextModel;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class ExecutionRuntimeService {

    private final Map<String, RuntimeContextModel> runtimeStore = new ConcurrentHashMap<>();

    public RuntimeContextModel startRuntime(String planId) {
        String runtimeId = "rt-" + UUID.randomUUID();

        RuntimeContextModel context = RuntimeContextModel.builder()
            .runtimeId(runtimeId)
            .executionId(UUID.randomUUID())
            .planId(planId)
            .workflowId("wf-default")
            .status(RuntimeState.RUNNING)
            .overallProgressPercent(50.0)
            .completedTasksCount(1)
            .totalTasksCount(2)
            .activeTaskName("System Architecture & Verification Check")
            .createdAt(Instant.now())
            .updatedAt(Instant.now())
            .build();

        runtimeStore.put(runtimeId, context);
        return context;
    }

    public Optional<RuntimeContextModel> getRuntime(String runtimeId) {
        return Optional.ofNullable(runtimeStore.get(runtimeId));
    }

    public RuntimeContextModel pauseRuntime(String runtimeId) {
        RuntimeContextModel rt = runtimeStore.get(runtimeId);
        if (rt != null) {
            rt.setStatus(RuntimeState.PAUSED);
            rt.setUpdatedAt(Instant.now());
        }
        return rt;
    }

    public RuntimeContextModel resumeRuntime(String runtimeId) {
        RuntimeContextModel rt = runtimeStore.get(runtimeId);
        if (rt != null) {
            rt.setStatus(RuntimeState.RESUMED);
            rt.setStatus(RuntimeState.RUNNING);
            rt.setUpdatedAt(Instant.now());
        }
        return rt;
    }

    public RuntimeContextModel cancelRuntime(String runtimeId) {
        RuntimeContextModel rt = runtimeStore.get(runtimeId);
        if (rt != null) {
            rt.setStatus(RuntimeState.CANCELLED);
            rt.setUpdatedAt(Instant.now());
        }
        return rt;
    }
}
