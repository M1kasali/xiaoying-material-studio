package com.xiaoying.studio.task;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class TaskQueries {
    private final TaskMapper mapper;
    public TaskQueries(TaskMapper mapper) { this.mapper = mapper; }

    public TaskRow owned(String id, String userId) {
        var row = mapper.findOwned(id, userId);
        if (row == null) throw new ResponseStatusException(HttpStatus.NOT_FOUND, "任务不存在");
        return row;
    }

    @Transactional(readOnly = true)
    public TaskResponse get(String id, String userId) {
        return response(owned(id, userId));
    }

    @Transactional(readOnly = true)
    public Page list(String userId, String cursor, int limit) {
        if (limit < 1 || limit > 50) throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "每页数量应为 1～50");
        Instant beforeTime = null;
        String beforeId = null;
        if (cursor != null) {
            try {
                if (cursor.length() > 200) throw new IllegalArgumentException();
                String[] parts = new String(Base64.getUrlDecoder().decode(cursor), StandardCharsets.UTF_8).split("\\|", -1);
                if (parts.length != 2) throw new IllegalArgumentException();
                long milliseconds = Long.parseLong(parts[0]);
                if (milliseconds < 0 || milliseconds > 253402300799999L) throw new IllegalArgumentException();
                beforeTime = Instant.ofEpochMilli(milliseconds);
                beforeId = UUID.fromString(parts[1]).toString();
            } catch (RuntimeException error) { throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "分页游标无效"); }
        }
        var rows = mapper.list(userId, beforeTime, beforeId, limit + 1);
        var visible = rows.subList(0, Math.min(limit, rows.size()));
        String nextCursor = null;
        if (rows.size() > limit) {
            var last = visible.getLast();
            nextCursor = Base64.getUrlEncoder().withoutPadding().encodeToString(
                    (last.createdAt().toEpochMilli() + "|" + last.id()).getBytes(StandardCharsets.UTF_8));
        }
        return new Page(visible.stream().map(this::response).toList(), nextCursor);
    }

    private TaskResponse response(TaskRow row) { return TaskResponse.from(row, mapper.steps(row.id())); }
    public record Page(List<TaskResponse> items, String nextCursor) {}
}
