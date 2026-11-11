package com.xiaoying.studio.task;

import java.time.Instant;

public record TaskStep(Stage stage, String status, Instant startedAt, Instant finishedAt, String errorMessage) {}
