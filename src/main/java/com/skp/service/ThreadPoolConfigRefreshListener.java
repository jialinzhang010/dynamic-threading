package com.skp.service;

import org.springframework.cloud.context.environment.EnvironmentChangeEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Component
public class ThreadPoolConfigRefreshListener {

    private final DynamicThreadPoolManager poolManager;

    public ThreadPoolConfigRefreshListener(DynamicThreadPoolManager poolManager) {
        this.poolManager = poolManager;
    }

    @EventListener(EnvironmentChangeEvent.class)
    public void onEnvironmentChange(EnvironmentChangeEvent event) {
        boolean threadPoolChanged = event.getKeys().stream().anyMatch(key -> key.startsWith("threadpool."));
        if (!threadPoolChanged) {
            return;
        }
        poolManager.reloadFromProperties();
    }
}
