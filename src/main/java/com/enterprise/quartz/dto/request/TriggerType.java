package com.enterprise.quartz.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Supported trigger scheduling strategies.
 */
@Schema(description = "Type of trigger strategy to apply")
public enum TriggerType {
    @Schema(description = "Cron expression based schedule (e.g., 0 0/5 * * * ?)")
    CRON,

    @Schema(description = "Fixed interval based repetition")
    SIMPLE_INTERVAL,

    @Schema(description = "One-time immediate or future execution")
    ONE_TIME
}
