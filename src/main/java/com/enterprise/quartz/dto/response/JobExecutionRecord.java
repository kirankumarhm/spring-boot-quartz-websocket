package com.enterprise.quartz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Historical audit record of a completed or failed job execution.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobExecutionRecord {
    private String id;
    private String jobName;
    private String jobGroup;
    private String triggerName;
    private String triggerGroup;
    private String status; // SUCCESS, FAILED, VETOED
    private Instant startTime;
    private Instant endTime;
    private Long durationMs;
    private String correlationId;
    private String errorMessage;
    private int refireCount;
}
