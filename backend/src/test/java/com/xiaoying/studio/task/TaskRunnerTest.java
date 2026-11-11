package com.xiaoying.studio.task;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.*;
import com.xiaoying.studio.asset.Asset;
import java.io.IOException;
import java.time.Instant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class TaskRunnerTest {
    TaskState state;
    TaskWork work;
    StageDelay delay;
    TaskRunner runner;
    final Asset image = new Asset("image", "user", "photo.png", "uploads/photo.png", "image/png", 10, Instant.EPOCH);
    final TaskWork.Output output = new TaskWork.Output("results/task.png", "image/png");

    @BeforeEach
    void setup() throws Exception {
        state = mock(TaskState.class); work = mock(TaskWork.class); delay = mock(StageDelay.class);
        runner = new TaskRunner(state, work, delay);
        when(work.generate(any())).thenReturn(image);
        when(work.finish(any(), any())).thenReturn(output);
    }

    TaskRow task(Stage failAt) {
        return new TaskRow("task", "user", "template", "Test", TaskStatus.RUNNING, null, null, null, null,
                failAt, 20, Instant.EPOCH, Instant.EPOCH, null, null, null);
    }

    @Test
    void performsAllStagesInOrderWithPersistedBoundaries() throws Exception {
        var task = task(null);
        when(state.claim("task")).thenReturn(task);
        runner.run("task");
        var order = inOrder(state, work, delay);
        order.verify(state).claim("task");
        order.verify(state).begin("task", Stage.VALIDATE);
        order.verify(delay).pause(20);
        order.verify(work).validate(task);
        order.verify(state).complete("task", Stage.VALIDATE, null);
        order.verify(state).begin("task", Stage.GENERATE);
        order.verify(delay).pause(20);
        order.verify(work).generate(task);
        order.verify(state).complete("task", Stage.GENERATE, null);
        order.verify(state).begin("task", Stage.FINALIZE);
        order.verify(delay).pause(20);
        order.verify(work).finish(task, image);
        order.verify(state).complete("task", Stage.FINALIZE, output);
        verify(state, never()).fail(anyString(), any(), anyString());
    }

    @ParameterizedTest
    @EnumSource(Stage.class)
    void controlledFailureStopsAllLaterStages(Stage failed) throws Exception {
        when(state.claim("task")).thenReturn(task(failed));
        runner.run("task");
        verify(state).fail("task", failed, "模拟失败：" + failed);
        verify(state, never()).complete(eq("task"), eq(failed), any());
        for (Stage stage : Stage.values()) if (stage.ordinal() > failed.ordinal()) verify(state, never()).begin("task", stage);
        verify(work, never()).finish(any(), any());
        if (failed != Stage.FINALIZE) verify(work, never()).generate(any());
    }

    @Test
    void unexpectedGeneratorFailureDoesNotExposeInternalPathsOrRunFinalization() throws Exception {
        when(state.claim("task")).thenReturn(task(null));
        when(work.generate(any())).thenThrow(new IllegalStateException("secret/internal/path"));
        runner.run("task");
        verify(state).fail("task", Stage.GENERATE, "当前步骤处理失败，请重新提交");
        verify(state, never()).begin("task", Stage.FINALIZE);
        verify(work, never()).finish(any(), any());
    }

    @Test
    void interruptionStopsProcessingAndRestoresInterruptFlag() throws Exception {
        when(state.claim("task")).thenReturn(task(null));
        doThrow(new InterruptedException()).when(delay).pause(20);
        try {
            runner.run("task");
            assertTrue(Thread.currentThread().isInterrupted());
            verify(state).fail("task", Stage.VALIDATE, "服务停止，任务已中止，请重新提交");
            verify(work, never()).validate(any());
            verify(state, never()).begin("task", Stage.GENERATE);
        } finally { Thread.interrupted(); }
    }

    @Test
    void duplicateDispatchDoesNotRerunTask() {
        when(state.claim("task")).thenReturn(null);
        runner.run("task");
        verifyNoInteractions(work, delay);
        verify(state, never()).begin(anyString(), any());
    }

    @Test
    void lostCommitAcknowledgementDoesNotDeletePossiblySuccessfulResult() throws Exception {
        when(state.claim("task")).thenReturn(task(null));
        doThrow(new IllegalStateException("commit response lost")).when(state).complete("task", Stage.FINALIZE, output);
        when(state.fail(eq("task"), eq(Stage.FINALIZE), anyString())).thenReturn(false);
        runner.run("task");
        verify(work, never()).discard(any());
    }
}
