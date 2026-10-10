package com.enterprise.quartz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Request payload for rescheduling an existing Quartz trigger.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Request model for rescheduling an existing job trigger")
public class RescheduleJobRequest {

    @NotNull(message = "Trigger type must be specified")
    @Schema(description = "New trigger type strategy", example = "CRON", requiredMode = Schema.RequiredMode.REQUIRED)
    private TriggerType triggerType;

    @Schema(description = "New cron expression (if triggerType is CRON)", example = "0 0/30 * * * ?")
    private String cronExpression;

    @Schema(description = "New interval in seconds (if triggerType is SIMPLE_INTERVAL)", example = "600")
    private Integer intervalInSeconds;

    @Schema(description = "Repeat count (-1 for indefinite)", example = "-1")
    @Builder.Default
    private Integer repeatCount = -1;

    @Schema(description = "Optional start timestamp in ISO-8601 UTC")
    private Instant startAt;

    @Schema(description = "Optional end timestamp in ISO-8601 UTC")
    private Instant endAt;
}
