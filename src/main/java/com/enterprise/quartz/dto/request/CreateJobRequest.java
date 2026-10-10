package com.enterprise.quartz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Request payload for creating and scheduling a new Quartz job.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request model for registering and scheduling a job")
public class CreateJobRequest {

    @NotBlank(message = "Job name cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Job name must only contain alphanumeric characters, dashes, or underscores")
    @Schema(description = "Unique identifier name for the job", example = "email-digest-job", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobName;

    @NotBlank(message = "Job group cannot be blank")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "Job group must only contain alphanumeric characters, dashes, or underscores")
    @Schema(description = "Logical group name for the job", example = "notifications", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobGroup;

    @NotBlank(message = "Job class name cannot be blank")
    @Schema(description = "Fully qualified class name or registered job alias", example = "com.enterprise.quartz.sample.SampleEmailJob", requiredMode = Schema.RequiredMode.REQUIRED)
    private String jobClass;

    @Schema(description = "Human-readable description of what this job performs", example = "Generates and sends weekly email digest")
    private String description;

    @NotNull(message = "Trigger type must be specified")
    @Schema(description = "Type of trigger strategy", example = "CRON", requiredMode = Schema.RequiredMode.REQUIRED)
    private TriggerType triggerType;

    @Schema(description = "Cron expression (mandatory if triggerType is CRON)", example = "0 0/15 * * * ?")
    private String cronExpression;

    @Schema(description = "Interval in seconds (mandatory if triggerType is SIMPLE_INTERVAL)", example = "300")
    private Integer intervalInSeconds;

    @Schema(description = "Total repeat count for interval trigger (-1 for indefinite)", example = "-1")
    @Builder.Default
    private Integer repeatCount = -1;

    @Schema(description = "Optional start timestamp in ISO-8601 UTC")
    private Instant startAt;

    @Schema(description = "Optional end timestamp in ISO-8601 UTC")
    private Instant endAt;

    @Schema(description = "Key-value data parameters passed to the executing job")
    private Map<String, Object> jobData;

    @Schema(description = "Whether job remains stored even after its triggers have finished firing", example = "true")
    @Builder.Default
    private boolean durable = true;
}
