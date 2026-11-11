package com.xiaoying.studio.task;

import com.xiaoying.studio.asset.Asset;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

@Component
public class TaskRunner {
    private static final Logger LOG = LoggerFactory.getLogger(TaskRunner.class);
    private final TaskState state;
    private final TaskWork work;
    private final StageDelay delay;

    public TaskRunner(TaskState state, TaskWork work, StageDelay delay) {
        this.state = state;
        this.work = work;
        this.delay = delay;
    }

    public void run(String id) {
        Stage stage = null;
        TaskWork.Output output = null;
        try {
            var task = state.claim(id);
            if (task == null) return; // Duplicate dispatch cannot rerun a terminal task.
            Asset generated = null;
            for (Stage next : Stage.values()) {
                stage = next;
                state.begin(id, stage);
                delay.pause(task.simulationDelayMs());
                if (stage == task.simulationFailureStage()) throw new TaskWork.ProcessingFailure("模拟失败：" + stage);
                switch (stage) {
                    case VALIDATE -> work.validate(task);
                    case GENERATE -> generated = work.generate(task);
                    case FINALIZE -> output = work.finish(task, generated);
                }
                state.complete(id, stage, output);
            }
        } catch (Exception error) {
            if (error instanceof InterruptedException) Thread.currentThread().interrupt();
            String reason = error instanceof TaskWork.ProcessingFailure ? error.getMessage()
                    : error instanceof InterruptedException ? "服务停止，任务已中止，请重新提交" : "当前步骤处理失败，请重新提交";
            LOG.warn("Task {} failed at {}", id, stage, error);
            boolean recorded = false;
            try { recorded = state.fail(id, stage, reason); }
            catch (Exception persistenceError) { LOG.error("Cannot persist failure for task {}", id, persistenceError); }
            // If a commit acknowledgement was lost, never delete a possibly successful result.
            if (recorded) {
                try { work.discard(output); }
                catch (Exception cleanupError) { LOG.warn("Cannot clean result for task {}", id, cleanupError); }
            }
        }
    }
}
