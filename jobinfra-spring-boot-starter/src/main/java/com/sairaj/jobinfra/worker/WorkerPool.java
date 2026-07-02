package com.sairaj.jobinfra.worker;

import com.sairaj.jobinfra.queue.JobQueue;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import jakarta.annotation.PreDestroy;
import com.sairaj.jobinfra.executor.JobExecutionStrategy;
import java.util.List;
import java.util.Map;
import java.util.EnumMap;
import com.sairaj.jobinfra.core.ExecutionType;
import org.springframework.context.ApplicationEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

public class WorkerPool {
    private final ExecutorService executor;
    private final int poolSize;
    private static final Logger auditLog = LoggerFactory.getLogger("com.sairaj.jobinfra.audit");
    
    public WorkerPool(JobQueue queue, List<JobExecutionStrategy> strategies, int poolSize, ApplicationEventPublisher eventPublisher) {
        this.executor = Executors.newFixedThreadPool(poolSize);
        this.poolSize = poolSize;
        Map<ExecutionType, JobExecutionStrategy> strategyMap = new EnumMap<>(ExecutionType.class);
        for (JobExecutionStrategy strategy : strategies) {
            strategyMap.put(strategy.getSupportedType(), strategy);
        }

        MDC.put("auditType", "WORKER");
        MDC.put("action", "STARTED");
        MDC.put("workerCount", String.valueOf(poolSize));
        auditLog.info("Worker Event: STARTED | WorkerCount: {}", poolSize);
        MDC.clear();

        for(int i = 0; i < poolSize; i++) {
            String workerName = "worker-" + i;
            executor.submit(new Worker(workerName, queue, strategyMap, eventPublisher));
        }
    }

    @PreDestroy
    public void shutdown() {
        MDC.put("auditType", "WORKER");
        MDC.put("action", "SHUTDOWN");
        MDC.put("workerCount", String.valueOf(poolSize));
        auditLog.info("Worker Event: SHUTDOWN | WorkerCount: {}", poolSize);
        MDC.clear();
        
        executor.shutdown();
    }
}