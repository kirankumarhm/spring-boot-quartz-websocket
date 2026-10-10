package com.enterprise.quartz.listener;

import lombok.extern.slf4j.Slf4j;
import org.quartz.JobExecutionContext;
import org.quartz.Trigger;
import org.quartz.Trigger.CompletedExecutionInstruction;
import org.quartz.TriggerListener;
import org.springframework.stereotype.Component;

/**
 * Observer listener that logs trigger lifecycle events including misfires.
 */
@Slf4j
@Component
public class LoggingTriggerListener implements TriggerListener {

    public static final String LISTENER_NAME = "GlobalLoggingTriggerListener";

    @Override
    public String getName() {
        return LISTENER_NAME;
    }

    @Override
    public void triggerFired(Trigger trigger, JobExecutionContext context) {
        log.debug("Trigger fired: [{}] for job: [{}]", trigger.getKey(), trigger.getJobKey());
    }

    @Override
    public boolean vetoJobExecution(Trigger trigger, JobExecutionContext context) {
        return false;
    }

    @Override
    public void triggerMisfired(Trigger trigger) {
        log.warn("TRIGGER MISFIRED! Trigger [{}] for Job [{}] failed to fire on scheduled time: {}",
                trigger.getKey(), trigger.getJobKey(), trigger.getNextFireTime());
    }

    @Override
    public void triggerComplete(Trigger trigger, JobExecutionContext context,
                                CompletedExecutionInstruction triggerInstructionCode) {
        log.debug("Trigger completed: [{}] with instruction: {}", trigger.getKey(), triggerInstructionCode);
    }
}
