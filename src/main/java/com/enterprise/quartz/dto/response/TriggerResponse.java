package com.enterprise.quartz.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Response model containing trigger state and execution schedules.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "Details of a Quartz Trigger")
public class TriggerResponse {

    @Schema(description = "Trigger identifier name", example = "email-digest-job-trigger")
    private String triggerName;

    @Schema(description = "Trigger group name", example = "notifications")
    private String triggerGroup;

    @Schema(description = "Trigger runtime state", example = "NORMAL, PAUSED, BLOCKED, COMPLETE, ERROR")
    private String state;

    @Schema(description = "Trigger type", example = "CRON")
    private String type;

    @Schema(description = "Cron expression if applicable", example = "0 0/15 * * * ?")
    private String cronExpression;

    @Schema(description = "Previous fire time in ISO-8601 UTC")
    private Instant previousFireTime;

    @Schema(description = "Next scheduled fire time in ISO-8601 UTC")
    private Instant nextFireTime;

    @Schema(description = "Trigger validity start time")
    private Instant startTime;

    @Schema(description = "Trigger validity end time")
    private Instant endTime;

    @Schema(description = "Priority of trigger execution", example = "5")
    private int priority;
}
