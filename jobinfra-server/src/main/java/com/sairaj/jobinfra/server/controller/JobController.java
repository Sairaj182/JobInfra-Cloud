package com.sairaj.jobinfra.server.controller;

import com.sairaj.jobinfra.core.Job;
import com.sairaj.jobinfra.service.JobService;
import com.sairaj.jobinfra.server.controller.dto.ApiResponse;
import com.sairaj.jobinfra.server.service.AuditLogger;
import com.sairaj.jobinfra.server.service.MetricsRegistry;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;

@RestController
@RequestMapping("/api/v1/jobs")
@Tag(name = "Jobs", description = "Submit and manage jobs using API Key Authentication")
public class JobController {

    private final JobService jobService;
    private final AuditLogger auditLogger;
    private final MetricsRegistry metricsRegistry;

    public JobController(JobService jobService, AuditLogger auditLogger, MetricsRegistry metricsRegistry) {
        this.jobService = jobService;
        this.auditLogger = auditLogger;
        this.metricsRegistry = metricsRegistry;
    }

    @PostMapping
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<String>> submitJob(@jakarta.validation.Valid @RequestBody JobRequest request) {
        String projectId = null;
        org.springframework.security.core.Authentication auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getCredentials() instanceof com.sairaj.jobinfra.server.domain.ApiKeyEntity apiKey) {
            projectId = String.valueOf(apiKey.getProject().getId());
        }

        String jobId = jobService.submit(
                request.getExecutionType(),
                request.getHandlerName(),
                request.getWebhookUrl(),
                request.getWebhookHeaders(),
                projectId,
                request.getPayload()
        );
        
        metricsRegistry.incrementJobsSubmitted();
        auditLogger.logJob("SUBMITTED", jobId, projectId, null, 
                request.getExecutionType() != null ? request.getExecutionType().name() : null, 
                null, null, "QUEUED");
        
        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(jobId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<Job>> getJob(@PathVariable String id) {
        Job job = jobService.get(id);
        if (job == null) {
            return ResponseEntity.status(404).body(com.sairaj.jobinfra.server.controller.dto.ApiResponse.error("NOT_FOUND", "Job not found"));
        }
        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(job));
    }

    @GetMapping
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<Collection<Job>>> getAllJobs() {
        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(jobService.getAll()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<com.sairaj.jobinfra.server.controller.dto.ApiResponse<Void>> deleteJob(@PathVariable String id) {
        Job job = jobService.get(id);
        if (job != null) {
            String projectId = job.getProjectId();
            auditLogger.logJob("CANCELLED", id, projectId, null, 
                    job.getExecutionType() != null ? job.getExecutionType().name() : null, 
                    null, null, "CANCELLED");
            metricsRegistry.incrementJobsCancelled();
        }
        
        jobService.delete(id);
        
        return ResponseEntity.ok(com.sairaj.jobinfra.server.controller.dto.ApiResponse.success(null));
    }

    public static class JobRequest {
        private com.sairaj.jobinfra.core.ExecutionType executionType;
        private String handlerName;
        private String webhookUrl;
        private java.util.Map<String, String> webhookHeaders;
        private String payload;

        public com.sairaj.jobinfra.core.ExecutionType getExecutionType() { return executionType; }
        public void setExecutionType(com.sairaj.jobinfra.core.ExecutionType executionType) { this.executionType = executionType; }
        public String getWebhookUrl() { return webhookUrl; }
        public void setWebhookUrl(String webhookUrl) { this.webhookUrl = webhookUrl; }
        public java.util.Map<String, String> getWebhookHeaders() { return webhookHeaders; }
        public void setWebhookHeaders(java.util.Map<String, String> webhookHeaders) { this.webhookHeaders = webhookHeaders; }
        public String getHandlerName() { return handlerName; }
        public void setHandlerName(String handlerName) { this.handlerName = handlerName; }
        public String getPayload() { return payload; }
        public void setPayload(String payload) { this.payload = payload; }
    }
}
