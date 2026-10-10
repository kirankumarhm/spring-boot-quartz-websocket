package com.enterprise.quartz.dto.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Real-time event model broadcast over WebSocket to active dashboards.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobEvent {

    public enum EventType {
        JOB_STARTED,
        JOB_COMPLETED,
        JOB_FAILED,
        JOB_PAUSED,
        JOB_RESUMED,
        JOB_SCHEDULED,
        JOB_RESCHEDULED,
        JOB_DELETED,
        TRIGGER_MISFIRED
    }

    private EventType eventType;
    private String jobName;
    private String jobGroup;
    private String triggerName;
    private String triggerGroup;
    private Long durationMs;
    private String message;
    private String correlationId;
    @Builder.Default
    private Instant timestamp = Instant.now();
}
