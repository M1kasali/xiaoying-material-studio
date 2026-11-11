package com.xiaoying.studio.task;

import com.xiaoying.studio.asset.Asset;
import com.xiaoying.studio.asset.AssetStorage;
import com.xiaoying.studio.catalog.TemplateMapper;
import java.io.IOException;
import java.nio.file.Files;
import org.springframework.stereotype.Component;

@Component
public class TaskWork {
    private final TaskMapper mapper;
    private final TemplateMapper templates;
    private final AssetStorage storage;

    public TaskWork(TaskMapper mapper, TemplateMapper templates, AssetStorage storage) {
        this.mapper = mapper;
        this.templates = templates;
        this.storage = storage;
    }

    public void validate(TaskRow task) {
        var assets = mapper.assets(task.id());
        var template = templates.findById(task.templateId());
        if (template == null || assets.size() != template.requiredImageCount()) throw new ProcessingFailure("素材数量不符合模板要求");
        for (var asset : assets) {
            if (!task.userId().equals(asset.userId()) || !Files.isReadable(storage.path(asset.storageKey()))) {
                throw new ProcessingFailure("任务素材文件不可用，请重新上传");
            }
        }
    }

    // The mock generator selects the first uploaded image; no external AI is used.
    public Asset generate(TaskRow task) {
        var assets = mapper.assets(task.id());
        if (assets.isEmpty()) throw new ProcessingFailure("没有可供生成的素材");
        return assets.getFirst();
    }

    public Output finish(TaskRow task, Asset generated) throws IOException {
        String extension = "image/png".equals(generated.mimeType()) ? ".png" : ".jpg";
        String key = "results/" + task.id() + extension;
        try { storage.copy(generated.storageKey(), key); }
        catch (IOException error) {
            try { storage.delete(key); } catch (IOException cleanup) { error.addSuppressed(cleanup); }
            throw error;
        }
        return new Output(key, generated.mimeType());
    }

    public void discard(Output output) throws IOException {
        if (output != null) storage.delete(output.key());
    }

    public record Output(String key, String mime) {}

    public static class ProcessingFailure extends RuntimeException {
        public ProcessingFailure(String message) { super(message); }
    }
}
