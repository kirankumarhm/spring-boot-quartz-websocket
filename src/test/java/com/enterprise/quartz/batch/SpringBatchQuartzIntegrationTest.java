package com.enterprise.quartz.batch;

import com.enterprise.quartz.batch.jobs.AuditArchivalBatchJobConfig;
import com.enterprise.quartz.batch.jobs.ProductImportBatchJobConfig;
import com.enterprise.quartz.dto.response.JobExecutionRecord;
import com.enterprise.quartz.service.QuartzJobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("h2")
class SpringBatchQuartzIntegrationTest {

    @Autowired
    private JobLauncher jobLauncher;

    @Autowired
    private ListableJobLocator jobLocator;

    @Autowired
    private QuartzJobService quartzJobService;

    @Test
    @DisplayName("Verify Spring Batch jobs are registered in JobLocator")
    void testBatchJobsRegisteredInLocator() {
        var jobNames = jobLocator.getJobNames();
        assertThat(jobNames).contains(
                ProductImportBatchJobConfig.JOB_NAME,
                AuditArchivalBatchJobConfig.JOB_NAME
        );
    }

    @Test
    @DisplayName("Direct execution of chunk-oriented ProductImportBatchJob (25 items in chunks of 10)")
    void testChunkOrientedProductImportJobExecution() throws Exception {
        Job job = jobLocator.getJob(ProductImportBatchJobConfig.JOB_NAME);
        JobParameters params = new JobParametersBuilder()
                .addLong("runTime", System.currentTimeMillis())
                .addString("testRun", "true")
                .toJobParameters();

        JobExecution execution = jobLauncher.run(job, params);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);

        // Verify Step metrics: 25 items read and written across 3 chunks
        var stepExecution = execution.getStepExecutions().iterator().next();
        assertThat(stepExecution.getReadCount()).isEqualTo(25);
        assertThat(stepExecution.getWriteCount()).isEqualTo(25);
        assertThat(stepExecution.getCommitCount()).isGreaterThanOrEqualTo(3);
    }

    @Test
    @DisplayName("Direct execution of Tasklet-oriented AuditArchivalBatchJob")
    void testTaskletAuditArchivalJobExecution() throws Exception {
        Job job = jobLocator.getJob(AuditArchivalBatchJobConfig.JOB_NAME);
        JobParameters params = new JobParametersBuilder()
                .addLong("runTime", System.currentTimeMillis())
                .addLong("retentionDays", 45L)
                .toJobParameters();

        JobExecution execution = jobLauncher.run(job, params);
        assertThat(execution.getStatus()).isEqualTo(BatchStatus.COMPLETED);
    }

    @Test
    @DisplayName("End-to-End: Trigger SpringBatch Quartz bridge job through QuartzJobService")
    void testQuartzToSpringBatchBridgeExecution() throws Exception {
        // Trigger seeded Quartz job that bridges to Spring Batch
        quartzJobService.triggerJobNow("batch-processing", "product-import-job");

        // Wait briefly for Quartz worker thread to complete execution
        Thread.sleep(1200);

        List<JobExecutionRecord> history = quartzJobService.getExecutionHistory("batch-processing", "product-import-job", 5);
        assertThat(history).isNotEmpty();

        JobExecutionRecord latest = history.getFirst();
        assertThat(latest.getStatus()).isEqualTo("SUCCESS");
        assertThat(latest.getDurationMs()).isNotNull();
    }
}
