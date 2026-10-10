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
 * Concrete strategy for generating Interval-based Quartz Triggers.
 */
@Component
public class SimpleIntervalTriggerStrategy implements TriggerStrategy {

    @Override
    public TriggerType getTriggerType() {
        return TriggerType.SIMPLE_INTERVAL;
    }

    @Override
    public Trigger buildTrigger(CreateJobRequest request, JobKey jobKey) {
        int interval = validateInterval(request.getIntervalInSeconds());
        int repeatCount = request.getRepeatCount() == null ? -1 : request.getRepeatCount();

        String triggerName = request.getJobName() + "-interval-trigger";
        String triggerGroup = request.getJobGroup();

        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(interval)
                .withMisfireHandlingInstructionNextWithExistingCount();

        if (repeatCount < 0) {
            scheduleBuilder.repeatForever();
        } else {
            scheduleBuilder.withRepeatCount(repeatCount);
        }

        TriggerBuilder<SimpleTrigger> builder = TriggerBuilder.newTrigger()
                .withIdentity(TriggerKey.triggerKey(triggerName, triggerGroup))
                .forJob(jobKey)
                .withDescription("Interval trigger for job " + jobKey.getName())
                .withSchedule(scheduleBuilder);

        if (request.getStartAt() != null) {
            builder.startAt(Date.from(request.getStartAt()));
        } else {
            builder.startNow();
        }

        if (request.getEndAt() != null) {
            builder.endAt(Date.from(request.getEndAt()));
        }

        return builder.build();
    }

    @Override
    public Trigger buildRescheduleTrigger(RescheduleJobRequest request, TriggerKey existingTriggerKey) {
        int interval = validateInterval(request.getIntervalInSeconds());
        int repeatCount = request.getRepeatCount() == null ? -1 : request.getRepeatCount();

        SimpleScheduleBuilder scheduleBuilder = SimpleScheduleBuilder.simpleSchedule()
                .withIntervalInSeconds(interval)
                .withMisfireHandlingInstructionNextWithExistingCount();

        if (repeatCount < 0) {
            scheduleBuilder.repeatForever();
        } else {
            scheduleBuilder.withRepeatCount(repeatCount);
        }

        TriggerBuilder<SimpleTrigger> builder = TriggerBuilder.newTrigger()
                .withIdentity(existingTriggerKey)
                .withSchedule(scheduleBuilder);

        if (request.getStartAt() != null) {
            builder.startAt(Date.from(request.getStartAt()));
        } else {
            builder.startNow();
        }

        if (request.getEndAt() != null) {
            builder.endAt(Date.from(request.getEndAt()));
        }

        return builder.build();
    }

    private int validateInterval(Integer interval) {
        if (interval == null || interval <= 0) {
            throw new IllegalArgumentException("Interval in seconds must be greater than 0 for SIMPLE_INTERVAL trigger type");
        }
        return interval;
    }
}
