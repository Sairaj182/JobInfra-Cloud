package com.sairaj.jobinfra.server.controller;

import com.sairaj.jobinfra.queue.JobQueue;
import com.sairaj.jobinfra.server.controller.dto.ApiResponse;
import com.sairaj.jobinfra.server.service.MetricsRegistry;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
public class ObservabilityController {

    private final DataSource dataSource;
    private final MetricsRegistry metricsRegistry;
    private final JobQueue jobQueue;

    public ObservabilityController(DataSource dataSource, MetricsRegistry metricsRegistry, JobQueue jobQueue) {
        this.dataSource = dataSource;
        this.metricsRegistry = metricsRegistry;
        this.jobQueue = jobQueue;
    }

    @GetMapping("/")
    public ResponseEntity<ApiResponse<Map<String, Object>>> root() {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("name", "JobInfra Cloud");
        metadata.put("version", "1.0.0");
        metadata.put("status", "ACTIVE");
        metadata.put("documentationUrl", "/swagger-ui/index.html");
        metadata.put("healthUrl", "/health");
        return ResponseEntity.ok(ApiResponse.success(metadata));
    }

    @GetMapping("/version")
    public ResponseEntity<ApiResponse<Map<String, Object>>> version() {
        Map<String, Object> versionInfo = new LinkedHashMap<>();
        versionInfo.put("version", "1.0.0");
        versionInfo.put("buildTime", Instant.now().toString()); 
        versionInfo.put("commit", "latest");
        return ResponseEntity.ok(ApiResponse.success(versionInfo));
    }

    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> health() {
        Map<String, Object> health = new LinkedHashMap<>();
        health.put("status", "UP");
        health.put("database", checkDatabase());
        health.put("uptimeSeconds", metricsRegistry.getUptimeMs() / 1000);
        health.put("timestamp", Instant.now().toString());
        return ResponseEntity.ok(ApiResponse.success(health));
    }

    @GetMapping("/api/v1/system/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> metrics() {
        Map<String, Object> metrics = new LinkedHashMap<>();
        
        metrics.put("applicationUptimeSeconds", metricsRegistry.getUptimeMs() / 1000);
        metrics.put("queueSize", jobQueue.size());
        
        metrics.put("jobsSubmitted", metricsRegistry.getJobsSubmitted());
        metrics.put("jobsRunning", metricsRegistry.getJobsRunning());
        metrics.put("jobsCompleted", metricsRegistry.getJobsCompleted());
        metrics.put("jobsFailed", metricsRegistry.getJobsFailed());
        metrics.put("jobsCancelled", metricsRegistry.getJobsCancelled());
        
        metrics.put("requestsServed", metricsRegistry.getRequestsServed());
        metrics.put("averageResponseTimeMs", metricsRegistry.getAverageResponseTimeMs());
        metrics.put("maxResponseTimeMs", metricsRegistry.getMaxResponseTimeMs());
        
        Runtime runtime = Runtime.getRuntime();
        metrics.put("heapMemoryUsedMb", (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024));
        metrics.put("heapMemoryMaxMb", runtime.maxMemory() / (1024 * 1024));
        metrics.put("availableProcessors", runtime.availableProcessors());
        
        return ResponseEntity.ok(ApiResponse.success(metrics));
    }

    private String checkDatabase() {
        try (java.sql.Connection conn = dataSource.getConnection()) {
            if (conn.isValid(1)) {
                return "UP";
            }
        } catch (Exception e) {
            return "DOWN (" + e.getMessage() + ")";
        }
        return "DOWN";
    }
}
