package com.sairaj.jobinfra.executor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sairaj.jobinfra.core.ExecutionType;
import com.sairaj.jobinfra.core.Job;
import com.sairaj.jobinfra.properties.JobInfraProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

@Component
public class WebhookExecutionStrategy implements JobExecutionStrategy {

    private final HttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final JobInfraProperties properties;
    private static final Logger auditLog = LoggerFactory.getLogger("com.sairaj.jobinfra.audit");

    public WebhookExecutionStrategy(ObjectMapper objectMapper, JobInfraProperties properties) {
        this.objectMapper = objectMapper;
        this.properties = properties;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(properties.getWebhook().getTimeoutMs()))
                .build();
    }

    private void logWebhookAudit(String action, Job job, String status, Long durationMs) {
        MDC.put("auditType", "WEBHOOK");
        MDC.put("action", action);
        MDC.put("jobId", job.getId());
        MDC.put("status", status);
        if (durationMs != null) MDC.put("durationMs", String.valueOf(durationMs));
        
        auditLog.info("Webhook Event: {} | JobId: {} | Status: {}", action, job.getId(), status);
        MDC.clear();
    }

    @Override
    public ExecutionResult execute(Job job) {
        long startTime = System.currentTimeMillis();
        
        logWebhookAudit("DISPATCH", job, "IN_PROGRESS", null);
        
        try {
            String url = job.getWebhookUrl();
            if (url == null || isInternalUrl(url)) {
                throw new IllegalArgumentException("Invalid or forbidden Webhook URL");
            }

            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("jobId", job.getId());
            payloadMap.put("projectId", job.getProjectId());
            payloadMap.put("timestamp", Instant.now().toString());
            payloadMap.put("payload", job.getPayload());

            String jsonBody = objectMapper.writeValueAsString(payloadMap);
            String signature = generateHmacSha256(jsonBody, properties.getWebhook().getSecret());

            HttpRequest.Builder requestBuilder = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(Duration.ofMillis(properties.getWebhook().getTimeoutMs()))
                    .header("Content-Type", "application/json")
                    .header("X-JobInfra-Signature", signature)
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody));

            if (job.getWebhookHeaders() != null) {
                job.getWebhookHeaders().forEach((k, v) -> {
                    // Do not log authorization headers
                    requestBuilder.header(k, v);
                });
            }

            HttpResponse<String> response = httpClient.send(requestBuilder.build(), HttpResponse.BodyHandlers.ofString());

            long duration = System.currentTimeMillis() - startTime;
            String responseBody = truncate(response.body(), 1000);

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                logWebhookAudit("SUCCESS", job, String.valueOf(response.statusCode()), duration);
                return ExecutionResult.success(response.statusCode(), duration, responseBody);
            } else {
                logWebhookAudit("FAILURE", job, String.valueOf(response.statusCode()), duration);
                return ExecutionResult.failure("HTTP " + response.statusCode(), response.statusCode(), duration, responseBody);
            }

        } catch (Exception e) {
            long duration = System.currentTimeMillis() - startTime;
            logWebhookAudit("FAILURE", job, "ERROR", duration);
            return ExecutionResult.failure(e.getMessage(), 0, duration, null);
        }
    }

    @Override
    public ExecutionType getSupportedType() {
        return ExecutionType.WEBHOOK;
    }

    private boolean isInternalUrl(String url) {
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("localhost") || 
               lowerUrl.contains("127.0.0.1") || 
               lowerUrl.contains("0.0.0.0") ||
               lowerUrl.contains("::1");
    }

    private String generateHmacSha256(String data, String secret) throws Exception {
        Mac sha256_HMAC = Mac.getInstance("HmacSHA256");
        SecretKeySpec secret_key = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        sha256_HMAC.init(secret_key);
        byte[] hash = sha256_HMAC.doFinal(data.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null) return null;
        return text.length() > maxLength ? text.substring(0, maxLength) + "..." : text;
    }
}
