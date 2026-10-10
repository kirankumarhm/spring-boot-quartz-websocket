package com.enterprise.quartz.sample;

import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.springframework.stereotype.Component;

/**
 * Production sample job for asynchronous email dispatch.
 * Annotations prevent concurrent runs across cluster nodes if a prior execution is still active.
 */
@Slf4j
@Component
@DisallowConcurrentExecution
public class SampleEmailJob implements Job {

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        JobDataMap data = context.getMergedJobDataMap();
        String recipient = data.getString("recipient");
        if (recipient == null) {
            recipient = "admin@enterprise.com";
        }

        String subject = data.getString("subject");
        if (subject == null) {
            subject = "System Notification";
        }

        log.info("Processing SampleEmailJob: Sending notification to [{}] with subject [{}]", recipient, subject);

        try {
            // Simulate work (e.g., SMTP dispatch)
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JobExecutionException("Email dispatch interrupted", e);
        }

        log.info("Successfully completed SampleEmailJob for [{}]", recipient);
    }
}
