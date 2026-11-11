package com.xiaoying.studio.asset;

import com.xiaoying.studio.auth.AuthenticationFilter;
import com.xiaoying.studio.auth.User;
import java.io.IOException;
import java.nio.file.Files;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/assets")
public class AssetController {
    private final AssetService assets;
    private final AssetStorage storage;
    public AssetController(AssetService assets, AssetStorage storage) { this.assets = assets; this.storage = storage; }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<AssetResponse> upload(@RequestAttribute(AuthenticationFilter.USER) User user,
            @RequestParam("file") MultipartFile file) throws IOException {
        var asset = assets.upload(user.id(), file);
        return ResponseEntity.status(HttpStatus.CREATED).body(new AssetResponse(asset.id(), asset.originalName(),
                asset.mimeType(), asset.sizeBytes(), "/api/assets/" + asset.id() + "/content"));
    }

    @GetMapping("/{id}/content")
    public ResponseEntity<Resource> content(@RequestAttribute(AuthenticationFilter.USER) User user, @PathVariable String id) {
        var asset = assets.owned(id, user.id());
        var path = storage.path(asset.storageKey());
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "素材文件已不可用");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(asset.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline").body(new FileSystemResource(path));
    }

    public record AssetResponse(String id, String name, String mimeType, long sizeBytes, String previewUrl) {}
}
