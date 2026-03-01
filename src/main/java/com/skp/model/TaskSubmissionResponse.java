package com.skp.model;

public class TaskSubmissionResponse {

    private final String taskId;
    private final TaskStatus status;
    private final boolean accepted;
    private final boolean degraded;
    private final String message;

    public TaskSubmissionResponse(String taskId, TaskStatus status, boolean accepted, boolean degraded, String message) {
        this.taskId = taskId;
        this.status = status;
        this.accepted = accepted;
        this.degraded = degraded;
        this.message = message;
    }

    public String getTaskId() {
        return taskId;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public boolean isAccepted() {
        return accepted;
    }

    public boolean isDegraded() {
        return degraded;
    }

    public String getMessage() {
        return message;
    }
}
