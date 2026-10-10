package com.enterprise.quartz.service.impl;

import com.enterprise.quartz.dto.request.CreateJobRequest;
import com.enterprise.quartz.dto.request.RescheduleJobRequest;
import com.enterprise.quartz.dto.response.JobResponse;
import com.enterprise.quartz.dto.response.TriggerResponse;
import com.enterprise.quartz.exception.JobConflictException;
import com.enterprise.quartz.exception.JobNotFoundException;
import com.enterprise.quartz.exception.SchedulerOperationException;
import com.enterprise.quartz.pattern.factory.JobDetailFactory;
import com.enterprise.quartz.pattern.strategy.TriggerStrategy;
import com.enterprise.quartz.pattern.strategy.TriggerStrategyFactory;
import com.enterprise.quartz.dto.event.JobEvent;
import com.enterprise.quartz.service.JobEventBroadcaster;
import com.enterprise.quartz.service.QuartzJobService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;
import org.quartz.JobKey;
import org.quartz.Scheduler;
import org.quartz.SchedulerException;
import org.quartz.Trigger;
import org.quartz.TriggerKey;
import org.quartz.impl.matchers.GroupMatcher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

import com.enterprise.quartz.dto.response.CurrentlyExecutingJobResponse;
import com.enterprise.quartz.dto.response.JobExecutionRecord;
import com.enterprise.quartz.dto.response.SchedulerMetadataResponse;
import com.enterprise.quartz.service.JobExecutionHistoryService;
import java.util.Map;

/**
 * Production implementation of QuartzJobService with real-time WebSocket telemetry.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class QuartzJobServiceImpl implements QuartzJobService {

    private final Scheduler scheduler;
    private final JobDetailFactory jobDetailFactory;
    private final TriggerStrategyFactory triggerStrategyFactory;
    private final JobEventBroadcaster broadcaster;
    private final JobExecutionHistoryService historyService;

    @Override
    @Transactional(readOnly = true)
    public List<JobResponse> getAllJobs() {
        log.debug("Fetching all registered Quartz jobs");
        List<JobResponse> jobs = new ArrayList<>();
        try {
            Set<JobKey> jobKeys = scheduler.getJobKeys(GroupMatcher.anyJobGroup());
            for (JobKey jobKey : jobKeys) {
                jobs.add(getJob(jobKey.getGroup(), jobKey.getName()));
            }
        } catch (SchedulerException e) {
            log.error("Failed to query scheduler for all jobs: {}", e.getMessage(), e);
            throw new SchedulerOperationException("Could not retrieve registered jobs", e);
        }
        return jobs;
    }

    @Override
    @Transactional(readOnly = true)
    public JobResponse getJob(String group, String name) {
        JobKey jobKey = JobKey.jobKey(name, group);
        try {
            if (!scheduler.checkExists(jobKey)) {
                throw new JobNotFoundException(group, name);
            }
            JobDetail jobDetail = scheduler.getJobDetail(jobKey);
            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);

            boolean isExecuting = false;
            for (JobExecutionContext context : scheduler.getCurrentlyExecutingJobs()) {
                if (context.getJobDetail().getKey().equals(jobKey)) {
                    isExecuting = true;
                    break;
                }
            }

            return mapToJobResponse(jobDetail, triggers, isExecuting);
        } catch (SchedulerException e) {
            log.error("Error retrieving job detail for [{}.{}]: {}", group, name, e.getMessage(), e);
            throw new SchedulerOperationException("Failed to read job metadata", e);
        }
    }

    @Override
    @Transactional
    public JobResponse createJob(CreateJobRequest request) {
        JobKey jobKey = JobKey.jobKey(request.getJobName(), request.getJobGroup());
        log.info("Registering new job: [{}]", jobKey);

        try {
            if (scheduler.checkExists(jobKey)) {
                throw new JobConflictException(request.getJobGroup(), request.getJobName());
            }

            JobDetail jobDetail = jobDetailFactory.createJobDetail(request);
            TriggerStrategy strategy = triggerStrategyFactory.getStrategy(request.getTriggerType());
            Trigger trigger = strategy.buildTrigger(request, jobKey);

            scheduler.scheduleJob(jobDetail, trigger);
            log.info("Successfully scheduled job [{}] with trigger [{}]", jobKey, trigger.getKey());

            broadcaster.broadcast(JobEvent.builder()
                    .eventType(JobEvent.EventType.JOB_SCHEDULED)
                    .jobName(request.getJobName())
                    .jobGroup(request.getJobGroup())
                    .triggerName(trigger.getKey().getName())
                    .triggerGroup(trigger.getKey().getGroup())
                    .message("Job scheduled with " + request.getTriggerType())
                    .build());

            return getJob(request.getJobGroup(), request.getJobName());
        } catch (SchedulerException e) {
            log.error("Failed to schedule job [{}]: {}", jobKey, e.getMessage(), e);
            throw new SchedulerOperationException("Failed to schedule job", e);
        }
    }

    @Override
    @Transactional
    public JobResponse rescheduleJob(String group, String name, RescheduleJobRequest request) {
        JobKey jobKey = JobKey.jobKey(name, group);
        log.info("Rescheduling job: [{}]", jobKey);

        try {
            if (!scheduler.checkExists(jobKey)) {
                throw new JobNotFoundException(group, name);
            }

            List<? extends Trigger> triggers = scheduler.getTriggersOfJob(jobKey);
            if (triggers.isEmpty()) {
                throw new SchedulerOperationException("Job has no existing trigger to reschedule. Delete and recreate.", null);
            }

            Trigger existingTrigger = null;
            for (Trigger t : triggers) {
                if (!t.getKey().getName().startsWith("MT_") && !Scheduler.DEFAULT_GROUP.equals(t.getKey().getGroup())) {
                    existingTrigger = t;
                    break;
                }
            }
            if (existingTrigger == null) {
                existingTrigger = triggers.getFirst();
            }
            TriggerStrategy strategy = triggerStrategyFactory.getStrategy(request.getTriggerType());
            Trigger newTrigger = strategy.buildRescheduleTrigger(request, existingTrigger.getKey());

            Date nextFire = scheduler.rescheduleJob(existingTrigger.getKey(), newTrigger);
            log.info("Rescheduled trigger [{}] for job [{}], next fire time: {}", existingTrigger.getKey(), jobKey, nextFire);

            broadcaster.broadcast(JobEvent.builder()
                    .eventType(JobEvent.EventType.JOB_RESCHEDULED)
                    .jobName(name)
                    .jobGroup(group)
                    .triggerName(existingTrigger.getKey().getName())
                    .triggerGroup(existingTrigger.getKey().getGroup())
                    .message("Job trigger rescheduled")
                    .build());

            return getJob(group, name);
        } catch (SchedulerException e) {
            log.error("Failed to reschedule job [{}]: {}", jobKey, e.getMessage(), e);
            throw new SchedulerOperationException("Failed to reschedule trigger", e);
        }
    }

    @Override
    @Transactional
    public void pauseJob(String group, String name) {
        JobKey jobKey = JobKey.jobKey(name, group);
        log.info("Pausing job: [{}]", jobKey);
        try {
            if (!scheduler.checkExists(jobKey)) {
                throw new JobNotFoundException(group, name);
            }
            scheduler.pauseJob(jobKey);
            broadcaster.broadcast(JobEvent.builder()
                    .eventType(JobEvent.EventType.JOB_PAUSED)
                    .jobName(name)
                    .jobGroup(group)
                    .message("Job paused")
                    .build());
        } catch (SchedulerException e) {
            log.error("Failed to pause job [{}]: {}", jobKey, e.getMessage(), e);
            throw new SchedulerOperationException("Failed to pause job", e);
        }
    }

    @Override
    @Transactional
    public void resumeJob(String group, String name) {
        JobKey jobKey = JobKey.jobKey(name, group);
        log.info("Resuming job: [{}]", jobKey);
        try {
            if (!scheduler.checkExists(jobKey)) {
                throw new JobNotFoundException(group, name);
            }
            scheduler.resumeJob(jobKey);
            broadcaster.broadcast(JobEvent.builder()
                    .eventType(JobEvent.EventType.JOB_RESUMED)
                    .jobName(name)
                    .jobGroup(group)
                    .message("Job resumed")
                    .build());
        } catch (SchedulerException e) {
            log.error("Failed to resume job [{}]: {}", jobKey, e.getMessage(), e);
            throw new SchedulerOperationException("Failed to resume job", e);
        }
    }

    @Override
    @Transactional
    public void triggerJobNow(String group, String name) {
        triggerJobNow(group, name, null);
    }

    @Override
    @Transactional
    public void triggerJobNow(String group, String name, Map<String, Object> jobData) {
        JobKey jobKey = JobKey.jobKey(name, group);
        log.info("Triggering immediate execution for job [{}] with data: {}", jobKey, jobData);
        try {
            if (!scheduler.checkExists(jobKey)) {
                throw new JobNotFoundException(group, name);
            }
            if (jobData != null && !jobData.isEmpty()) {
                org.quartz.JobDataMap map = new org.quartz.JobDataMap();
                map.putAll(jobData);
                scheduler.triggerJob(jobKey, map);
            } else {
                scheduler.triggerJob(jobKey);
            }
        } catch (SchedulerException e) {
            log.error("Failed to trigger job [{}]: {}", jobKey, e.getMessage(), e);
            throw new SchedulerOperationException("Failed to fire job immediately", e);
        }
    }

    @Override
    @Transactional
    public void deleteJob(String group, String name) {
        JobKey jobKey = JobKey.jobKey(name, group);
        log.info("Deleting job: [{}]", jobKey);
        try {
            if (!scheduler.checkExists(jobKey)) {
                throw new JobNotFoundException(group, name);
            }
            scheduler.deleteJob(jobKey);
            broadcaster.broadcast(JobEvent.builder()
                    .eventType(JobEvent.EventType.JOB_DELETED)
                    .jobName(name)
                    .jobGroup(group)
                    .message("Job deleted from scheduler")
                    .build());
        } catch (SchedulerException e) {
            log.error("Failed to delete job [{}]: {}", jobKey, e.getMessage(), e);
            throw new SchedulerOperationException("Failed to delete job", e);
        }
    }

    @Override
    @Transactional
    public void pauseAll() {
        log.warn("Globally pausing scheduler");
        try {
            scheduler.pauseAll();
        } catch (SchedulerException e) {
            log.error("Failed to globally pause scheduler: {}", e.getMessage(), e);
            throw new SchedulerOperationException("Failed to globally pause scheduler", e);
        }
    }

    @Override
    @Transactional
    public void resumeAll() {
        log.info("Globally resuming scheduler");
        try {
            scheduler.resumeAll();
        } catch (SchedulerException e) {
            log.error("Failed to globally resume scheduler: {}", e.getMessage(), e);
            throw new SchedulerOperationException("Failed to globally resume scheduler", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public SchedulerMetadataResponse getSchedulerMetadata() {
        try {
            org.quartz.SchedulerMetaData metaData = scheduler.getMetaData();
            String status = "RUNNING";
            if (scheduler.isShutdown()) {
                status = "SHUTDOWN";
            } else if (scheduler.isInStandbyMode()) {
                status = "STANDBY";
            }

            return SchedulerMetadataResponse.builder()
                    .schedulerName(metaData.getSchedulerName())
                    .schedulerInstanceId(metaData.getSchedulerInstanceId())
                    .status(status)
                    .isClustered(metaData.isJobStoreClustered())
                    .jobStoreClass(metaData.getJobStoreClass() != null ? metaData.getJobStoreClass().getSimpleName() : "N/A")
                    .threadPoolClass(metaData.getThreadPoolClass() != null ? metaData.getThreadPoolClass().getSimpleName() : "N/A")
                    .threadPoolSize(metaData.getThreadPoolSize())
                    .runningSince(metaData.getRunningSince() != null ? metaData.getRunningSince().toInstant() : null)
                    .numberOfJobsExecuted(metaData.getNumberOfJobsExecuted())
                    .currentlyExecutingCount(scheduler.getCurrentlyExecutingJobs().size())
                    .version(metaData.getVersion())
                    .inStandbyMode(scheduler.isInStandbyMode())
                    .isShutdown(scheduler.isShutdown())
                    .build();
        } catch (SchedulerException e) {
            log.error("Failed to query scheduler metadata: {}", e.getMessage(), e);
            throw new SchedulerOperationException("Failed to read scheduler metadata", e);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<CurrentlyExecutingJobResponse> getCurrentlyExecutingJobs() {
        List<CurrentlyExecutingJobResponse> list = new ArrayList<>();
        try {
            List<JobExecutionContext> contexts = scheduler.getCurrentlyExecutingJobs();
            for (JobExecutionContext ctx : contexts) {
                list.add(CurrentlyExecutingJobResponse.builder()
                        .jobName(ctx.getJobDetail().getKey().getName())
                        .jobGroup(ctx.getJobDetail().getKey().getGroup())
                        .triggerName(ctx.getTrigger() != null ? ctx.getTrigger().getKey().getName() : "N/A")
                        .triggerGroup(ctx.getTrigger() != null ? ctx.getTrigger().getKey().getGroup() : "N/A")
                        .fireTime(ctx.getFireTime() != null ? ctx.getFireTime().toInstant() : null)
                        .scheduledFireTime(ctx.getScheduledFireTime() != null ? ctx.getScheduledFireTime().toInstant() : null)
                        .runTimeMs(ctx.getJobRunTime())
                        .recovering(ctx.isRecovering())
                        .refireCount(ctx.getRefireCount())
                        .jobData(ctx.getMergedJobDataMap() != null ? ctx.getMergedJobDataMap().getWrappedMap() : null)
                        .build());
            }
        } catch (SchedulerException e) {
            log.error("Failed to query currently executing jobs: {}", e.getMessage(), e);
            throw new SchedulerOperationException("Could not retrieve in-flight jobs", e);
        }
        return list;
    }

    @Override
    public List<JobExecutionRecord> getExecutionHistory(String group, String name, int limit) {
        return historyService.getHistory(group, name, limit);
    }

    @Override
    public void clearExecutionHistory() {
        historyService.clearHistory();
    }

    private JobResponse mapToJobResponse(JobDetail jobDetail, List<? extends Trigger> triggers, boolean isExecuting) {
        List<TriggerResponse> triggerResponses = new ArrayList<>();
        if (triggers != null) {
            triggers.stream()
                    .sorted((t1, t2) -> {
                        boolean t1IsManual = t1.getKey().getName().startsWith("MT_") || Scheduler.DEFAULT_GROUP.equals(t1.getKey().getGroup());
                        boolean t2IsManual = t2.getKey().getName().startsWith("MT_") || Scheduler.DEFAULT_GROUP.equals(t2.getKey().getGroup());
                        return Boolean.compare(t1IsManual, t2IsManual);
                    })
                    .forEach(trigger -> triggerResponses.add(mapToTriggerResponse(trigger)));
        }

        // Compute high-level human-readable operational Job Status
        String computedStatus = "IDLE";
        if (isExecuting) {
            computedStatus = "IN_PROGRESS";
        } else if (triggerResponses.stream().anyMatch(t -> "PAUSED".equalsIgnoreCase(t.getState()))) {
            computedStatus = "PAUSED";
        } else if (triggerResponses.stream().anyMatch(t -> "BLOCKED".equalsIgnoreCase(t.getState()))) {
            computedStatus = "BLOCKED";
        } else if (triggerResponses.stream().anyMatch(t -> "ERROR".equalsIgnoreCase(t.getState()))) {
            computedStatus = "ERROR";
        } else if (triggerResponses.stream().anyMatch(t -> "NORMAL".equalsIgnoreCase(t.getState()) || "WAITING".equalsIgnoreCase(t.getState()))) {
            computedStatus = "SCHEDULED";
        } else if (triggerResponses.stream().anyMatch(t -> "COMPLETE".equalsIgnoreCase(t.getState()))) {
            computedStatus = "COMPLETED";
        }

        // Query latest execution outcome for this job from history
        List<JobExecutionRecord> history = historyService.getHistory(jobDetail.getKey().getGroup(), jobDetail.getKey().getName(), 1);
        String lastStatus = null;
        Long lastDuration = null;
        java.time.Instant lastTime = null;
        if (history != null && !history.isEmpty()) {
            JobExecutionRecord rec = history.getFirst();
            lastStatus = rec.getStatus();
            lastDuration = rec.getDurationMs();
            lastTime = rec.getEndTime();
        }

        return JobResponse.builder()
                .jobName(jobDetail.getKey().getName())
                .jobGroup(jobDetail.getKey().getGroup())
                .jobClass(jobDetail.getJobClass().getName())
                .description(jobDetail.getDescription())
                .jobStatus(computedStatus)
                .lastExecutionStatus(lastStatus)
                .lastExecutionDurationMs(lastDuration)
                .lastExecutionTime(lastTime)
                .currentlyExecuting(isExecuting)
                .durable(jobDetail.isDurable())
                .concurrentExecutionDisallowed(jobDetail.isConcurrentExectionDisallowed())
                .requestsRecovery(jobDetail.requestsRecovery())
                .jobData(jobDetail.getJobDataMap().getWrappedMap())
                .triggers(triggerResponses)
                .build();
    }

    private TriggerResponse mapToTriggerResponse(Trigger trigger) {
        Trigger.TriggerState state = Trigger.TriggerState.NONE;
        try {
            state = scheduler.getTriggerState(trigger.getKey());
        } catch (SchedulerException e) {
            log.warn("Could not determine state for trigger [{}]: {}", trigger.getKey(), e.getMessage());
        }

        String cronExpr = null;
        if (trigger instanceof CronTrigger cronTrigger) {
            cronExpr = cronTrigger.getCronExpression();
        }

        return TriggerResponse.builder()
                .triggerName(trigger.getKey().getName())
                .triggerGroup(trigger.getKey().getGroup())
                .state(state.name())
                .type(trigger.getClass().getSimpleName())
                .cronExpression(cronExpr)
                .previousFireTime(trigger.getPreviousFireTime() != null ? trigger.getPreviousFireTime().toInstant() : null)
                .nextFireTime(trigger.getNextFireTime() != null ? trigger.getNextFireTime().toInstant() : null)
                .startTime(trigger.getStartTime() != null ? trigger.getStartTime().toInstant() : null)
                .endTime(trigger.getEndTime() != null ? trigger.getEndTime().toInstant() : null)
                .priority(trigger.getPriority())
                .build();
    }
}
