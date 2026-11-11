package com.xiaoying.studio.task;

import com.xiaoying.studio.asset.AssetStorage;
import com.xiaoying.studio.auth.AuthenticationFilter;
import com.xiaoying.studio.auth.User;
import jakarta.validation.Valid;
import java.nio.file.Files;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {
    private final TaskSubmission submissions;
    private final TaskQueries queries;
    private final AssetStorage storage;
    public TaskController(TaskSubmission submissions, TaskQueries queries, AssetStorage storage) {
        this.submissions = submissions; this.queries = queries; this.storage = storage;
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(@RequestAttribute(AuthenticationFilter.USER) User user,
            @Valid @RequestBody TaskSubmission.Request request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(submissions.create(user.id(), request));
    }

    @GetMapping
    public TaskQueries.Page list(@RequestAttribute(AuthenticationFilter.USER) User user,
            @RequestParam(required = false) String cursor, @RequestParam(defaultValue = "20") int limit) {
        return queries.list(user.id(), cursor, limit);
    }

    @GetMapping("/{id}")
    public TaskResponse get(@RequestAttribute(AuthenticationFilter.USER) User user, @PathVariable String id) {
        return queries.get(id, user.id());
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<Resource> result(@RequestAttribute(AuthenticationFilter.USER) User user, @PathVariable String id) {
        var task = queries.owned(id, user.id());
        if (task.status() != TaskStatus.SUCCEEDED || task.resultStorageKey() == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "制作结果尚不可用");
        }
        var path = storage.path(task.resultStorageKey());
        if (!Files.isRegularFile(path)) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "结果文件不可用");
        return ResponseEntity.ok().contentType(MediaType.parseMediaType(task.resultMimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline").body(new FileSystemResource(path));
    }
}
