package com.enterprise.quartz.pattern.strategy;

import com.enterprise.quartz.dto.request.CreateJobRequest;
import com.enterprise.quartz.dto.request.RescheduleJobRequest;
import com.enterprise.quartz.dto.request.TriggerType;
import org.quartz.JobKey;
import org.quartz.SimpleScheduleBuilder;
import org.quartz.SimpleTrigger;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Concrete strategy for generating One-Time execution Quartz Triggers.
 */
@Component
public class OneTimeTriggerStrategy implements TriggerStrategy {

    @Override
    public TriggerType getTriggerType() {
        return TriggerType.ONE_TIME;
    }

    @Override
    public Trigger buildTrigger(CreateJobRequest request, JobKey jobKey) {
        String triggerName = request.getJobName() + "-onetime-trigger";
        String triggerGroup = request.getJobGroup();

        TriggerBuilder<SimpleTrigger> builder = TriggerBuilder.newTrigger()
                .withIdentity(TriggerKey.triggerKey(triggerName, triggerGroup))
                .forJob(jobKey)
                .withDescription("One-time trigger for job " + jobKey.getName())
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withRepeatCount(0)
                        .withMisfireHandlingInstructionFireNow());

        if (request.getStartAt() != null) {
            builder.startAt(Date.from(request.getStartAt()));
        } else {
            builder.startNow();
        }

        return builder.build();
    }

    @Override
    public Trigger buildRescheduleTrigger(RescheduleJobRequest request, TriggerKey existingTriggerKey) {
        TriggerBuilder<SimpleTrigger> builder = TriggerBuilder.newTrigger()
                .withIdentity(existingTriggerKey)
                .withSchedule(SimpleScheduleBuilder.simpleSchedule()
                        .withRepeatCount(0)
                        .withMisfireHandlingInstructionFireNow());

        if (request.getStartAt() != null) {
            builder.startAt(Date.from(request.getStartAt()));
        } else {
            builder.startNow();
        }

        return builder.build();
    }
}
