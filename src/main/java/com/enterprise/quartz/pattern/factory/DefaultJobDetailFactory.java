package com.enterprise.quartz.pattern.factory;

import com.enterprise.quartz.dto.request.CreateJobRequest;
import com.enterprise.quartz.exception.SchedulerOperationException;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobBuilder;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobKey;
import org.springframework.stereotype.Component;

/**
 * Default implementation of JobDetailFactory utilizing Quartz JobBuilder.
 */
@Slf4j
@Component
public class DefaultJobDetailFactory implements JobDetailFactory {

    @Override
    @SuppressWarnings("unchecked")
    public JobDetail createJobDetail(CreateJobRequest request) {
        log.debug("Creating JobDetail for jobName={}, jobGroup={}, jobClass={}",
                request.getJobName(), request.getJobGroup(), request.getJobClass());

        Class<? extends Job> jobClass;
        try {
            Class<?> clazz = Class.forName(request.getJobClass());
            if (!Job.class.isAssignableFrom(clazz)) {
                throw new IllegalArgumentException("Class " + request.getJobClass() + " does not implement org.quartz.Job");
            }
            jobClass = (Class<? extends Job>) clazz;
        } catch (ClassNotFoundException e) {
            log.error("Job class not found: {}", request.getJobClass());
            throw new SchedulerOperationException("Job class not found: " + request.getJobClass(), e);
        } catch (IllegalArgumentException e) {
            log.error("Invalid job class: {}", e.getMessage());
            throw new SchedulerOperationException(e.getMessage(), e);
        }

        JobDataMap jobDataMap = new JobDataMap();
        if (request.getJobData() != null) {
            jobDataMap.putAll(request.getJobData());
        }

        JobKey jobKey = JobKey.jobKey(request.getJobName(), request.getJobGroup());

        return JobBuilder.newJob(jobClass)
                .withIdentity(jobKey)
                .withDescription(request.getDescription())
                .usingJobData(jobDataMap)
                .storeDurably(request.isDurable())
                .requestRecovery(false)
                .build();
    }
}
