package com.enterprise.quartz.config;

import com.enterprise.quartz.listener.LoggingTriggerListener;
import com.enterprise.quartz.listener.MdcJobListener;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.ListenerManager;
import org.quartz.Scheduler;
import org.quartz.impl.matchers.EverythingMatcher;
import org.springframework.boot.autoconfigure.quartz.SchedulerFactoryBeanCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.quartz.SchedulerFactoryBean;

/**
 * Quartz Infrastructure configuration for Spring Boot.
 * Registers global listeners for MDC and trigger logging, and binds Spring context.
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
public class QuartzConfig {

    private final MdcJobListener mdcJobListener;
    private final LoggingTriggerListener loggingTriggerListener;

    @Bean
    public SchedulerFactoryBeanCustomizer schedulerFactoryBeanCustomizer() {
        return (SchedulerFactoryBean schedulerFactoryBean) -> {
            schedulerFactoryBean.setWaitForJobsToCompleteOnShutdown(true);
            schedulerFactoryBean.setOverwriteExistingJobs(false);
            schedulerFactoryBean.setGlobalJobListeners(mdcJobListener);
            schedulerFactoryBean.setGlobalTriggerListeners(loggingTriggerListener);
            log.info("Configured Quartz SchedulerFactoryBean with global MDC and Logging listeners");
        };
    }
}
