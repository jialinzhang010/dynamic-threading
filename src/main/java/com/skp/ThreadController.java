package com.skp;

import com.skp.model.PoolMetrics;
import com.skp.model.PoolUpdateRequest;
import com.skp.model.PoolUpdateResponse;
import com.skp.model.TaskQueryResponse;
import com.skp.model.TaskSubmissionResponse;
import com.skp.service.DynamicThreadPoolManager;
import com.skp.service.WorkloadService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class ThreadController {

    private final WorkloadService workloadService;
    private final DynamicThreadPoolManager threadPoolManager;

    public ThreadController(WorkloadService workloadService, DynamicThreadPoolManager threadPoolManager) {
        this.workloadService = workloadService;
        this.threadPoolManager = threadPoolManager;
    }

    @PostMapping("/threads")
    public ResponseEntity<TaskSubmissionResponse> submitTask(@RequestParam(defaultValue = "200") long durationMs,
                                                             @RequestParam(defaultValue = "demo-task") String message) {
        TaskSubmissionResponse response = workloadService.submitAsyncTask(durationMs, message);
        if (!response.isAccepted()) {
            return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS).body(response);
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(response);
    }

    @GetMapping("/tasks/{id}")
    public ResponseEntity<TaskQueryResponse> queryTask(@PathVariable("id") String taskId) {
        return workloadService.queryTask(taskId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping("/pool/metrics")
    public PoolMetrics metrics() {
        return threadPoolManager.currentMetrics();
    }

    @PostMapping("/pool/monitor")
    public ResponseEntity<Map<String, String>> triggerMonitor() {
        return ResponseEntity.ok(Map.of("action", threadPoolManager.evaluateAndScale()));
    }

    @PutMapping("/pools/{name}")
    public ResponseEntity<PoolUpdateResponse> updatePool(@PathVariable("name") String poolName,
                                                         @RequestBody PoolUpdateRequest request) {
        try {
            return ResponseEntity.ok(threadPoolManager.updatePool(poolName, request));
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().build();
        }
    }
}
