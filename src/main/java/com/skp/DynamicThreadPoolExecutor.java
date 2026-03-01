package com.skp;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public class DynamicThreadPoolExecutor extends ThreadPoolExecutor {

    private final String poolName;
    private final AtomicLong submittedTasks = new AtomicLong();
    private final AtomicLong rejectedTasks = new AtomicLong();
    private final AtomicLong degradedTasks = new AtomicLong();

    public DynamicThreadPoolExecutor(String poolName,
                                     int corePoolSize,
                                     int maximumPoolSize,
                                     long keepAliveTime,
                                     TimeUnit unit,
                                     BlockingQueue<Runnable> workQueue,
                                     ThreadFactory threadFactory,
                                     RejectedExecutionHandler handler) {
        super(corePoolSize, maximumPoolSize, keepAliveTime, unit, workQueue, threadFactory, handler);
        this.poolName = poolName;
    }

    @Override
    public void execute(Runnable command) {
        submittedTasks.incrementAndGet();
        super.execute(command);
    }

    public synchronized void adjustPoolSize(int newCorePoolSize, int newMaximumPoolSize) {
        int adjustedCore = Math.max(1, Math.min(newCorePoolSize, newMaximumPoolSize));
        int adjustedMax = Math.max(adjustedCore, newMaximumPoolSize);

        if (adjustedMax >= getMaximumPoolSize()) {
            setMaximumPoolSize(adjustedMax);
            setCorePoolSize(adjustedCore);
            return;
        }

        setCorePoolSize(adjustedCore);
        setMaximumPoolSize(adjustedMax);
    }

    public String getPoolName() {
        return poolName;
    }

    public long getSubmittedTasks() {
        return submittedTasks.get();
    }

    public long getRejectedTasks() {
        return rejectedTasks.get();
    }

    public long getDegradedTasks() {
        return degradedTasks.get();
    }

    public void markRejectedTask() {
        rejectedTasks.incrementAndGet();
    }

    public void markDegradedTask() {
        degradedTasks.incrementAndGet();
    }
}
