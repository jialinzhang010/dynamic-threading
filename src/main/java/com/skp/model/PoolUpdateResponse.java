package com.skp.model;

public class PoolUpdateResponse {

    private final String poolName;
    private final int corePoolSize;
    private final int maxPoolSize;
    private final int queueCapacity;
    private final long keepAliveSeconds;
    private final String rejectionPolicy;
    private final boolean hotSwapped;
    private final String action;

    public PoolUpdateResponse(String poolName,
                              int corePoolSize,
                              int maxPoolSize,
                              int queueCapacity,
                              long keepAliveSeconds,
                              String rejectionPolicy,
                              boolean hotSwapped,
                              String action) {
        this.poolName = poolName;
        this.corePoolSize = corePoolSize;
        this.maxPoolSize = maxPoolSize;
        this.queueCapacity = queueCapacity;
        this.keepAliveSeconds = keepAliveSeconds;
        this.rejectionPolicy = rejectionPolicy;
        this.hotSwapped = hotSwapped;
        this.action = action;
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

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public long getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public String getRejectionPolicy() {
        return rejectionPolicy;
    }

    public boolean isHotSwapped() {
        return hotSwapped;
    }

    public String getAction() {
        return action;
    }
}
