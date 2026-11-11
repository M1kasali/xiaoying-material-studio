package com.xiaoying.studio.asset;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.ImageIO;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AssetService {
    public static final long MAX_BYTES = 5L * 1024 * 1024;
    private final AssetMapper mapper;
    private final AssetStorage storage;

    public AssetService(AssetMapper mapper, AssetStorage storage) { this.mapper = mapper; this.storage = storage; }

    public Asset upload(String userId, MultipartFile file) throws IOException {
        if (file.isEmpty()) throw badImage("请选择非空图片");
        if (file.getSize() > MAX_BYTES) throw new ResponseStatusException(HttpStatus.PAYLOAD_TOO_LARGE, "每张图片不能超过 5 MB");
        String declared = file.getContentType();
        if (!"image/jpeg".equals(declared) && !"image/png".equals(declared)) throw unsupported();
        byte[] bytes = file.getBytes();
        String extension;
        try (var input = ImageIO.createImageInputStream(new ByteArrayInputStream(bytes))) {
            var readers = ImageIO.getImageReaders(input);
            if (!readers.hasNext()) throw badImage("文件不是有效图片");
            var reader = readers.next();
            try {
                reader.setInput(input, true, true);
                String format = reader.getFormatName().toLowerCase(Locale.ROOT);
                String actual = switch (format) {
                    case "png" -> "image/png";
                    case "jpeg", "jpg" -> "image/jpeg";
                    default -> throw unsupported();
                };
                if (!actual.equals(declared)) throw unsupported();
                int width = reader.getWidth(0), height = reader.getHeight(0);
                if (width < 1 || height < 1 || width > 10000 || height > 10000 || (long) width * height > 20_000_000) {
                    throw badImage("图片尺寸过大，请使用不超过 2000 万像素的图片");
                }
                if (reader.read(0) == null) throw badImage("图片无法解码，请更换文件");
                extension = actual.equals("image/png") ? ".png" : ".jpg";
            } finally { reader.dispose(); }
        } catch (IOException error) { throw badImage("图片内容损坏，请更换文件"); }
        String id = UUID.randomUUID().toString();
        String key = "uploads/" + id + extension;
        String name = file.getOriginalFilename() == null ? "image" + extension : file.getOriginalFilename();
        name = name.replace('\\', '/');
        name = name.substring(name.lastIndexOf('/') + 1).replaceAll("\\p{Cntrl}", "");
        if (name.isBlank()) name = "image" + extension;
        if (name.length() > 200) name = name.substring(0, 200);
        var asset = new Asset(id, userId, name, key, declared, bytes.length, Instant.now());
        try {
            storage.write(key, bytes);
            mapper.insert(asset);
            return asset;
        } catch (IOException | RuntimeException error) {
            try { storage.delete(key); } catch (IOException cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
    }

    public Asset owned(String id, String userId) {
        var asset = mapper.findOwned(id, userId);
        if (asset == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "素材不存在");
        return asset;
    }

    private ResponseStatusException unsupported() {
        return new ResponseStatusException(HttpStatus.UNSUPPORTED_MEDIA_TYPE, "仅支持内容与类型一致的 JPEG 或 PNG 图片");
    }

    private ResponseStatusException badImage(String message) { return new ResponseStatusException(HttpStatus.BAD_REQUEST, message); }
}
