package com.enterprise.quartz.batch.listener;

import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.batch.core.BatchStatus;
import org.springframework.batch.core.ExitStatus;
import org.springframework.batch.core.JobExecution;
import org.springframework.batch.core.JobExecutionListener;
import org.springframework.batch.core.StepExecution;
import org.springframework.batch.core.StepExecutionListener;
import org.springframework.stereotype.Component;

/**
 * Enterprise observability listener for Spring Batch jobs and steps.
 * Propagates MDC trace context across execution boundaries and logs execution metrics.
 */
@Slf4j
@Component
public class BatchExecutionLoggingListener implements JobExecutionListener, StepExecutionListener {

    private static final String MDC_BATCH_JOB = "batchJob";
    private static final String MDC_STEP_NAME = "stepName";

    @Override
    public void beforeJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        MDC.put(MDC_BATCH_JOB, jobName);
        log.info(">>> [Spring Batch] Job [{}] starting with execution ID [{}] and parameters: {}",
                jobName, jobExecution.getId(), jobExecution.getJobParameters().getParameters());
    }

    @Override
    public void afterJob(JobExecution jobExecution) {
        String jobName = jobExecution.getJobInstance().getJobName();
        long durationMs = 0;
        if (jobExecution.getStartTime() != null && jobExecution.getEndTime() != null) {
            durationMs = java.time.Duration.between(jobExecution.getStartTime(), jobExecution.getEndTime()).toMillis();
        }

        BatchStatus status = jobExecution.getStatus();
        if (status == BatchStatus.COMPLETED) {
            log.info("<<< [Spring Batch] Job [{}] COMPLETED successfully in {}ms (exitCode={})",
                    jobName, durationMs, jobExecution.getExitStatus().getExitCode());
        } else {
            log.error("<<< [Spring Batch] Job [{}] finished with status [{}] in {}ms (exitDescription={})",
                    jobName, status, durationMs, jobExecution.getExitStatus().getExitDescription());
        }
        MDC.remove(MDC_BATCH_JOB);
    }

    @Override
    public void beforeStep(StepExecution stepExecution) {
        MDC.put(MDC_STEP_NAME, stepExecution.getStepName());
        log.debug("--> [Spring Batch Step] Step [{}] started", stepExecution.getStepName());
    }

    @Override
    public ExitStatus afterStep(StepExecution stepExecution) {
        log.info("<-- [Spring Batch Step] Step [{}] finished with status [{}]. Read={}, Written={}, Committed={}, Filtered={}, Skips={}",
                stepExecution.getStepName(),
                stepExecution.getStatus(),
                stepExecution.getReadCount(),
                stepExecution.getWriteCount(),
                stepExecution.getCommitCount(),
                stepExecution.getFilterCount(),
                stepExecution.getSkipCount());
        MDC.remove(MDC_STEP_NAME);
        return stepExecution.getExitStatus();
    }
}
