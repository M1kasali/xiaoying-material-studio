package com.xiaoying.studio.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import com.xiaoying.studio.config.StudioProperties;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class TaskDispatcherTest {
    @Test
    void saturatedQueueProducesRecordedFailureInsteadOfRunningInRequestThread() throws Exception {
        var started = new CountDownLatch(1);
        var release = new CountDownLatch(1);
        var runner = mock(TaskRunner.class);
        var state = mock(TaskState.class);
        doAnswer(call -> { started.countDown(); release.await(); return null; }).when(runner).run("first");
        var dispatcher = new TaskDispatcher(new StudioProperties(Path.of("data"), true, false, List.of(), 1, 1, 0), runner, state);
        try {
            dispatcher.run(null);
            dispatcher.submit("first");
            assertTrue(started.await(5, TimeUnit.SECONDS));
            dispatcher.submit("second");
            dispatcher.submit("third");
            verify(state).fail("third", null, "处理队列已满或服务正在停止，请稍后重新提交");
            verify(runner, never()).run("second");
            verify(runner, never()).run("third");
        } finally {
            release.countDown();
            dispatcher.shutdown();
        }
    }
}
