package com.xiaoying.studio.task;

import com.xiaoying.studio.config.StudioProperties;
import jakarta.annotation.PreDestroy;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

@Component
public class TaskDispatcher implements ApplicationRunner {
    private static final Logger LOG = LoggerFactory.getLogger(TaskDispatcher.class);
    private final ThreadPoolExecutor executor;
    private final TaskRunner runner;
    private final TaskState state;
    private volatile boolean accepting;

    public TaskDispatcher(StudioProperties properties, TaskRunner runner, TaskState state) {
        this.runner = runner;
        this.state = state;
        AtomicInteger sequence = new AtomicInteger();
        executor = new ThreadPoolExecutor(properties.workerCount(), properties.workerCount(), 0, TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(properties.queueCapacity()), runnable -> {
                    var thread = new Thread(runnable, "production-task-" + sequence.incrementAndGet());
                    thread.setDaemon(true);
                    return thread;
                }, new ThreadPoolExecutor.AbortPolicy());
    }

    @Override
    public void run(ApplicationArguments args) {
        int interrupted = state.interruptPreviousRun();
        if (interrupted > 0) LOG.info("Marked {} tasks from a previous process as interrupted", interrupted);
        accepting = true;
    }

    public boolean accepting() { return accepting; }

    public void submit(String id) {
        try {
            if (!accepting) throw new RejectedExecutionException("Shutting down");
            executor.execute(() -> runner.run(id));
        } catch (RejectedExecutionException rejected) {
            state.fail(id, null, "处理队列已满或服务正在停止，请稍后重新提交");
        }
    }

    @PreDestroy
    public void shutdown() {
        accepting = false;
        executor.shutdownNow();
    }
}
