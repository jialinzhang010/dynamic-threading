package com.skp.service;

import com.skp.DynamicThreadPoolExecutor;
import com.skp.config.ThreadPoolProperties;
import com.skp.model.PoolMetrics;
import com.skp.model.PoolUpdateRequest;
import com.skp.model.PoolUpdateResponse;
import com.skp.model.RejectionPolicyType;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import javax.annotation.PreDestroy;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
public class DynamicThreadPoolManager {

    private final ThreadPoolProperties properties;
    private final MeterRegistry meterRegistry;

    private final AtomicReference<DynamicThreadPoolExecutor> executorRef = new AtomicReference<>();
    private final List<DynamicThreadPoolExecutor> drainingExecutors = new ArrayList<>();

    private final AtomicLong rejectedEvents = new AtomicLong();
    private final AtomicLong degradedEvents = new AtomicLong();
    private final ThreadLocal<Boolean> submissionDegradedFlag = ThreadLocal.withInitial(() -> false);

    private final Timer waitLatencyTimer;
    private final Timer execLatencyTimer;
    private final Timer totalLatencyTimer;

    private volatile long lastScaleTimestamp = 0L;
    private volatile String lastScaleAction = "INIT";
    private volatile String poolName;
    private volatile int queueCapacity;
    private volatile long keepAliveSeconds;
    private volatile RejectionPolicyType rejectionPolicy;

    public DynamicThreadPoolManager(ThreadPoolProperties properties, MeterRegistry meterRegistry) {
        this.properties = properties;
        this.meterRegistry = meterRegistry;

        this.poolName = properties.getPoolName();
        this.queueCapacity = properties.getQueueCapacity();
        this.keepAliveSeconds = properties.getKeepAliveSeconds();
        this.rejectionPolicy = RejectionPolicyType.from(properties.getRejectionPolicy());

        int core = Math.max(properties.getMinCoreSize(), properties.getInitialCoreSize());
        int max = Math.min(Math.max(core, properties.getInitialMaxSize()), properties.getMaxCoreSize());

        DynamicThreadPoolExecutor initialExecutor = buildExecutor(core, max, queueCapacity, keepAliveSeconds, rejectionPolicy);
        executorRef.set(initialExecutor);

        this.waitLatencyTimer = Timer.builder("threadpool_task_latency")
                .tag("pool", poolName)
                .tag("phase", "wait")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
        this.execLatencyTimer = Timer.builder("threadpool_task_latency")
                .tag("pool", poolName)
                .tag("phase", "exec")
                .publishPercentileHistogram(true)
                .register(meterRegistry);
        this.totalLatencyTimer = Timer.builder("threadpool_task_latency")
                .tag("pool", poolName)
                .tag("phase", "total")
                .publishPercentileHistogram(true)
                .register(meterRegistry);

        registerGauges();
    }

    @Scheduled(fixedDelayString = "${threadpool.monitor-interval-ms:1000}")
    public void monitorAndScale() {
        evaluateAndScale();
        cleanupDrainingExecutors();
    }

    public synchronized String evaluateAndScale() {
        DynamicThreadPoolExecutor threadPool = executor();

        long now = System.currentTimeMillis();
        if (now - lastScaleTimestamp < properties.getCooldownMs()) {
            return "COOLDOWN";
        }

        int core = threadPool.getCorePoolSize();
        int max = threadPool.getMaximumPoolSize();
        int active = threadPool.getActiveCount();
        int queueSize = threadPool.getQueue().size();
        int currentQueueCapacity = queueSize + threadPool.getQueue().remainingCapacity();

        double utilization = core == 0 ? 0.0 : (double) active / core;
        double queuePressure = currentQueueCapacity == 0 ? 0.0 : (double) queueSize / currentQueueCapacity;

        if (utilization >= properties.getScaleUpThreshold() || queuePressure >= properties.getScaleUpThreshold()) {
            int newCore = Math.min(core + properties.getScaleStep(), properties.getMaxCoreSize());
            int newMax = Math.min(max + properties.getScaleStep() * 2, properties.getMaxCoreSize());
            if (newCore != core || newMax != max) {
                threadPool.adjustPoolSize(newCore, newMax);
                lastScaleTimestamp = now;
                lastScaleAction = "SCALE_UP to core=" + newCore + ", max=" + newMax;
                return lastScaleAction;
            }
        }

        if (utilization <= properties.getScaleDownThreshold() && queueSize == 0) {
            int newCore = Math.max(core - properties.getScaleStep(), properties.getMinCoreSize());
            int newMax = Math.max(max - properties.getScaleStep() * 2, newCore);
            if (newCore != core || newMax != max) {
                threadPool.adjustPoolSize(newCore, newMax);
                lastScaleTimestamp = now;
                lastScaleAction = "SCALE_DOWN to core=" + newCore + ", max=" + newMax;
                return lastScaleAction;
            }
        }

        return "NOOP";
    }

    public synchronized String manualScale(int coreSize, int maxSize) {
        DynamicThreadPoolExecutor threadPool = executor();

        int boundedCore = Math.max(properties.getMinCoreSize(), Math.min(coreSize, properties.getMaxCoreSize()));
        int boundedMax = Math.max(boundedCore, Math.min(maxSize, properties.getMaxCoreSize()));

        threadPool.adjustPoolSize(boundedCore, boundedMax);
        lastScaleTimestamp = System.currentTimeMillis();
        lastScaleAction = "MANUAL_SCALE to core=" + boundedCore + ", max=" + boundedMax;
        return lastScaleAction;
    }

    public synchronized PoolUpdateResponse updatePool(String name, PoolUpdateRequest request) {
        if (!poolName.equals(name)) {
            throw new IllegalArgumentException("Unknown pool: " + name);
        }

        DynamicThreadPoolExecutor current = executor();

        int targetCore = normalizeCore(request.getCorePoolSize() != null ? request.getCorePoolSize() : current.getCorePoolSize());
        int targetMax = normalizeMax(request.getMaxPoolSize() != null ? request.getMaxPoolSize() : current.getMaximumPoolSize(), targetCore);
        int targetQueueCapacity = request.getQueueCapacity() != null ? Math.max(1, request.getQueueCapacity()) : queueCapacity;
        long targetKeepAlive = request.getKeepAliveSeconds() != null ? Math.max(1L, request.getKeepAliveSeconds()) : keepAliveSeconds;
        RejectionPolicyType targetPolicy = request.getRejectionPolicy() != null
                ? RejectionPolicyType.from(request.getRejectionPolicy())
                : rejectionPolicy;

        boolean hotSwapped = targetQueueCapacity != queueCapacity;
        String action;

        if (hotSwapped) {
            DynamicThreadPoolExecutor replacement = buildExecutor(targetCore, targetMax, targetQueueCapacity, targetKeepAlive, targetPolicy);
            DynamicThreadPoolExecutor old = executorRef.getAndSet(replacement);
            synchronized (drainingExecutors) {
                drainingExecutors.add(old);
            }
            old.shutdown();
            action = "HOT_SWAP (queue capacity " + queueCapacity + " -> " + targetQueueCapacity + ")";
        } else {
            current.adjustPoolSize(targetCore, targetMax);
            current.setKeepAliveTime(targetKeepAlive, TimeUnit.SECONDS);
            current.setRejectedExecutionHandler(buildRejectedExecutionHandler(targetPolicy));
            action = "RECONFIGURE";
        }

        queueCapacity = targetQueueCapacity;
        keepAliveSeconds = targetKeepAlive;
        rejectionPolicy = targetPolicy;

        lastScaleTimestamp = System.currentTimeMillis();
        lastScaleAction = action + " core=" + targetCore + ", max=" + targetMax + ", policy=" + targetPolicy.name();

        return new PoolUpdateResponse(
                poolName,
                targetCore,
                targetMax,
                queueCapacity,
                keepAliveSeconds,
                rejectionPolicy.name(),
                hotSwapped,
                action
        );
    }

    public synchronized PoolUpdateResponse reloadFromProperties() {
        PoolUpdateRequest request = new PoolUpdateRequest();
        request.setCorePoolSize(properties.getInitialCoreSize());
        request.setMaxPoolSize(properties.getInitialMaxSize());
        request.setQueueCapacity(properties.getQueueCapacity());
        request.setKeepAliveSeconds(properties.getKeepAliveSeconds());
        request.setRejectionPolicy(properties.getRejectionPolicy());
        return updatePool(poolName, request);
    }

    public PoolMetrics currentMetrics() {
        DynamicThreadPoolExecutor current = executor();
        int core = current.getCorePoolSize();
        int active = current.getActiveCount();
        double utilization = core == 0 ? 0.0 : (double) active / core;

        int draining;
        synchronized (drainingExecutors) {
            draining = drainingExecutors.size();
        }

        return new PoolMetrics(
                poolName,
                core,
                current.getMaximumPoolSize(),
                active,
                current.getPoolSize(),
                current.getQueue().size(),
                current.getQueue().remainingCapacity(),
                queueCapacity,
                keepAliveSeconds,
                rejectionPolicy.name(),
                current.getCompletedTaskCount(),
                current.getSubmittedTasks(),
                rejectedEvents.get(),
                degradedEvents.get(),
                draining,
                utilization,
                lastScaleAction
        );
    }

    public DynamicThreadPoolExecutor executor() {
        return executorRef.get();
    }

    public String poolThreadPrefix() {
        return "pool-" + poolName + "-";
    }

    public void recordTaskLatency(long waitMs, long execMs, long totalMs) {
        waitLatencyTimer.record(waitMs, TimeUnit.MILLISECONDS);
        execLatencyTimer.record(execMs, TimeUnit.MILLISECONDS);
        totalLatencyTimer.record(totalMs, TimeUnit.MILLISECONDS);
    }

    public void markDegraded() {
        degradedEvents.incrementAndGet();
        executor().markDegradedTask();
    }

    public void markRejected() {
        rejectedEvents.incrementAndGet();
        executor().markRejectedTask();
    }

    public void resetSubmissionContext() {
        submissionDegradedFlag.set(false);
    }

    public boolean consumeSubmissionDegradedFlag() {
        boolean degraded = Boolean.TRUE.equals(submissionDegradedFlag.get());
        submissionDegradedFlag.remove();
        return degraded;
    }

    @PreDestroy
    public void shutdown() {
        executor().shutdown();
        synchronized (drainingExecutors) {
            for (DynamicThreadPoolExecutor drainingExecutor : drainingExecutors) {
                drainingExecutor.shutdown();
            }
        }
    }

    private DynamicThreadPoolExecutor buildExecutor(int core,
                                                    int max,
                                                    int queueCapacity,
                                                    long keepAliveSeconds,
                                                    RejectionPolicyType policy) {
        return new DynamicThreadPoolExecutor(
                poolName,
                core,
                max,
                keepAliveSeconds,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(queueCapacity),
                buildThreadFactory(),
                buildRejectedExecutionHandler(policy)
        );
    }

    private ThreadFactory buildThreadFactory() {
        AtomicInteger sequence = new AtomicInteger();
        String prefix = poolThreadPrefix();
        return runnable -> {
            Thread thread = new Thread(runnable);
            thread.setName(prefix + sequence.incrementAndGet());
            thread.setDaemon(false);
            return thread;
        };
    }

    private RejectedExecutionHandler buildRejectedExecutionHandler(RejectionPolicyType policy) {
        return (runnable, executor) -> {
            markRejected();

            switch (policy) {
                case ABORT:
                    throw new RejectedExecutionException("Task rejected by ABORT policy");
                case CALLER_RUNS:
                    markDegraded();
                    submissionDegradedFlag.set(true);
                    if (!executor.isShutdown()) {
                        runnable.run();
                        return;
                    }
                    throw new RejectedExecutionException("Executor is shut down");
                case DISCARD_OLDEST:
                    markDegraded();
                    submissionDegradedFlag.set(true);
                    if (!executor.isShutdown()) {
                        executor.getQueue().poll();
                        executor.execute(runnable);
                        return;
                    }
                    throw new RejectedExecutionException("Executor is shut down");
                default:
                    throw new RejectedExecutionException("Unsupported rejection policy");
            }
        };
    }

    private void registerGauges() {
        Gauge.builder("threadpool_active", () -> executor().getActiveCount())
                .tag("pool", poolName)
                .register(meterRegistry);

        Gauge.builder("threadpool_queue_size", () -> executor().getQueue().size())
                .tag("pool", poolName)
                .register(meterRegistry);

        Gauge.builder("threadpool_completed", () -> executor().getCompletedTaskCount())
                .tag("pool", poolName)
                .register(meterRegistry);

        Gauge.builder("threadpool_rejected", rejectedEvents::get)
                .tag("pool", poolName)
                .register(meterRegistry);

        Gauge.builder("threadpool_degraded", degradedEvents::get)
                .tag("pool", poolName)
                .register(meterRegistry);
    }

    private void cleanupDrainingExecutors() {
        synchronized (drainingExecutors) {
            List<DynamicThreadPoolExecutor> terminated = new ArrayList<>();
            for (DynamicThreadPoolExecutor executor : drainingExecutors) {
                if (executor.isTerminated()) {
                    terminated.add(executor);
                }
            }
            drainingExecutors.removeAll(terminated);
        }
    }

    private int normalizeCore(int coreSize) {
        return Math.max(properties.getMinCoreSize(), Math.min(coreSize, properties.getMaxCoreSize()));
    }

    private int normalizeMax(int maxSize, int coreSize) {
        return Math.max(coreSize, Math.min(maxSize, properties.getMaxCoreSize()));
    }
}
