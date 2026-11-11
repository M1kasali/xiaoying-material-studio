package com.xiaoying.studio.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import com.xiaoying.studio.asset.Asset;
import com.xiaoying.studio.asset.AssetService;
import com.xiaoying.studio.catalog.Template;
import com.xiaoying.studio.catalog.TemplateMapper;
import com.xiaoying.studio.config.StudioProperties;
import java.nio.file.Path;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;
import org.springframework.transaction.support.TransactionTemplate;

class TaskSubmissionTest {
    @Test
    void dispatchOnlyHappensAfterSuccessfulCommit() {
        verifyCommitBoundary(false);
    }

    @Test
    void failedCommitDoesNotDispatchAnUncommittedTask() {
        verifyCommitBoundary(true);
    }

    private void verifyCommitBoundary(boolean failCommit) {
        var committed = new AtomicBoolean();
        var manager = new AbstractPlatformTransactionManager() {
            protected Object doGetTransaction() { return new Object(); }
            protected void doBegin(Object transaction, TransactionDefinition definition) {}
            protected void doCommit(DefaultTransactionStatus status) {
                if (failCommit) throw new TransactionSystemException("commit failed");
                committed.set(true);
            }
            protected void doRollback(DefaultTransactionStatus status) {}
        };
        var mapper = mock(TaskMapper.class);
        var templates = mock(TemplateMapper.class);
        var assets = mock(AssetService.class);
        var dispatcher = mock(TaskDispatcher.class);
        var queries = mock(TaskQueries.class);
        when(dispatcher.accepting()).thenReturn(true);
        when(templates.findById("template")).thenReturn(new Template("template", "Test", "demo", "demo", "/demo.png", 1, 100));
        when(assets.owned("asset", "user")).thenReturn(new Asset("asset", "user", "x.png", "uploads/x.png", "image/png", 10, Instant.EPOCH));
        doAnswer(call -> { assertFalse(committed.get()); return null; }).when(mapper).insert(anyString(), anyString(), anyString(), any(), anyInt());
        doAnswer(call -> { assertTrue(committed.get()); return null; }).when(dispatcher).submit(anyString());
        var properties = new StudioProperties(Path.of("data"), true, false, List.of(), 2, 32, 2000);
        var service = new TaskSubmission(mapper, templates, assets, new TransactionTemplate(manager), dispatcher, queries, properties);
        var request = new TaskSubmission.Request("template", List.of("asset"), null);
        if (failCommit) {
            assertThrows(TransactionSystemException.class, () -> service.create("user", request));
            verify(dispatcher, never()).submit(anyString());
        } else {
            service.create("user", request);
            verify(dispatcher).submit(anyString());
        }
    }
}
