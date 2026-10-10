package com.enterprise.quartz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.Map;

/**
 * Real-time metadata for in-flight jobs currently being executed by worker threads.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CurrentlyExecutingJobResponse {
    private String jobName;
    private String jobGroup;
    private String triggerName;
    private String triggerGroup;
    private Instant fireTime;
    private Instant scheduledFireTime;
    private long runTimeMs;
    private boolean recovering;
    private int refireCount;
    private Map<String, Object> jobData;
}
