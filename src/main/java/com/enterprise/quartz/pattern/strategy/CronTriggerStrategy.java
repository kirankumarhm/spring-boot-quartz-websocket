package com.enterprise.quartz.pattern.strategy;

import com.enterprise.quartz.dto.request.CreateJobRequest;
import com.enterprise.quartz.dto.request.RescheduleJobRequest;
import com.enterprise.quartz.dto.request.TriggerType;
import com.enterprise.quartz.exception.InvalidCronExpressionException;
import org.quartz.CronExpression;
import org.quartz.CronScheduleBuilder;
import org.quartz.CronTrigger;
import org.quartz.JobKey;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.quartz.TriggerKey;
import org.springframework.stereotype.Component;

import java.util.Date;

/**
 * Concrete strategy for generating Cron-based Quartz Triggers.
 */
@Component
public class CronTriggerStrategy implements TriggerStrategy {

    @Override
    public TriggerType getTriggerType() {
        return TriggerType.CRON;
    }

    @Override
    public Trigger buildTrigger(CreateJobRequest request, JobKey jobKey) {
        String cron = request.getCronExpression();
        validateCron(cron);

        String triggerName = request.getJobName() + "-cron-trigger";
        String triggerGroup = request.getJobGroup();

        TriggerBuilder<CronTrigger> builder = TriggerBuilder.newTrigger()
                .withIdentity(TriggerKey.triggerKey(triggerName, triggerGroup))
                .forJob(jobKey)
                .withDescription("Cron trigger for job " + jobKey.getName())
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionDoNothing());

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
        String cron = request.getCronExpression();
        validateCron(cron);

        TriggerBuilder<CronTrigger> builder = TriggerBuilder.newTrigger()
                .withIdentity(existingTriggerKey)
                .withSchedule(CronScheduleBuilder.cronSchedule(cron)
                        .withMisfireHandlingInstructionDoNothing());

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

    private void validateCron(String cron) {
        if (cron == null || cron.isBlank()) {
            throw new InvalidCronExpressionException("<null/empty>", "Cron expression must not be blank for CRON trigger type");
        }
        if (!CronExpression.isValidExpression(cron)) {
            throw new InvalidCronExpressionException(cron, "Syntax error in expression format");
        }
    }
}
