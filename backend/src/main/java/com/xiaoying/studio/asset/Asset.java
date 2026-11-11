package com.xiaoying.studio.asset;

import java.time.Instant;

public record Asset(String id, String userId, String originalName, String storageKey,
        String mimeType, long sizeBytes, Instant createdAt) {}
