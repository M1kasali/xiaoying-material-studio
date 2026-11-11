package com.xiaoying.studio.task;

import java.time.Instant;
import java.util.List;

public record TaskResponse(String id, String templateId, String templateTitle, TaskStatus status,
        Stage currentStage, Instant createdAt, Instant updatedAt, Instant finishedAt,
        String resultUrl, Stage failureStage, String failureReason, List<TaskStep> steps) {
    public static TaskResponse from(TaskRow row, List<TaskStep> steps) {
        return new TaskResponse(row.id(), row.templateId(), row.templateTitle(), row.status(), row.currentStage(),
                row.createdAt(), row.updatedAt(), row.finishedAt(), row.resultUrl(), row.failureStage(), row.failureReason(), steps);
    }
}
