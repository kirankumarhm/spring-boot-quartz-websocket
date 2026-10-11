package com.enterprise.quartz.config;

import com.enterprise.quartz.batch.bridge.SpringBatchQuartzJobLauncher;
import com.enterprise.quartz.batch.jobs.AuditArchivalBatchJobConfig;
import com.enterprise.quartz.batch.jobs.ProductImportBatchJobConfig;
import com.enterprise.quartz.dto.request.CreateJobRequest;
import com.enterprise.quartz.dto.request.TriggerType;
import com.enterprise.quartz.service.QuartzJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Automatically seeds production-grade sample Quartz and Spring Batch jobs
 * on application startup if they are not already present in the clustered JobStore.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SampleJobInitializer implements ApplicationRunner {

    private final QuartzJobService quartzJobService;
    private final Scheduler scheduler;

    @Override
    public void run(ApplicationArguments args) {
        log.info("Checking sample Quartz and Spring Batch jobs in clustered JobStore...");

        // 1. Standard Quartz Job: email-digest
        seedJobIfNotExists(
                "notifications",
                "email-digest",
                "com.enterprise.quartz.sample.SampleEmailJob",
                "Production email digest notification routine",
                TriggerType.CRON,
                "0 0/15 * * * ?",
                null,
                Map.of("subject", "Weekly Report", "recipient", "lead@enterprise.com")
        );

        // 2. Standard Quartz Job: db-vacuum-cleanup
        seedJobIfNotExists(
                "maintenance",
                "db-vacuum-cleanup",
                "com.enterprise.quartz.sample.SampleDataCleanupJob",
                "Purges expired audit logs and vacuum indexes",
                TriggerType.SIMPLE_INTERVAL,
                null,
                300,
                Map.of("retentionDays", 30)
        );

        // 3. Spring Batch Chunk-Oriented ETL Job: product-import-batch-job
        seedJobIfNotExists(
                "batch-processing",
                "product-import-job",
                SpringBatchQuartzJobLauncher.class.getName(),
                "Chunk-oriented ETL catalog import pipeline (Reader -> Processor -> Writer)",
                TriggerType.CRON,
                "0 0/30 * * * ?",
                null,
                Map.of("batchJobName", ProductImportBatchJobConfig.JOB_NAME, "sourceSystem", "ERP_WAREHOUSE")
        );

        // 4. Spring Batch Operational Tasklet Job: audit-archival-batch-job
        seedJobIfNotExists(
                "maintenance",
                "audit-archival-job",
                SpringBatchQuartzJobLauncher.class.getName(),
                "Spring Batch tasklet compliance log cold storage archival",
                TriggerType.CRON,
                "0 0 1 * * ?",
                null,
                Map.of("batchJobName", AuditArchivalBatchJobConfig.JOB_NAME, "retentionDays", 60)
        );
    }

    private void seedJobIfNotExists(
            String group,
            String name,
            String jobClass,
            String description,
            TriggerType triggerType,
            String cronExpression,
            Integer intervalInSeconds,
            Map<String, Object> jobData) {

        try {
            if (!scheduler.checkExists(JobKey.jobKey(name, group))) {
                log.info("Seeding initial job [{}.{}] into clustered Quartz store", group, name);
                CreateJobRequest request = CreateJobRequest.builder()
                        .jobGroup(group)
                        .jobName(name)
                        .jobClass(jobClass)
                        .description(description)
                        .triggerType(triggerType)
                        .cronExpression(cronExpression)
                        .intervalInSeconds(intervalInSeconds)
                        .jobData(jobData)
                        .durable(true)
                        .build();
                quartzJobService.createJob(request);
            } else {
                log.debug("Job [{}.{}] already present in JobStore, skipping seeding", group, name);
            }
        } catch (Exception e) {
            log.warn("Could not seed initial job [{}.{}]: {}", group, name, e.getMessage());
        }
    }
}
