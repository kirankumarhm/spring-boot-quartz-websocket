package com.enterprise.quartz.exception;

import com.enterprise.quartz.filter.CorrelationIdFilter;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.net.URI;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Global REST exception advisor compliant with RFC 7807 Problem Details.
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(JobNotFoundException.class)
    public ProblemDetail handleJobNotFound(JobNotFoundException ex, HttpServletRequest request) {
        log.warn("Job not found: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
        problem.setTitle("Job Not Found");
        problem.setType(URI.create("https://api.quartz.enterprise.com/errors/not-found"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(JobConflictException.class)
    public ProblemDetail handleJobConflict(JobConflictException ex, HttpServletRequest request) {
        log.warn("Job conflict: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
        problem.setTitle("Job Already Exists");
        problem.setType(URI.create("https://api.quartz.enterprise.com/errors/conflict"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(InvalidCronExpressionException.class)
    public ProblemDetail handleInvalidCron(InvalidCronExpressionException ex, HttpServletRequest request) {
        log.warn("Invalid cron expression: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
        problem.setTitle("Invalid Cron Expression");
        problem.setType(URI.create("https://api.quartz.enterprise.com/errors/invalid-cron"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error on request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, "Validation failed for one or more fields");
        problem.setTitle("Bad Request Validation");
        problem.setType(URI.create("https://api.quartz.enterprise.com/errors/validation-failed"));

        Map<String, String> errors = new HashMap<>();
        for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
            errors.put(fieldError.getField(), fieldError.getDefaultMessage());
        }
        problem.setProperty("invalidFields", errors);
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ProblemDetail handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied on request: {}", ex.getMessage());
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.FORBIDDEN, "Access denied: insufficient privileges");
        problem.setTitle("Forbidden");
        problem.setType(URI.create("https://api.quartz.enterprise.com/errors/forbidden"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(SchedulerOperationException.class)
    public ProblemDetail handleSchedulerOperation(SchedulerOperationException ex, HttpServletRequest request) {
        log.error("Underlying Quartz scheduler failure: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, ex.getMessage());
        problem.setTitle("Scheduler Operation Failure");
        problem.setType(URI.create("https://api.quartz.enterprise.com/errors/scheduler-error"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGeneralException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled server exception: {}", ex.getMessage(), ex);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, "An internal error occurred");
        problem.setTitle("Internal Server Error");
        problem.setType(URI.create("https://api.quartz.enterprise.com/errors/internal"));
        enrichProblemDetail(problem, request);
        return problem;
    }

    private void enrichProblemDetail(ProblemDetail problem, HttpServletRequest request) {
        String correlationId = MDC.get(CorrelationIdFilter.CORRELATION_ID_MDC_KEY);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = "N/A";
        }
        problem.setInstance(URI.create(request.getRequestURI()));
        problem.setProperty("timestamp", Instant.now());
        problem.setProperty("correlationId", correlationId);
    }
}
