package com.xiaoying.studio.asset;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.xiaoying.studio.config.StudioProperties;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

class AssetServiceTest {
    @TempDir Path root;
    AssetMapper mapper;
    AssetService service;
    AssetStorage storage;

    @BeforeEach
    void setup() throws Exception {
        mapper = mock(AssetMapper.class);
        storage = new AssetStorage(new StudioProperties(root, true, false, List.of(), 2, 32, 2000));
        service = new AssetService(mapper, storage);
    }

    byte[] png() throws Exception {
        var output = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(8, 8, BufferedImage.TYPE_INT_RGB), "png", output);
        return output.toByteArray();
    }

    @Test
    void storesValidImageWithServerGeneratedPath() throws Exception {
        byte[] bytes = png();
        var asset = service.upload("user", new MockMultipartFile("file", "../../outside.png", "image/png", bytes));
        assertEquals("outside.png", asset.originalName());
        assertTrue(storage.path(asset.storageKey()).startsWith(root));
        assertArrayEquals(bytes, Files.readAllBytes(storage.path(asset.storageKey())));
        verify(mapper).insert(asset);
    }

    @Test
    void rejectsMimeMismatchAndDisguisedNonImages() throws Exception {
        var mismatch = assertThrows(ResponseStatusException.class, () -> service.upload("user",
                new MockMultipartFile("file", "x.jpg", "image/jpeg", png())));
        assertEquals(415, mismatch.getStatusCode().value());
        var invalid = assertThrows(ResponseStatusException.class, () -> service.upload("user",
                new MockMultipartFile("file", "x.png", "image/png", "not an image".getBytes())));
        assertEquals(400, invalid.getStatusCode().value());
        verifyNoInteractions(mapper);
    }

    @Test
    void rejectsOversizedUploadsBeforeDecoding() {
        var error = assertThrows(ResponseStatusException.class, () -> service.upload("user",
                new MockMultipartFile("file", "x.png", "image/png", new byte[(int) AssetService.MAX_BYTES + 1])));
        assertEquals(413, error.getStatusCode().value());
        verifyNoInteractions(mapper);
    }

    @Test
    void removesFileIfDatabaseWriteFails() throws Exception {
        doThrow(new IllegalStateException("database unavailable")).when(mapper).insert(any());
        assertThrows(IllegalStateException.class, () -> service.upload("user",
                new MockMultipartFile("file", "x.png", "image/png", png())));
        try (var files = Files.list(root.resolve("uploads"))) { assertEquals(0, files.count()); }
    }
}
