package com.xiaoying.studio.config;

import java.nio.file.Path;
import java.util.List;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("studio")
public record StudioProperties(Path storageRoot, boolean demoEnabled, boolean secureCookie,
        List<String> allowedOrigins, int workerCount, int queueCapacity, int defaultDelayMs) {
    public StudioProperties {
        if (workerCount < 1 || queueCapacity < 1 || defaultDelayMs < 0 || defaultDelayMs > 30000) {
            throw new IllegalArgumentException("Invalid worker, queue, or simulation configuration");
        }
    }
}
