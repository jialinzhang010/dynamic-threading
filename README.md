# Dynamic Threading

一个面向 Java 后端校招面试的线程池项目：实现“任务异步化 + 有界队列 + 拒绝策略 + 可观测 + 动态调参 + 热切换 + Nacos 动态配置”。

## 重点
- 有界队列与背压：使用 `ArrayBlockingQueue(capacity)`，`maxPoolSize` 才有实际意义，并能稳定触发拒绝策略。
- 拒绝策略与降级：支持 `ABORT` / `CALLER_RUNS` / `DISCARD_OLDEST`，统计 `rejectCount` 与 `degradedCount`。
- 可观测性：接入 Micrometer，暴露线程池与延迟指标，并可通过 Actuator 对接 Prometheus/Grafana。
- 动态调参与热切换：`PUT /api/pools/{name}` 动态修改 `core/max/keepAlive/strategy`；队列容量变化时执行“新池热切换 + 旧池 drain”。
- Nacos 集成：支持 Nacos 配置中心动态下发 `threadpool.*` 参数，运行时自动刷新到线程池。

## 技术栈
- Java 11+
- Spring Boot 2.7
- Spring Cloud Alibaba Nacos (Config + Discovery)
- Spring Boot Actuator
- Micrometer + Prometheus Registry
- Maven + JUnit5 + MockMvc

## 核心接口
1. 提交异步任务
```bash
curl -X POST "http://localhost:8080/api/threads?durationMs=300&message=io-task"
```

2. 查询任务状态
```bash
curl "http://localhost:8080/api/tasks/{taskId}"
```

3. 查看线程池指标
```bash
curl "http://localhost:8080/api/pool/metrics"
```

4. 动态更新线程池
```bash
curl -X PUT "http://localhost:8080/api/pools/default" \
  -H "Content-Type: application/json" \
  -d '{
    "corePoolSize": 4,
    "maxPoolSize": 8,
    "keepAliveSeconds": 60,
    "queueCapacity": 200,
    "rejectionPolicy": "CALLER_RUNS"
  }'
```

5. 手动触发一次自动扩缩容评估
```bash
curl -X POST "http://localhost:8080/api/pool/monitor"
```

## 监控与指标
- Actuator metrics：`/actuator/metrics`
- Prometheus scrape：`/actuator/prometheus`

主要指标（示例）：
- `threadpool_active`
- `threadpool_queue_size`
- `threadpool_completed`
- `threadpool_rejected`
- `threadpool_degraded`
- `threadpool_task_latency{phase=wait|exec|total}`

## Nacos 使用
1. 启动 Nacos Server（默认 `127.0.0.1:8848`）
2. 设置环境变量后启动应用
```bash
export NACOS_SERVER_ADDR=127.0.0.1:8848
export NACOS_CONFIG_ENABLED=true
export NACOS_DISCOVERY_ENABLED=true
mvn spring-boot:run
```

3. 在 Nacos 配置中心创建 Data ID：`dynamic-threading.yaml`（Group: `DEFAULT_GROUP`）
```yaml
threadpool:
  initial-core-size: 6
  initial-max-size: 12
  queue-capacity: 300
  keep-alive-seconds: 45
  rejection-policy: CALLER_RUNS
  scale-up-threshold: 0.75
  scale-down-threshold: 0.30
```

发布配置后，应用会监听 `threadpool.*` 变更并自动刷新线程池参数。

## 本地运行
```bash
mvn clean test
mvn spring-boot:run
```

默认地址：`http://localhost:8080`

## 简历
- 设计并实现基于 Spring Boot 的动态线程池服务，将同步请求改造为“异步提交 + 结果查询”模型，支持任务状态追踪与延迟分析。  
- 落地有界队列和多种拒绝策略（Abort/CallerRuns/DiscardOldest），实现拒绝与降级指标统计，支持高并发场景下背压与服务保护。  
- 接入 Micrometer + Actuator 暴露线程池与延迟指标，并整合 Nacos 配置中心实现线程池参数动态刷新及运行时热切换。
