package com.enterprise.quartz;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.ConfigurationPropertiesScan;

/**
 * Enterprise Production-Grade Quartz Scheduler Service.
 * <p>
 * Configured with Spring Boot 3.4+ / Java 21 LTS, Quartz Clustered JDBC JobStore,
 * SpringDoc OpenAPI 3, Spring Security RBAC, and MDC tracing.
 */
@SpringBootApplication
@ConfigurationPropertiesScan
public class QuartzApplication {

    public static void main(String[] args) {
        SpringApplication.run(QuartzApplication.class, args);
    }
}
