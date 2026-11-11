package com.xiaoying.studio.task;

import java.time.Instant;

public record TaskRow(String id, String userId, String templateId, String templateTitle,
        TaskStatus status, Stage currentStage, Stage failureStage, String failureReason,
        String resultUrl, Stage simulationFailureStage, int simulationDelayMs,
        Instant createdAt, Instant updatedAt, Instant finishedAt, String resultStorageKey, String resultMimeType) {}
