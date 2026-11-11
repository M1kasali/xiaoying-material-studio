package com.xiaoying.studio.task;

import com.xiaoying.studio.asset.AssetService;
import com.xiaoying.studio.catalog.TemplateMapper;
import com.xiaoying.studio.config.StudioProperties;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskSubmission {
    private final TaskMapper mapper;
    private final TemplateMapper templates;
    private final AssetService assets;
    private final TransactionTemplate transactions;
    private final TaskDispatcher dispatcher;
    private final TaskQueries queries;
    private final StudioProperties properties;

    public TaskSubmission(TaskMapper mapper, TemplateMapper templates, AssetService assets, TransactionTemplate transactions,
            TaskDispatcher dispatcher, TaskQueries queries, StudioProperties properties) {
        this.mapper = mapper;
        this.templates = templates;
        this.assets = assets;
        this.transactions = transactions;
        this.dispatcher = dispatcher;
        this.queries = queries;
        this.properties = properties;
    }

    public TaskResponse create(String userId, Request request) {
        if (!dispatcher.accepting()) throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "服务正在启动或停止，请稍后再试");
        if (request.simulation() != null && !properties.demoEnabled()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前环境未启用模拟参数");
        }
        int delay = request.simulation() == null || request.simulation().delayMs() == null
                ? properties.defaultDelayMs() : request.simulation().delayMs();
        Stage failure = request.simulation() == null ? null : request.simulation().failAt();
        String id = UUID.randomUUID().toString();
        transactions.executeWithoutResult(transaction -> {
            var template = templates.findById(request.templateId());
            if (template == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "模板不存在");
            if (request.assetIds().size() != template.requiredImageCount()
                    || new HashSet<>(request.assetIds()).size() != request.assetIds().size()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请按模板要求上传数量正确且不重复的图片");
            }
            for (String assetId : request.assetIds()) {
                var asset = assets.owned(assetId, userId);
                if (asset.sizeBytes() > template.maxFileSizeBytes()
                        || (!asset.mimeType().equals("image/jpeg") && !asset.mimeType().equals("image/png"))) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "素材不符合模板要求");
                }
            }
            mapper.insert(id, userId, request.templateId(), failure, delay);
            for (int slot = 0; slot < request.assetIds().size(); slot++) mapper.addAsset(id, request.assetIds().get(slot), slot);
        });
        // executeWithoutResult returns only after commit. No background worker can see an uncommitted task.
        dispatcher.submit(id);
        return queries.get(id, userId);
    }

    public record Request(@NotBlank String templateId, @NotNull @Size(min = 1, max = 4) List<@NotBlank String> assetIds,
            @Valid Simulation simulation) {}
    public record Simulation(@Min(0) @Max(30000) Integer delayMs, Stage failAt) {}
}
