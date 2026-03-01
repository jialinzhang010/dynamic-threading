package com.skp;

import com.skp.config.ThreadPoolProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableScheduling;

@SpringBootApplication
@EnableScheduling
@EnableConfigurationProperties(ThreadPoolProperties.class)
public class DynamicThreadPoolApplication {

    public static void main(String[] args) {
        SpringApplication.run(DynamicThreadPoolApplication.class, args);
    }
}
