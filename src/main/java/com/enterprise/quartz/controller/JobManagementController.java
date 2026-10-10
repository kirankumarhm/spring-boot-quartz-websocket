package com.enterprise.quartz.controller;

import com.enterprise.quartz.dto.request.CreateJobRequest;
import com.enterprise.quartz.dto.request.RescheduleJobRequest;
import com.enterprise.quartz.dto.response.ApiResponse;
import com.enterprise.quartz.dto.response.JobResponse;
import com.enterprise.quartz.service.QuartzJobService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.enterprise.quartz.dto.response.CurrentlyExecutingJobResponse;
import com.enterprise.quartz.dto.response.JobExecutionRecord;
import com.enterprise.quartz.dto.response.SchedulerMetadataResponse;
import org.springframework.web.bind.annotation.RequestParam;
import java.util.List;
import java.util.Map;

/**
 * REST Controller providing full lifecycle management for Quartz Jobs and Triggers.
 */
@Slf4j
@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
@Tag(name = "Quartz Jobs Management", description = "Endpoints for creating, managing, pausing, resuming, and triggering Quartz tasks")
public class JobManagementController {

    private final QuartzJobService quartzJobService;

    @GetMapping
    @Operation(summary = "List all jobs", description = "Retrieves all registered jobs, their execution states, and triggers across all groups.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Successfully retrieved jobs"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "Unauthorized"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "403", description = "Forbidden")
    })
    public ResponseEntity<List<JobResponse>> getAllJobs() {
        return ResponseEntity.ok(quartzJobService.getAllJobs());
    }

    @GetMapping("/{group}/{name}")
    @Operation(summary = "Get job details", description = "Retrieves metadata, runtime execution status, and triggers for a specific job.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job found"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<JobResponse> getJob(
            @Parameter(description = "Logical group name of the job", example = "notifications")
            @PathVariable String group,
            @Parameter(description = "Identifier name of the job", example = "email-digest-job")
            @PathVariable String name) {
        return ResponseEntity.ok(quartzJobService.getJob(group, name));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register and schedule a new job", description = "Registers a new durable job and schedule its initial trigger strategy.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "Job scheduled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "Invalid request payload or cron expression",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class))),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "409", description = "Job already exists",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<JobResponse> createJob(@Valid @RequestBody CreateJobRequest request) {
        JobResponse created = quartzJobService.createJob(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{group}/{name}/reschedule")
    @Operation(summary = "Reschedule an existing job trigger", description = "Updates the schedule (cron or interval) for an existing job.")
    @ApiResponses({
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Job rescheduled successfully"),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "Job not found",
                    content = @Content(schema = @Schema(implementation = ProblemDetail.class)))
    })
    public ResponseEntity<JobResponse> rescheduleJob(
            @PathVariable String group,
            @PathVariable String name,
            @Valid @RequestBody RescheduleJobRequest request) {
        return ResponseEntity.ok(quartzJobService.rescheduleJob(group, name, request));
    }

    @PostMapping("/{group}/{name}/pause")
    @Operation(summary = "Pause a job", description = "Pauses all triggers associated with the specified job.")
    public ResponseEntity<ApiResponse<Void>> pauseJob(@PathVariable String group, @PathVariable String name) {
        quartzJobService.pauseJob(group, name);
        return ResponseEntity.ok(ApiResponse.ok("Job '" + group + "." + name + "' paused successfully"));
    }

    @PostMapping("/{group}/{name}/resume")
    @Operation(summary = "Resume a job", description = "Resumes all triggers associated with the specified paused job.")
    public ResponseEntity<ApiResponse<Void>> resumeJob(@PathVariable String group, @PathVariable String name) {
        quartzJobService.resumeJob(group, name);
        return ResponseEntity.ok(ApiResponse.ok("Job '" + group + "." + name + "' resumed successfully"));
    }

    @PostMapping("/{group}/{name}/trigger")
    @Operation(summary = "Trigger a job immediately", description = "Fires an immediate, one-time out-of-band execution with optional extra parameters.")
    public ResponseEntity<ApiResponse<Void>> triggerJobNow(
            @PathVariable String group,
            @PathVariable String name,
            @RequestBody(required = false) Map<String, Object> jobData) {
        quartzJobService.triggerJobNow(group, name, jobData);
        return ResponseEntity.ok(ApiResponse.ok("Job '" + group + "." + name + "' triggered immediately"));
    }

    @DeleteMapping("/{group}/{name}")
    @Operation(summary = "Delete a job", description = "Un-schedules and completely removes a job from the Quartz store.")
    public ResponseEntity<ApiResponse<Void>> deleteJob(@PathVariable String group, @PathVariable String name) {
        quartzJobService.deleteJob(group, name);
        return ResponseEntity.ok(ApiResponse.ok("Job '" + group + "." + name + "' deleted successfully"));
    }

    @PostMapping("/pause-all")
    @Operation(summary = "Globally pause scheduler", description = "Pauses all triggers across all job groups in the cluster.")
    public ResponseEntity<ApiResponse<Void>> pauseAll() {
        quartzJobService.pauseAll();
        return ResponseEntity.ok(ApiResponse.ok("Scheduler globally paused"));
    }

    @PostMapping("/resume-all")
    @Operation(summary = "Globally resume scheduler", description = "Resumes all triggers across all job groups in the cluster.")
    public ResponseEntity<ApiResponse<Void>> resumeAll() {
        quartzJobService.resumeAll();
        return ResponseEntity.ok(ApiResponse.ok("Scheduler globally resumed"));
    }

    @GetMapping("/cluster-metadata")
    @Operation(summary = "Get scheduler and cluster metadata", description = "Retrieves Quartz cluster instance topology, version, thread pool metrics, and JobStore details.")
    public ResponseEntity<SchedulerMetadataResponse> getClusterMetadata() {
        return ResponseEntity.ok(quartzJobService.getSchedulerMetadata());
    }

    @GetMapping("/executing")
    @Operation(summary = "Get currently executing jobs", description = "Retrieves all in-flight jobs currently running across worker threads with elapsed execution time.")
    public ResponseEntity<List<CurrentlyExecutingJobResponse>> getCurrentlyExecutingJobs() {
        return ResponseEntity.ok(quartzJobService.getCurrentlyExecutingJobs());
    }

    @GetMapping("/history")
    @Operation(summary = "Get execution history audit log", description = "Retrieves recent job execution records with durations, correlation IDs, and statuses.")
    public ResponseEntity<List<JobExecutionRecord>> getExecutionHistory(
            @RequestParam(required = false) String group,
            @RequestParam(required = false) String name,
            @RequestParam(defaultValue = "100") int limit) {
        return ResponseEntity.ok(quartzJobService.getExecutionHistory(group, name, limit));
    }

    @DeleteMapping("/history")
    @Operation(summary = "Clear execution history", description = "Clears the in-memory execution audit log.")
    public ResponseEntity<ApiResponse<Void>> clearHistory() {
        quartzJobService.clearExecutionHistory();
        return ResponseEntity.ok(ApiResponse.ok("Execution history cleared successfully"));
    }
}
