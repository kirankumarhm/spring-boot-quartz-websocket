package com.enterprise.quartz.listener;

import com.enterprise.quartz.dto.event.JobEvent;
import com.enterprise.quartz.service.JobEventBroadcaster;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;
import org.quartz.JobListener;
import org.slf4j.MDC;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Observer listener that binds Quartz Job execution context to SLF4J MDC
 * for distributed tracing, logs execution duration audits, and broadcasts
 * live execution telemetry over WebSockets.
 */
@Slf4j
@Component
public class MdcJobListener implements JobListener {

    public static final String LISTENER_NAME = "GlobalMdcJobListener";
    private static final String START_TIME_KEY = "executionStartTime";

    private final JobEventBroadcaster broadcaster;
    private final com.enterprise.quartz.service.JobExecutionHistoryService historyService;

    public MdcJobListener(@Lazy JobEventBroadcaster broadcaster,
                          com.enterprise.quartz.service.JobExecutionHistoryService historyService) {
        this.broadcaster = broadcaster;
        this.historyService = historyService;
    }

    @Override
    public String getName() {
        return LISTENER_NAME;
    }

    @Override
    public void jobToBeExecuted(JobExecutionContext context) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
        String triggerName = context.getTrigger().getKey().getName();
        String triggerGroup = context.getTrigger().getKey().getGroup();

        String correlationId = MDC.get("correlationId");
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
            MDC.put("correlationId", correlationId);
        }

        MDC.put("jobKey", jobGroup + "." + jobName);
        MDC.put("triggerKey", triggerGroup + "." + triggerName);

        context.put(START_TIME_KEY, System.currentTimeMillis());

        log.info("Starting execution of Job [{}.{}] via Trigger [{}.{}]", jobGroup, jobName, triggerGroup, triggerName);

        // Broadcast live start event
        broadcaster.broadcast(JobEvent.builder()
                .eventType(JobEvent.EventType.JOB_STARTED)
                .jobName(jobName)
                .jobGroup(jobGroup)
                .triggerName(triggerName)
                .triggerGroup(triggerGroup)
                .correlationId(correlationId)
                .message("Job execution started")
                .build());
    }

    @Override
    public void jobExecutionVetoed(JobExecutionContext context) {
        String jobKey = context.getJobDetail().getKey().toString();
        log.warn("Execution of Job [{}] was vetoed by a trigger listener", jobKey);
        clearMdc();
    }

    @Override
    public void jobWasExecuted(JobExecutionContext context, JobExecutionException jobException) {
        String jobName = context.getJobDetail().getKey().getName();
        String jobGroup = context.getJobDetail().getKey().getGroup();
        String triggerName = context.getTrigger().getKey().getName();
        String triggerGroup = context.getTrigger().getKey().getGroup();
        String correlationId = MDC.get("correlationId");

        Long startTime = (Long) context.get(START_TIME_KEY);
        long duration = startTime != null ? (System.currentTimeMillis() - startTime) : 0;

        if (jobException != null) {
            log.error("Job [{}.{}] failed with exception after {}ms: {}",
                    jobGroup, jobName, duration, jobException.getMessage(), jobException);
            broadcaster.broadcast(JobEvent.builder()
                    .eventType(JobEvent.EventType.JOB_FAILED)
                    .jobName(jobName)
                    .jobGroup(jobGroup)
                    .triggerName(triggerName)
                    .triggerGroup(triggerGroup)
                    .durationMs(duration)
                    .correlationId(correlationId)
                    .message("Failed: " + jobException.getMessage())
                    .build());
            historyService.recordExecution(com.enterprise.quartz.dto.response.JobExecutionRecord.builder()
                    .id(UUID.randomUUID().toString())
                    .jobName(jobName)
                    .jobGroup(jobGroup)
                    .triggerName(triggerName)
                    .triggerGroup(triggerGroup)
                    .status("FAILED")
                    .startTime(startTime != null ? java.time.Instant.ofEpochMilli(startTime) : java.time.Instant.now())
                    .endTime(java.time.Instant.now())
                    .durationMs(duration)
                    .correlationId(correlationId)
                    .errorMessage(jobException.getMessage())
                    .refireCount(context.getRefireCount())
                    .build());
        } else {
            log.info("Job [{}.{}] completed successfully in {}ms (refireCount={})",
                    jobGroup, jobName, duration, context.getRefireCount());
            broadcaster.broadcast(JobEvent.builder()
                    .eventType(JobEvent.EventType.JOB_COMPLETED)
                    .jobName(jobName)
                    .jobGroup(jobGroup)
                    .triggerName(triggerName)
                    .triggerGroup(triggerGroup)
                    .durationMs(duration)
                    .correlationId(correlationId)
                    .message("Completed successfully in " + duration + "ms")
                    .build());

            historyService.recordExecution(com.enterprise.quartz.dto.response.JobExecutionRecord.builder()
                    .id(UUID.randomUUID().toString())
                    .jobName(jobName)
                    .jobGroup(jobGroup)
                    .triggerName(triggerName)
                    .triggerGroup(triggerGroup)
                    .status("SUCCESS")
                    .startTime(startTime != null ? java.time.Instant.ofEpochMilli(startTime) : java.time.Instant.now())
                    .endTime(java.time.Instant.now())
                    .durationMs(duration)
                    .correlationId(correlationId)
                    .errorMessage(null)
                    .refireCount(context.getRefireCount())
                    .build());
        }

        clearMdc();
    }

    private void clearMdc() {
        MDC.remove("jobKey");
        MDC.remove("triggerKey");
    }
}
