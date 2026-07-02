package com.sairaj.jobinfra.worker;

import com.sairaj.jobinfra.core.Job;
import com.sairaj.jobinfra.core.JobStatus;
import com.sairaj.jobinfra.core.JobLifecycleEvent;
import com.sairaj.jobinfra.queue.JobQueue;
import com.sairaj.jobinfra.executor.JobExecutionStrategy;
import com.sairaj.jobinfra.executor.ExecutionResult;
import com.sairaj.jobinfra.core.ExecutionType;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import java.util.Map;
import java.time.Instant;

public class Worker implements Runnable {
    private final String workerName;
    private final JobQueue queue;
    private final Map<ExecutionType, JobExecutionStrategy> strategyMap;
    private final ApplicationEventPublisher eventPublisher;
    private static final Logger auditLog = LoggerFactory.getLogger("com.sairaj.jobinfra.audit");
    
    public Worker(String workerName, JobQueue queue, Map<ExecutionType, JobExecutionStrategy> strategyMap, ApplicationEventPublisher eventPublisher) {
        this.workerName = workerName;
        this.queue = queue;
        this.strategyMap = strategyMap;
        this.eventPublisher = eventPublisher;
    }

    private void logAudit(String action, Job job, Long durationMs) {
        MDC.put("auditType", "JOB");
        MDC.put("action", action);
        MDC.put("jobId", job.getId());
        if (job.getProjectId() != null) MDC.put("projectId", job.getProjectId());
        MDC.put("workerName", workerName);
        if (job.getExecutionType() != null) MDC.put("executionType", job.getExecutionType().name());
        if (durationMs != null) MDC.put("durationMs", String.valueOf(durationMs));
        MDC.put("retryCount", String.valueOf(job.getRetryCount()));
        MDC.put("status", job.getStatus().name());
        
        auditLog.info("Job Event: {} | JobId: {} | Status: {}", action, job.getId(), job.getStatus());
        MDC.clear();
    }

    public void run() {
        Thread.currentThread().setName(workerName);
        while(true) {
            Job job = null;
            long startTime = 0;
            try {
                job = queue.dequeue();
                job.setStatus(JobStatus.RUNNING);
                job.setStartedAt(Instant.now());
                startTime = System.currentTimeMillis();
                
                logAudit("PICKED", job, null);
                logAudit("STARTED", job, null);
                
                if (eventPublisher != null) {
                    eventPublisher.publishEvent(new JobLifecycleEvent(this, job, JobLifecycleEvent.EventType.STARTED));
                }
                
                ExecutionType type = job.getExecutionType() != null ? job.getExecutionType() : ExecutionType.SPRING_HANDLER;
                JobExecutionStrategy strategy = strategyMap.get(type);
                
                if (strategy == null) {
                    throw new IllegalStateException("No execution strategy found for type: " + type);
                }

                ExecutionResult result = strategy.execute(job);
                long duration = System.currentTimeMillis() - startTime;
                
                logAudit("FINISHED", job, duration);
                
                if (result.isSuccess()) {
                    job.setStatus(JobStatus.SUCCESS);
                    job.setCompletedAt(Instant.now());
                    logAudit("COMPLETED", job, duration);
                    if (eventPublisher != null) {
                        eventPublisher.publishEvent(new JobLifecycleEvent(this, job, JobLifecycleEvent.EventType.COMPLETED));
                    }
                } else {
                    throw new RuntimeException("Job execution failed: " + result.getErrorMessage());
                }
            } catch(Exception e) {
                if (job == null) continue;
                long duration = startTime > 0 ? System.currentTimeMillis() - startTime : 0;
                job.incrementRetryCount();
                
                if (job.getRetryCount() <= job.getMaxRetries()) {
                    job.setStatus(JobStatus.QUEUED);
                    logAudit("RETRY_ATTEMPT", job, duration);
                    queue.enqueue(job);
                } else {
                    job.setStatus(JobStatus.FAILED);
                    logAudit("FAILED", job, duration);
                    if (eventPublisher != null) {
                        eventPublisher.publishEvent(new JobLifecycleEvent(this, job, JobLifecycleEvent.EventType.FAILED));
                    }
                }
            }
        }
    }
}