package com.xiaoying.studio.asset;

import com.xiaoying.studio.config.StudioProperties;
import java.io.IOException;
import java.nio.file.*;
import org.springframework.stereotype.Component;

@Component
public class AssetStorage {
    private final Path root;
    public AssetStorage(StudioProperties properties) throws IOException {
        root = properties.storageRoot().toAbsolutePath().normalize();
        Files.createDirectories(root.resolve("uploads"));
        Files.createDirectories(root.resolve("results"));
    }

    public Path path(String key) {
        if (key == null || !key.matches("(uploads|results)/[a-f0-9-]{36}\\.(png|jpg)")) {
            throw new IllegalArgumentException("Invalid storage key");
        }
        var path = root.resolve(key).normalize();
        if (!path.startsWith(root)) throw new IllegalArgumentException("Invalid storage path");
        return path;
    }

    public void write(String key, byte[] bytes) throws IOException {
        Files.write(path(key), bytes, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    }

    public void copy(String from, String to) throws IOException { Files.copy(path(from), path(to)); }

    public void delete(String key) throws IOException { Files.deleteIfExists(path(key)); }
}
