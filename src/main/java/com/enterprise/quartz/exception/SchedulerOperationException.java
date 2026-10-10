package com.enterprise.quartz.exception;

/**
 * Thrown when an underlying Quartz Scheduler operation fails unexpectedly.
 */
public class SchedulerOperationException extends RuntimeException {
    public SchedulerOperationException(String message, Throwable cause) {
        super(message, cause);
    }
}
