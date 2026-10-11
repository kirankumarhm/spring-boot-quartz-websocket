package com.enterprise.quartz.batch.bridge;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.Job;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobParameters;
import org.springframework.batch.core.JobParametersBuilder;
import org.springframework.batch.core.configuration.ListableJobLocator;
import org.springframework.batch.core.launch.JobLauncher;
import org.springframework.batch.core.launch.NoSuchJobException;
import org.springframework.scheduling.quartz.QuartzJobBean;
import org.springframework.stereotype.Component;

import java.util.Date;
import java.util.UUID;

/**
 * <h3>Bridge Pattern Implementation: Quartz Scheduler &rarr; Spring Batch</h3>
 * <p>
 * Decouples the scheduling abstraction (Quartz) from the heavy batch data processing
 * abstraction (Spring Batch). When Quartz triggers fire, this bridge dynamically discovers
 * the targeted Spring Batch {@link Job} via {@link ListableJobLocator}, constructs immutable
 * {@link JobParameters} (including correlation IDs and execution timestamps), and launches
 * the batch pipeline via {@link JobLauncher}.
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SpringBatchQuartzJobLauncher extends QuartzJobBean {

    public static final String KEY_BATCH_JOB_NAME = "batchJobName";

    private final JobLauncher jobLauncher;
    private final ListableJobLocator jobLocator;

    @Override
    protected void executeInternal(JobExecutionContext context) throws JobExecutionException {
        JobDataMap dataMap = context.getMergedJobDataMap();
        String batchJobName = dataMap.getString(KEY_BATCH_JOB_NAME);

        // Fall back to Quartz Job name if batchJobName is not explicitly mapped
        if (batchJobName == null || batchJobName.isBlank()) {
            batchJobName = context.getJobDetail().getKey().getName();
        }

        String quartzJobKey = context.getJobDetail().getKey().toString();
        String triggerKey = context.getTrigger() != null ? context.getTrigger().getKey().toString() : "MANUAL";
        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        log.info("Bridge executing: Launching Spring Batch job [{}] triggered by Quartz job [{}] via trigger [{}]",
                batchJobName, quartzJobKey, triggerKey);

        Job batchJob;
        try {
            batchJob = jobLocator.getJob(batchJobName);
        } catch (NoSuchJobException e) {
            log.error("Spring Batch job [{}] not found in JobRegistry. Configured beans: {}",
                    batchJobName, jobLocator.getJobNames(), e);
            throw new JobExecutionException("Spring Batch job '" + batchJobName + "' not registered in JobLocator", e, false);
        }

        // Build typed Spring Batch JobParameters
        JobParametersBuilder paramsBuilder = new JobParametersBuilder();
        paramsBuilder.addLong("runTime", System.currentTimeMillis());
        paramsBuilder.addString("correlationId", correlationId);
        paramsBuilder.addString("quartzJobKey", quartzJobKey);
        paramsBuilder.addString("quartzTriggerKey", triggerKey);

        // Propagate custom parameters from Quartz JobDataMap into Spring Batch
        for (String key : dataMap.keySet()) {
            if (KEY_BATCH_JOB_NAME.equalsIgnoreCase(key)) continue;

            Object val = dataMap.get(key);
            if (val instanceof String s) {
                paramsBuilder.addString(key, s);
            } else if (val instanceof Long l) {
                paramsBuilder.addLong(key, l);
            } else if (val instanceof Integer i) {
                paramsBuilder.addLong(key, i.longValue());
            } else if (val instanceof Double d) {
                paramsBuilder.addDouble(key, d);
            } else if (val instanceof Date dt) {
                paramsBuilder.addDate(key, dt);
            } else if (val != null) {
                paramsBuilder.addString(key, val.toString());
            }
        }

        JobParameters jobParameters = paramsBuilder.toJobParameters();

        try {
            JobExecution execution = jobLauncher.run(batchJob, jobParameters);
            BatchStatus status = execution.getStatus();

            log.info("Spring Batch job [{}] finished with status: [{}], exit status: [{}]",
                    batchJobName, status, execution.getExitStatus().getExitCode());

            if (status == BatchStatus.FAILED) {
                String errorDesc = execution.getExitStatus().getExitDescription();
                log.error("Spring Batch job [{}] FAILED: {}", batchJobName, errorDesc);
                throw new JobExecutionException("Spring Batch job '" + batchJobName + "' execution failed: " + errorDesc, false);
            }

            // Expose the batch execution ID in Quartz context for telemetry
            context.setResult("Spring Batch Execution ID: " + execution.getId() + " [COMPLETED]");
        } catch (JobExecutionException e) {
            throw e;
        } catch (Exception e) {
            log.error("Unexpected error executing Spring Batch job [{}]: {}", batchJobName, e.getMessage(), e);
            throw new JobExecutionException("Failed to launch Spring Batch job '" + batchJobName + "'", e, false);
        }
    }
}
