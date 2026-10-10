package com.enterprise.quartz.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

/**
 * Cluster and Scheduler runtime engine metadata.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SchedulerMetadataResponse {
    private String schedulerName;
    private String schedulerInstanceId;
    private String status; // RUNNING, STANDBY, SHUTDOWN
    private boolean isClustered;
    private String jobStoreClass;
    private String threadPoolClass;
    private int threadPoolSize;
    private Instant runningSince;
    private int numberOfJobsExecuted;
    private int currentlyExecutingCount;
    private String version;
    private boolean inStandbyMode;
    private boolean isShutdown;
}
