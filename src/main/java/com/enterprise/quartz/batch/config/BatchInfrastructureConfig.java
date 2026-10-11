package com.enterprise.quartz.batch.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

/**
 * Core infrastructure configuration for Spring Batch 5.x.
 * In Spring Boot 3.4+, BatchAutoConfiguration automatically registers all declared
 * Spring Batch Job beans into the central JobRegistry via JobRegistrySmartInitializingSingleton,
 * enabling dynamic job lookup by name via ListableJobLocator.
 */
@Slf4j
@Configuration
public class BatchInfrastructureConfig {
    // Spring Boot 3.4+ auto-configures JobRepository, JobLauncher, JobExplorer, and JobRegistry automatically.
}
