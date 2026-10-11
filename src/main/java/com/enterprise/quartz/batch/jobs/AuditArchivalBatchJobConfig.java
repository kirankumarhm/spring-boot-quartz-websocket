package com.enterprise.quartz.batch.jobs;

import com.enterprise.quartz.batch.listener.BatchExecutionLoggingListener;
import lombok.extern.slf4j.Slf4j;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.Step;
import org.springframework.batch.core.job.builder.JobBuilder;
import org.springframework.batch.core.repository.JobRepository;
import org.springframework.batch.core.step.builder.StepBuilder;
import org.springframework.batch.core.step.tasklet.Tasklet;
import org.springframework.batch.repeat.RepeatStatus;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * <h3>Production Tasklet-Oriented Batch Job: Audit Log Archival</h3>
 * <p>
 * Demonstrates Spring Batch 5 Tasklet processing for operational, single-step
 * batch routines (such as archiving compliance logs, file rotations, or cleanup routines).
 * </p>
 */
@Slf4j
@Configuration
public class AuditArchivalBatchJobConfig {

    public static final String JOB_NAME = "audit-archival-batch-job";
    public static final String STEP_NAME = "audit-archival-step";

    @Bean
    public Tasklet auditArchivalTasklet() {
        return (contribution, chunkContext) -> {
            var stepContext = chunkContext.getStepContext();
            var jobParams = stepContext.getJobParameters();
            long retentionDays = 30;
            if (jobParams.containsKey("retentionDays")) {
                retentionDays = Long.parseLong(jobParams.get("retentionDays").toString());
            }

            log.info("--> [AuditArchivalTasklet] Commencing archival of audit logs older than {} days", retentionDays);
            log.info("    Scanning partitions, compressing records into cold storage archive...");
            
            // Simulating partition archival
            long archivedCount = 1420;
            contribution.incrementWriteCount((int) archivedCount);
            
            log.info("<-- [AuditArchivalTasklet] Successfully archived {} compliance records into cold storage", archivedCount);
            return RepeatStatus.FINISHED;
        };
    }

    @Bean
    public Step auditArchivalStep(
            JobRepository jobRepository,
            PlatformTransactionManager transactionManager,
            Tasklet auditArchivalTasklet,
            BatchExecutionLoggingListener listener) {

        return new StepBuilder(STEP_NAME, jobRepository)
                .tasklet(auditArchivalTasklet, transactionManager)
                .listener(listener)
                .build();
    }

    @Bean(name = JOB_NAME)
    public Job auditArchivalBatchJob(
            JobRepository jobRepository,
            Step auditArchivalStep,
            BatchExecutionLoggingListener listener) {

        return new JobBuilder(JOB_NAME, jobRepository)
                .start(auditArchivalStep)
                .listener(listener)
                .build();
    }
}
