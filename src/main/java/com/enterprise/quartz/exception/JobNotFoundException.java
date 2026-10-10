package com.enterprise.quartz.exception;

/**
 * Thrown when a requested job does not exist in the scheduler.
 */
public class JobNotFoundException extends RuntimeException {
    public JobNotFoundException(String jobGroup, String jobName) {
        super(String.format("Job with group '%s' and name '%s' was not found", jobGroup, jobName));
    }
}
