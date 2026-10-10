package com.enterprise.quartz.sample;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

/**
 * Production sample job for purging expired data or cache entries.
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class SampleDataCleanupJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        int retentionDays = data.containsKey("retentionDays") ? data.getInt("retentionDays") : 30;

        log.info("Processing SampleDataCleanupJob: Purging database records older than {} days", retentionDays);

        try {
            // Simulate database purge operation
            Thread.sleep(700);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobExecutionException("Data cleanup interrupted", e);
        }

        log.info("Successfully completed SampleDataCleanupJob for retention period {} days", retentionDays);
    }
}
