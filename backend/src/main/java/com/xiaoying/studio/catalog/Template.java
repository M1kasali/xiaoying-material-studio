package com.xiaoying.studio.catalog;

public record Template(
        String id, String title, String category, String description,
        String coverUrl, int requiredImageCount, long maxFileSizeBytes) {}
