package com.skp.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "threadpool")
public class ThreadPoolProperties {

    private String poolName = "default";
    private int minCoreSize = 4;
    private int maxCoreSize = 32;
    private int initialCoreSize = 8;
    private int initialMaxSize = 16;
    private int queueCapacity = 200;
    private long keepAliveSeconds = 60;
    private String rejectionPolicy = "ABORT";

    private double scaleUpThreshold = 0.80;
    private double scaleDownThreshold = 0.25;
    private int scaleStep = 2;
    private long cooldownMs = 3000;
    private long monitorIntervalMs = 1000;

    public String getPoolName() {
        return poolName;
    }

    public void setPoolName(String poolName) {
        this.poolName = poolName;
    }

    public int getMinCoreSize() {
        return minCoreSize;
    }

    public void setMinCoreSize(int minCoreSize) {
        this.minCoreSize = minCoreSize;
    }

    public int getMaxCoreSize() {
        return maxCoreSize;
    }

    public void setMaxCoreSize(int maxCoreSize) {
        this.maxCoreSize = maxCoreSize;
    }

    public int getInitialCoreSize() {
        return initialCoreSize;
    }

    public void setInitialCoreSize(int initialCoreSize) {
        this.initialCoreSize = initialCoreSize;
    }

    public int getInitialMaxSize() {
        return initialMaxSize;
    }

    public void setInitialMaxSize(int initialMaxSize) {
        this.initialMaxSize = initialMaxSize;
    }

    public int getQueueCapacity() {
        return queueCapacity;
    }

    public void setQueueCapacity(int queueCapacity) {
        this.queueCapacity = queueCapacity;
    }

    public long getKeepAliveSeconds() {
        return keepAliveSeconds;
    }

    public void setKeepAliveSeconds(long keepAliveSeconds) {
        this.keepAliveSeconds = keepAliveSeconds;
    }

    public String getRejectionPolicy() {
        return rejectionPolicy;
    }

    public void setRejectionPolicy(String rejectionPolicy) {
        this.rejectionPolicy = rejectionPolicy;
    }

    public double getScaleUpThreshold() {
        return scaleUpThreshold;
    }

    public void setScaleUpThreshold(double scaleUpThreshold) {
        this.scaleUpThreshold = scaleUpThreshold;
    }

    public double getScaleDownThreshold() {
        return scaleDownThreshold;
    }

    public void setScaleDownThreshold(double scaleDownThreshold) {
        this.scaleDownThreshold = scaleDownThreshold;
    }

    public int getScaleStep() {
        return scaleStep;
    }

    public void setScaleStep(int scaleStep) {
        this.scaleStep = scaleStep;
    }

    public long getCooldownMs() {
        return cooldownMs;
    }

    public void setCooldownMs(long cooldownMs) {
        this.cooldownMs = cooldownMs;
    }

    public long getMonitorIntervalMs() {
        return monitorIntervalMs;
    }

    public void setMonitorIntervalMs(long monitorIntervalMs) {
        this.monitorIntervalMs = monitorIntervalMs;
    }
}
