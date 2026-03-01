package com.skp.model;

public class TaskRecord {

    private final String taskId;
    private final long submittedAtMs;
    private final long durationMs;
    private final String message;

    private volatile TaskStatus status;
    private volatile String threadName;
    private volatile String error;
    private volatile boolean degraded;
    private volatile long startedAtMs;
    private volatile long finishedAtMs;

    public TaskRecord(String taskId, long submittedAtMs, long durationMs, String message) {
        this.taskId = taskId;
        this.submittedAtMs = submittedAtMs;
        this.durationMs = durationMs;
        this.message = message;
        this.status = TaskStatus.SUBMITTED;
    }

    public String getTaskId() {
        return taskId;
    }

    public long getSubmittedAtMs() {
        return submittedAtMs;
    }

    public long getDurationMs() {
        return durationMs;
    }

    public String getMessage() {
        return message;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public void setStatus(TaskStatus status) {
        this.status = status;
    }

    public String getThreadName() {
        return threadName;
    }

    public void setThreadName(String threadName) {
        this.threadName = threadName;
    }

    public String getError() {
        return error;
    }

    public void setError(String error) {
        this.error = error;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public void setDegraded(boolean degraded) {
        this.degraded = degraded;
    }

    public long getStartedAtMs() {
        return startedAtMs;
    }

    public void setStartedAtMs(long startedAtMs) {
        this.startedAtMs = startedAtMs;
    }

    public long getFinishedAtMs() {
        return finishedAtMs;
    }

    public void setFinishedAtMs(long finishedAtMs) {
        this.finishedAtMs = finishedAtMs;
    }
}
