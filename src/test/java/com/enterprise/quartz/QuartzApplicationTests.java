package com.enterprise.quartz;

import com.enterprise.quartz.dto.request.CreateJobRequest;
import com.enterprise.quartz.dto.request.RescheduleJobRequest;
import com.enterprise.quartz.dto.request.TriggerType;
import com.enterprise.quartz.dto.response.JobResponse;
import com.enterprise.quartz.exception.InvalidCronExpressionException;
import com.enterprise.quartz.exception.JobConflictException;
import com.enterprise.quartz.exception.JobNotFoundException;
import com.enterprise.quartz.sample.SampleEmailJob;
import com.enterprise.quartz.service.QuartzJobService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.quartz.Scheduler;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
@ActiveProfiles("h2")
class QuartzApplicationTests {

    @Autowired
    private Scheduler scheduler;

    @Autowired
    private QuartzJobService quartzJobService;

    @Test
    @DisplayName("Context loads and Quartz scheduler starts successfully")
    void contextLoads() throws Exception {
        assertThat(scheduler).isNotNull();
        assertThat(scheduler.isStarted()).isTrue();
    }

    @Test
    @DisplayName("Successfully create, retrieve, pause, resume, trigger and delete a Cron job")
    void testCronJobLifecycle() {
        String jobName = "test-email-job";
        String jobGroup = "test-group";

        CreateJobRequest request = CreateJobRequest.builder()
                .jobName(jobName)
                .jobGroup(jobGroup)
                .jobClass(SampleEmailJob.class.getName())
                .description("Automated test email job")
                .triggerType(TriggerType.CRON)
                .cronExpression("0 0/10 * * * ?")
                .jobData(Map.of("recipient", "qa@enterprise.com", "subject", "Integration Test"))
                .durable(true)
                .build();

        // 1. Create
        JobResponse created = quartzJobService.createJob(request);
        assertThat(created).isNotNull();
        assertThat(created.getJobName()).isEqualTo(jobName);
        assertThat(created.getJobGroup()).isEqualTo(jobGroup);
        assertThat(created.getTriggers()).hasSize(1);
        assertThat(created.getTriggers().getFirst().getCronExpression()).isEqualTo("0 0/10 * * * ?");

        // 2. Query
        JobResponse retrieved = quartzJobService.getJob(jobGroup, jobName);
        assertThat(retrieved).isNotNull();
        assertThat(retrieved.getJobName()).isEqualTo(jobName);

        // 3. Pause
        quartzJobService.pauseJob(jobGroup, jobName);
        JobResponse paused = quartzJobService.getJob(jobGroup, jobName);
        assertThat(paused.getTriggers().getFirst().getState()).isEqualTo("PAUSED");

        // 4. Resume
        quartzJobService.resumeJob(jobGroup, jobName);
        JobResponse resumed = quartzJobService.getJob(jobGroup, jobName);
        assertThat(resumed.getTriggers().getFirst().getState()).isIn("NORMAL", "WAITING");

        // 5. Trigger Out of Band
        quartzJobService.triggerJobNow(jobGroup, jobName);

        // 6. Reschedule
        RescheduleJobRequest rescheduleRequest = RescheduleJobRequest.builder()
                .triggerType(TriggerType.CRON)
                .cronExpression("0 0/20 * * * ?")
                .build();
        JobResponse rescheduled = quartzJobService.rescheduleJob(jobGroup, jobName, rescheduleRequest);
        assertThat(rescheduled.getTriggers().stream()
                .anyMatch(t -> "0 0/20 * * * ?".equals(t.getCronExpression()))).isTrue();

        // 7. Delete
        quartzJobService.deleteJob(jobGroup, jobName);
        assertThatThrownBy(() -> quartzJobService.getJob(jobGroup, jobName))
                .isInstanceOf(JobNotFoundException.class);
    }

    @Test
    @DisplayName("Job conflict throws JobConflictException when registering duplicate key")
    void testDuplicateJobConflict() {
        String jobName = "duplicate-job";
        String jobGroup = "test-group";

        CreateJobRequest request = CreateJobRequest.builder()
                .jobName(jobName)
                .jobGroup(jobGroup)
                .jobClass(SampleEmailJob.class.getName())
                .triggerType(TriggerType.ONE_TIME)
                .build();

        quartzJobService.createJob(request);

        assertThatThrownBy(() -> quartzJobService.createJob(request))
                .isInstanceOf(JobConflictException.class);

        // Cleanup
        quartzJobService.deleteJob(jobGroup, jobName);
    }

    @Test
    @DisplayName("Invalid cron expression throws InvalidCronExpressionException")
    void testInvalidCronExpression() {
        CreateJobRequest request = CreateJobRequest.builder()
                .jobName("invalid-cron-job")
                .jobGroup("test-group")
                .jobClass(SampleEmailJob.class.getName())
                .triggerType(TriggerType.CRON)
                .cronExpression("invalid cron expression syntax")
                .build();

        assertThatThrownBy(() -> quartzJobService.createJob(request))
                .isInstanceOf(InvalidCronExpressionException.class);
    }

    @Test
    @DisplayName("Verify scheduler metadata and history query")
    void testSchedulerMetadataAndHistory() {
        var metadata = quartzJobService.getSchedulerMetadata();
        assertThat(metadata).isNotNull();
        assertThat(metadata.getSchedulerName()).isNotBlank();
        assertThat(metadata.getStatus()).isEqualTo("RUNNING");
        assertThat(metadata.getThreadPoolSize()).isGreaterThan(0);

        var executing = quartzJobService.getCurrentlyExecutingJobs();
        assertThat(executing).isNotNull();

        var history = quartzJobService.getExecutionHistory(null, null, 10);
        assertThat(history).isNotNull();
    }
}
