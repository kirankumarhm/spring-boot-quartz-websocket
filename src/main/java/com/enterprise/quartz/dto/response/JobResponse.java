package com.enterprise.quartz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Detailed representation of a registered Quartz Job with high-level operational status.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of a registered Quartz Job")
public class JobResponse {

    @Schema(description = "Job identifier name", example = "email-digest-job")
    private String jobName;

    @Schema(description = "Job group name", example = "notifications")
    private String jobGroup;

    @Schema(description = "Fully qualified class implementing org.quartz.Job", example = "com.enterprise.quartz.sample.SampleEmailJob")
    private String jobClass;

    @Schema(description = "Job description", example = "Generates and sends weekly email digest")
    private String description;

    @Schema(description = "High-level operational job status (IN_PROGRESS, SCHEDULED, PAUSED, COMPLETED, BLOCKED, ERROR, IDLE)", example = "SCHEDULED")
    private String jobStatus;

    @Schema(description = "Outcome of the most recent execution (SUCCESS, FAILED)", example = "SUCCESS")
    private String lastExecutionStatus;

    @Schema(description = "Duration of the most recent execution in milliseconds", example = "527")
    private Long lastExecutionDurationMs;

    @Schema(description = "Timestamp of the most recent execution")
    private Instant lastExecutionTime;

    @Schema(description = "Whether the job is currently executing", example = "false")
    private boolean currentlyExecuting;

    @Schema(description = "Whether the job persists after triggers finish", example = "true")
    private boolean durable;

    @Schema(description = "Whether the job prevents concurrent executions (@DisallowConcurrentExecution)", example = "true")
    private boolean concurrentExecutionDisallowed;

    @Schema(description = "Whether the job requests recovery after scheduler crash", example = "false")
    private boolean requestsRecovery;

    @Schema(description = "Custom parameters stored in JobDataMap")
    private Map<String, Object> jobData;

    @Schema(description = "Associated triggers defining when this job runs")
    private List<TriggerResponse> triggers;
}
