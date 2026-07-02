package com.sairaj.jobinfra.server.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Service;

@Service
public class AuditLogger {

    private static final Logger auditLog = LoggerFactory.getLogger("com.sairaj.jobinfra.audit");

    public void logAuth(String action, String username, boolean success, String details) {
        MDC.put("auditType", "AUTH");
        MDC.put("action", action);
        MDC.put("username", username);
        MDC.put("success", String.valueOf(success));
        
        auditLog.info("Auth Event: {} | Username: {} | Success: {} | Details: {}", action, username, success, details);
        
        MDC.remove("auditType");
        MDC.remove("action");
        MDC.remove("username");
        MDC.remove("success");
    }

    public void logProject(String action, String projectId, String projectName, String username) {
        MDC.put("auditType", "PROJECT");
        MDC.put("action", action);
        MDC.put("projectId", projectId);
        MDC.put("projectName", projectName);
        MDC.put("username", username);
        
        auditLog.info("Project Event: {} | ProjectId: {} | ProjectName: {} | Username: {}", action, projectId, projectName, username);
        
        MDC.remove("auditType");
        MDC.remove("action");
        MDC.remove("projectId");
        MDC.remove("projectName");
        MDC.remove("username");
    }

    public void logApiKey(String action, String projectId, String apiKeyId, String username) {
        MDC.put("auditType", "API_KEY");
        MDC.put("action", action);
        MDC.put("projectId", projectId);
        MDC.put("apiKeyId", apiKeyId);
        MDC.put("username", username);
        
        auditLog.info("API Key Event: {} | ProjectId: {} | ApiKeyId: {} | Username: {}", action, projectId, apiKeyId, username);
        
        MDC.remove("auditType");
        MDC.remove("action");
        MDC.remove("projectId");
        MDC.remove("apiKeyId");
        MDC.remove("username");
    }

    public void logJob(String action, String jobId, String projectId, String workerName, String executionType, Long durationMs, Integer retryCount, String status) {
        MDC.put("auditType", "JOB");
        MDC.put("action", action);
        MDC.put("jobId", jobId);
        if (projectId != null) MDC.put("projectId", projectId);
        if (workerName != null) MDC.put("workerName", workerName);
        if (executionType != null) MDC.put("executionType", executionType);
        if (durationMs != null) MDC.put("durationMs", String.valueOf(durationMs));
        if (retryCount != null) MDC.put("retryCount", String.valueOf(retryCount));
        if (status != null) MDC.put("status", status);
        
        auditLog.info("Job Event: {} | JobId: {} | Status: {}", action, jobId, status != null ? status : "N/A");
        
        MDC.remove("auditType");
        MDC.remove("action");
        MDC.remove("jobId");
        MDC.remove("projectId");
        MDC.remove("workerName");
        MDC.remove("executionType");
        MDC.remove("durationMs");
        MDC.remove("retryCount");
        MDC.remove("status");
    }

    public void logWebhook(String action, String jobId, String status, Long durationMs) {
        MDC.put("auditType", "WEBHOOK");
        MDC.put("action", action);
        MDC.put("jobId", jobId);
        MDC.put("status", status);
        if (durationMs != null) MDC.put("durationMs", String.valueOf(durationMs));
        
        auditLog.info("Webhook Event: {} | JobId: {} | Status: {}", action, jobId, status);
        
        MDC.remove("auditType");
        MDC.remove("action");
        MDC.remove("jobId");
        MDC.remove("status");
        MDC.remove("durationMs");
    }

    public void logWorker(String action, String workerName, int currentWorkerCount) {
        MDC.put("auditType", "WORKER");
        MDC.put("action", action);
        MDC.put("workerName", workerName);
        MDC.put("workerCount", String.valueOf(currentWorkerCount));
        
        auditLog.info("Worker Event: {} | WorkerName: {} | WorkerCount: {}", action, workerName, currentWorkerCount);
        
        MDC.remove("auditType");
        MDC.remove("action");
        MDC.remove("workerName");
        MDC.remove("workerCount");
    }
}
