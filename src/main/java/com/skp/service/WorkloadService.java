package com.skp.service;

import com.skp.model.TaskQueryResponse;
import com.skp.model.TaskRecord;
import com.skp.model.TaskStatus;
import com.skp.model.TaskSubmissionResponse;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.RejectedExecutionException;

@Service
public class WorkloadService {

    private final DynamicThreadPoolManager threadPoolManager;
    private final Map<String, TaskRecord> taskStore = new ConcurrentHashMap<>();

    public WorkloadService(DynamicThreadPoolManager threadPoolManager) {
        this.threadPoolManager = threadPoolManager;
    }

    public TaskSubmissionResponse submitAsyncTask(long durationMs, String message) {
        long submittedAt = System.currentTimeMillis();
        String taskId = UUID.randomUUID().toString();
        TaskRecord record = new TaskRecord(taskId, submittedAt, durationMs, message);
        taskStore.put(taskId, record);

        Runnable task = () -> {
            long startAt = System.currentTimeMillis();
            record.setStartedAtMs(startAt);
            record.setStatus(TaskStatus.RUNNING);
            record.setThreadName(Thread.currentThread().getName());
            boolean degraded = !record.getThreadName().startsWith(threadPoolManager.poolThreadPrefix());
            if (degraded) {
                record.setDegraded(true);
            }

            try {
                Thread.sleep(durationMs);
                long finishAt = System.currentTimeMillis();
                record.setFinishedAtMs(finishAt);
                record.setStatus(TaskStatus.SUCCESS);

                threadPoolManager.recordTaskLatency(
                        startAt - submittedAt,
                        finishAt - startAt,
                        finishAt - submittedAt
                );
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                long finishAt = System.currentTimeMillis();
                record.setFinishedAtMs(finishAt);
                record.setStatus(TaskStatus.FAILED);
                record.setError("Interrupted: " + ex.getMessage());
            } catch (Exception ex) {
                long finishAt = System.currentTimeMillis();
                record.setFinishedAtMs(finishAt);
                record.setStatus(TaskStatus.FAILED);
                record.setError(ex.getMessage());
            }
        };

        try {
            threadPoolManager.resetSubmissionContext();
            threadPoolManager.executor().execute(task);
            boolean degradedResponse = threadPoolManager.consumeSubmissionDegradedFlag();
            if (degradedResponse) {
                record.setDegraded(true);
            }
            return new TaskSubmissionResponse(
                    taskId,
                    TaskStatus.SUBMITTED,
                    true,
                    degradedResponse,
                    degradedResponse ? "Task accepted with degraded strategy" : "Task accepted"
            );
        } catch (RejectedExecutionException ex) {
            long finishAt = System.currentTimeMillis();
            record.setFinishedAtMs(finishAt);
            record.setStatus(TaskStatus.REJECTED);
            record.setDegraded(true);
            record.setError(ex.getMessage());
            threadPoolManager.markDegraded();
            return new TaskSubmissionResponse(taskId, TaskStatus.REJECTED, false, true, "Task rejected, please retry");
        }
    }

    public Optional<TaskQueryResponse> queryTask(String taskId) {
        TaskRecord record = taskStore.get(taskId);
        if (record == null) {
            return Optional.empty();
        }

        Long wait = null;
        Long exec = null;
        Long total = null;

        if (record.getStartedAtMs() > 0) {
            wait = record.getStartedAtMs() - record.getSubmittedAtMs();
        }

        if (record.getStartedAtMs() > 0 && record.getFinishedAtMs() > 0) {
            exec = record.getFinishedAtMs() - record.getStartedAtMs();
            total = record.getFinishedAtMs() - record.getSubmittedAtMs();
        }

        return Optional.of(new TaskQueryResponse(
                record.getTaskId(),
                record.getStatus(),
                record.getThreadName(),
                record.getMessage(),
                record.getError(),
                record.isDegraded(),
                wait,
                exec,
                total
        ));
    }
}
