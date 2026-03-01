package com.skp.model;

public class TaskQueryResponse {

    private final String taskId;
    private final TaskStatus status;
    private final String threadName;
    private final String message;
    private final String error;
    private final boolean degraded;
    private final Long waitLatencyMs;
    private final Long execLatencyMs;
    private final Long totalLatencyMs;

    public TaskQueryResponse(String taskId,
                             TaskStatus status,
                             String threadName,
                             String message,
                             String error,
                             boolean degraded,
                             Long waitLatencyMs,
                             Long execLatencyMs,
                             Long totalLatencyMs) {
        this.taskId = taskId;
        this.status = status;
        this.threadName = threadName;
        this.message = message;
        this.error = error;
        this.degraded = degraded;
        this.waitLatencyMs = waitLatencyMs;
        this.execLatencyMs = execLatencyMs;
        this.totalLatencyMs = totalLatencyMs;
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public String getThreadName() {
        return threadName;
    }

    public String getMessage() {
        return message;
    }

    public String getError() {
        return error;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public Long getWaitLatencyMs() {
        return waitLatencyMs;
    }

    public Long getExecLatencyMs() {
        return execLatencyMs;
    }

    public Long getTotalLatencyMs() {
        return totalLatencyMs;
    }
}
