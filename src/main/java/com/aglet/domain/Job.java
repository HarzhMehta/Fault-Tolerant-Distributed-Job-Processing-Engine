package com.aglet.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class Job {

    private final UUID jobId;
    private Instant startedAt;
    private final Instant createdAt;
    private Instant completedAt;
    private JobState state;
    private String failureReason;

    private final List<Task> tasks;

    public Job() {
        this.jobId = UUID.randomUUID();
        this.createdAt = Instant.now();
        this.state = JobState.CREATED;
        this.tasks = new ArrayList<>();
    }

    public void start() {
        requireState(JobState.CREATED);

        state = JobState.RUNNING;
        startedAt = Instant.now();
    }

    public void complete() {
        requireState(JobState.RUNNING);

        state = JobState.COMPLETED;
        completedAt = Instant.now();
    }

    public void fail(String reason) {
        requireState(JobState.RUNNING);

        state = JobState.FAILED;
        failureReason = reason;
        completedAt = Instant.now();
    }

    public void cancel() {
        if (state != JobState.CREATED && state != JobState.RUNNING) {
            throw new IllegalStateException(
                    "Job cannot be cancelled from state: " + state);
        }

        state = JobState.CANCELLED;
        completedAt = Instant.now();
    }

    private void requireState(JobState expectedState) {
        if (state != expectedState) {
            throw new IllegalStateException(
                    "Expected " + expectedState + " but was " + state);
        }
    }

}