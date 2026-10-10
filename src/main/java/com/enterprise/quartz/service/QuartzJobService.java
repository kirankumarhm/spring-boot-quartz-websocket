package com.enterprise.quartz.service;

import com.enterprise.quartz.dto.request.CreateJobRequest;
import com.enterprise.quartz.dto.request.RescheduleJobRequest;
import com.enterprise.quartz.dto.response.CurrentlyExecutingJobResponse;
import com.enterprise.quartz.dto.response.JobExecutionRecord;
import com.enterprise.quartz.dto.response.JobResponse;
import com.enterprise.quartz.dto.response.SchedulerMetadataResponse;

import java.util.List;
import java.util.Map;

/**
 * High-level orchestration service for managing Quartz Schedulers and Job lifecycle.
 */
public interface QuartzJobService {

    /**
     * Lists all registered jobs across all groups.
     */
    List<JobResponse> getAllJobs();

    /**
     * Retrieves job metadata and associated triggers by group and name.
     */
    JobResponse getJob(String group, String name);

    /**
     * Creates and registers a new Job along with its initial trigger.
     */
    JobResponse createJob(CreateJobRequest request);

    /**
     * Reschedules an existing job's primary trigger.
     */
    JobResponse rescheduleJob(String group, String name, RescheduleJobRequest request);

    /**
     * Pauses all triggers associated with a specific job.
     */
    void pauseJob(String group, String name);

    /**
     * Resumes all triggers associated with a specific job.
     */
    void resumeJob(String group, String name);

    /**
     * Immediately triggers an out-of-schedule execution of a job.
     */
    void triggerJobNow(String group, String name);

    /**
     * Immediately triggers an out-of-schedule execution of a job with optional custom JobDataMap params.
     */
    void triggerJobNow(String group, String name, Map<String, Object> jobData);

    /**
     * Deletes a job and un-schedules all its triggers.
     */
    void deleteJob(String group, String name);

    /**
     * Globally pauses the scheduler.
     */
    void pauseAll();

    /**
     * Globally resumes the scheduler.
     */
    void resumeAll();

    /**
     * Retrieves cluster and scheduler engine runtime metadata.
     */
    SchedulerMetadataResponse getSchedulerMetadata();

    /**
     * Retrieves real-time information on all currently executing jobs across worker threads.
     */
    List<CurrentlyExecutingJobResponse> getCurrentlyExecutingJobs();

    /**
     * Retrieves execution history audit records.
     */
    List<JobExecutionRecord> getExecutionHistory(String group, String name, int limit);

    /**
     * Clears in-memory execution audit records.
     */
    void clearExecutionHistory();
}
