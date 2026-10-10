package com.enterprise.quartz.exception;

/**
 * Thrown when an invalid cron expression is supplied.
 */
public class InvalidCronExpressionException extends RuntimeException {
    public InvalidCronExpressionException(String cronExpression, String details) {
        super(String.format("Invalid Quartz cron expression '%s': %s", cronExpression, details));
    }
}
