package com.skp;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.skp.model.PoolUpdateRequest;
import com.skp.service.DynamicThreadPoolManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
        "threadpool.pool-name=default",
        "threadpool.min-core-size=1",
        "threadpool.max-core-size=4",
        "threadpool.initial-core-size=1",
        "threadpool.initial-max-size=1",
        "threadpool.queue-capacity=1",
        "threadpool.keep-alive-seconds=30",
        "threadpool.rejection-policy=ABORT",
        "threadpool.scale-step=1",
        "threadpool.scale-up-threshold=0.50",
        "threadpool.scale-down-threshold=0.10",
        "threadpool.cooldown-ms=0",
        "threadpool.monitor-interval-ms=60000",
        "management.endpoints.web.exposure.include=health,info,prometheus,metrics"
})
@AutoConfigureMockMvc
class DynamicThreadPoolApplicationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private DynamicThreadPoolManager threadPoolManager;

    @BeforeEach
    void resetPool() {
        PoolUpdateRequest request = new PoolUpdateRequest();
        request.setCorePoolSize(1);
        request.setMaxPoolSize(1);
        request.setQueueCapacity(1);
        request.setKeepAliveSeconds(30L);
        request.setRejectionPolicy("ABORT");
        threadPoolManager.updatePool("default", request);
    }

    @Test
    void shouldSubmitTaskAndQueryResult() throws Exception {
        MvcResult submitResult = mockMvc.perform(post("/api/threads")
                        .param("durationMs", "80")
                        .param("message", "resume-demo"))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.accepted").value(true))
                .andReturn();

        JsonNode submitJson = objectMapper.readTree(submitResult.getResponse().getContentAsString());
        String taskId = submitJson.get("taskId").asText();

        TaskStatusHolder statusHolder = new TaskStatusHolder();
        for (int i = 0; i < 8; i++) {
            MvcResult queryResult = mockMvc.perform(get("/api/tasks/{id}", taskId))
                    .andExpect(status().isOk())
                    .andReturn();
            JsonNode queryJson = objectMapper.readTree(queryResult.getResponse().getContentAsString());
            statusHolder.status = queryJson.get("status").asText();
            if ("SUCCESS".equals(statusHolder.status)) {
                assertThat(queryJson.get("threadName").asText()).startsWith("pool-default-");
                assertThat(queryJson.get("totalLatencyMs").asLong()).isGreaterThan(0);
                return;
            }
            Thread.sleep(30);
        }

        assertThat(statusHolder.status).isEqualTo("SUCCESS");
    }

    @Test
    void shouldReturn429WhenAbortPolicyRejects() throws Exception {
        mockMvc.perform(post("/api/threads").param("durationMs", "400"))
                .andExpect(status().isAccepted());
        mockMvc.perform(post("/api/threads").param("durationMs", "400"))
                .andExpect(status().isAccepted());

        mockMvc.perform(post("/api/threads").param("durationMs", "400"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.accepted").value(false))
                .andExpect(jsonPath("$.status").value("REJECTED"));
    }

    @Test
    void shouldHotSwapWhenQueueCapacityChanges() throws Exception {
        String body = "{\"corePoolSize\":2,\"maxPoolSize\":3,\"queueCapacity\":5,\"keepAliveSeconds\":45,\"rejectionPolicy\":\"CALLER_RUNS\"}";

        mockMvc.perform(put("/api/pools/default")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hotSwapped").value(true))
                .andExpect(jsonPath("$.queueCapacity").value(5))
                .andExpect(jsonPath("$.rejectionPolicy").value("CALLER_RUNS"));

        mockMvc.perform(get("/api/pool/metrics"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.queueCapacity").value(5))
                .andExpect(jsonPath("$.rejectionPolicy").value("CALLER_RUNS"));
    }

    @Test
    void shouldExposeMicrometerMetrics() throws Exception {
        mockMvc.perform(get("/actuator/metrics/threadpool_active"))
                .andExpect(status().isOk());
    }

    private static class TaskStatusHolder {
        private String status;
    }
}
