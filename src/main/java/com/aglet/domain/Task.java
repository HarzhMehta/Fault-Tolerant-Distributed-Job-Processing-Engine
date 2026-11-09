package com.aglet.domain;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class Task {
    // class props
    private final UUID jobId;
    private final UUID taskId;
    private final Instant createdAt;
    private final List<UUID> dependencies;
    private final Object input;
    private Object result;
    private int attemptCount;
    private final int priority;
    private Instant completedAt;
    private TaskState state;
    private String failureReason;

    // class constr
    public Task(UUID jobId, List<UUID> dependencies,
            Object input, int priority) {
        this.jobId = jobId;
        this.dependencies = List.copyOf(dependencies);
        this.input = input;
        this.priority = priority;

        this.attemptCount = 0;
        this.taskId = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.state = TaskState.PENDING;

    }

    // class methods
    public void start() {
        requireState(TaskState.PENDING);
        state = TaskState.RUNNING;
        attemptCount++;
    }

    public void complete(Object result) {
        requireState(TaskState.RUNNING);
        this.result = result;
        this.state = TaskState.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail(String reason) {
        requireState(TaskState.RUNNING);
        this.failureReason = reason;
        this.state = TaskState.FAILED;
    }

    public void retry() {
        requireState(TaskState.FAILED);
        this.state = TaskState.PENDING;

    }

    public void cancel() {
        if (state != TaskState.PENDING && state != TaskState.RUNNING) {
            throw new IllegalStateException(
                    "Task cannot be cancelled from state: " + state);
        }
        this.state = TaskState.CANCELLED;
        this.completedAt = Instant.now();
    }

    private void requireState(TaskState expectedState) {
        if (state != expectedState) {
            throw new IllegalStateException(
                    "Expected" + expectedState + " but was " + state);
        }
    }

    // getters
    public UUID getTaskId() {
        return taskId;
    }

    public List<UUID> getDependencies() {
        return dependencies;
    }

    public TaskState getState() {
        return state;
    }
}
