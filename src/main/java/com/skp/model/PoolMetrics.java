package com.skp.model;

public class PoolMetrics {

    private final String poolName;
    private final int corePoolSize;
    private final int maxPoolSize;
    private final int activeCount;
    private final int currentPoolSize;
    private final int queueSize;
    private final int queueRemainingCapacity;
    private final int queueCapacity;
    private final long keepAliveSeconds;
    private final String rejectionPolicy;
    private final long completedTaskCount;
    private final long submittedTaskCount;
    private final long rejectedTaskCount;
    private final long degradedTaskCount;
    private final int drainingPoolCount;
    private final double utilization;
    private final String lastScaleAction;

    public PoolMetrics(String poolName,
                       int corePoolSize,
                       int maxPoolSize,
                       int activeCount,
                       int currentPoolSize,
                       int queueSize,
                       int queueRemainingCapacity,
                       int queueCapacity,
                       long keepAliveSeconds,
                       String rejectionPolicy,
                       long completedTaskCount,
                       long submittedTaskCount,
                       long rejectedTaskCount,
                       long degradedTaskCount,
                       int drainingPoolCount,
                       double utilization,
                       String lastScaleAction) {
        this.poolName = poolName;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.activeCount = activeCount;
        this.currentPoolSize = currentPoolSize;
        this.queueSize = queueSize;
        this.queueRemainingCapacity = queueRemainingCapacity;
        this.queueCapacity = queueCapacity;
        this.keepAliveSeconds = keepAliveSeconds;
        this.rejectionPolicy = rejectionPolicy;
        this.completedTaskCount = completedTaskCount;
        this.submittedTaskCount = submittedTaskCount;
        this.rejectedTaskCount = rejectedTaskCount;
        this.degradedTaskCount = degradedTaskCount;
        this.drainingPoolCount = drainingPoolCount;
        this.utilization = utilization;
        this.lastScaleAction = lastScaleAction;
    }

    public String getPoolName() {
        return poolName;
    }

    public int getCorePoolSize() {
        return corePoolSize;
    }

    public int getMaxPoolSize() {
        return maxPoolSize;
    }

    public int getActiveCount() {
        return activeCount;
    }

    public int getCurrentPoolSize() {
        return currentPoolSize;
    }

    public int getQueueSize() {
        return queueSize;
    }

    public int getQueueRemainingCapacity() {
        return queueRemainingCapacity;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public long getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public String getRejectionPolicy() {
        return rejectionPolicy;
    }

    public long getCompletedTaskCount() {
        return completedTaskCount;
    }

    public long getSubmittedTaskCount() {
        return submittedTaskCount;
    }

    public long getRejectedTaskCount() {
        return rejectedTaskCount;
    }

    public long getDegradedTaskCount() {
        return degradedTaskCount;
    }

    public int getDrainingPoolCount() {
        return drainingPoolCount;
    }

    public double getUtilization() {
        return utilization;
    }

    public String getLastScaleAction() {
        return lastScaleAction;
    }
}
