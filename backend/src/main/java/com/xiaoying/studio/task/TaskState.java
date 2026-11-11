package com.xiaoying.studio.task;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TaskState {
    private final TaskMapper mapper;
    public TaskState(TaskMapper mapper) { this.mapper = mapper; }

    public TaskRow claim(String id) { return mapper.claim(id) == 1 ? mapper.find(id) : null; }

    @Transactional
    public void begin(String id, Stage stage) {
        if (mapper.setStage(id, stage) != 1) throw new IllegalStateException("Task is no longer running");
        mapper.startStep(id, stage);
    }

    @Transactional
    public void complete(String id, Stage stage, TaskWork.Output output) {
        if (mapper.finishStep(id, stage) != 1) throw new IllegalStateException("Step is no longer running");
        if (stage == Stage.FINALIZE) {
            if (output == null || mapper.succeed(id, "/api/tasks/" + id + "/result", output.key(), output.mime()) != 1) {
                throw new IllegalStateException("Cannot complete task");
            }
        }
    }

    @Transactional
    public boolean fail(String id, Stage stage, String reason) {
        if (mapper.fail(id, stage, reason) != 1) return false;
        mapper.failRunningStep(id, reason);
        return true;
    }

    @Transactional
    public int interruptPreviousRun() {
        mapper.interruptSteps();
        return mapper.interruptTasks();
    }
}
