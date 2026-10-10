package com.enterprise.quartz.pattern.strategy;

import com.enterprise.quartz.dto.request.CreateJobRequest;
import com.enterprise.quartz.dto.request.RescheduleJobRequest;
import com.enterprise.quartz.dto.request.TriggerType;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerKey;

/**
 * Strategy interface for building Quartz Triggers based on scheduling type.
 */
public interface TriggerStrategy {

    /**
     * Identifies the TriggerType supported by this strategy.
     */
    TriggerType getTriggerType();

    /**
     * Builds a new Trigger for an initial job creation request.
     *
     * @param request the creation payload
     * @param jobKey  the associated JobKey
     * @return constructed Quartz Trigger
     */
    Trigger buildTrigger(CreateJobRequest request, JobKey jobKey);

    /**
     * Builds an updated Trigger for an existing trigger key.
     *
     * @param request            the reschedule payload
     * @param existingTriggerKey the trigger key to replace
     * @return reconstructed Quartz Trigger
     */
    Trigger buildRescheduleTrigger(RescheduleJobRequest request, TriggerKey existingTriggerKey);
}
