package com.enterprise.quartz.exception;

/**
 * Thrown when attempting to register a job that already exists.
 */
public class JobConflictException extends RuntimeException {
    public JobConflictException(String jobGroup, String jobName) {
        super(String.format("Job with group '%s' and name '%s' already exists", jobGroup, jobName));
    }
}
