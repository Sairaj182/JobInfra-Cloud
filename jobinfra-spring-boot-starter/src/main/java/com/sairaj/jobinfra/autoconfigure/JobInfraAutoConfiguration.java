package com.sairaj.jobinfra.autoconfigure;

import com.sairaj.jobinfra.queue.*;
import com.sairaj.jobinfra.worker.*;
import com.sairaj.jobinfra.registry.*;
import com.sairaj.jobinfra.executor.*;
import com.sairaj.jobinfra.service.*;
import com.sairaj.jobinfra.properties.JobInfraProperties;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;

import java.util.List;

@Configuration
@EnableConfigurationProperties(JobInfraProperties.class)
public class JobInfraAutoConfiguration {

    @Bean
    @ConditionalOnMissingBean
    public JobQueue jobQueue() {
        return new InMemoryJobQueue();
    }

    @Bean
    @ConditionalOnMissingBean
    public JobRegistry jobRegistry() {
        return new InMemoryJobRegistry();
    }

    @Bean
    @ConditionalOnMissingBean
    public JobExecutorRegistry jobExecutorRegistry(List<JobExecutor> executors) {
        return new JobExecutorRegistry(executors);
    }

    @Bean
    @ConditionalOnMissingBean
    public JobService jobService(JobQueue queue, JobRegistry registry) {
        return new JobService(queue, registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringHandlerExecutionStrategy springHandlerExecutionStrategy(JobExecutorRegistry registry) {
        return new SpringHandlerExecutionStrategy(registry);
    }

    @Bean
    @ConditionalOnMissingBean
    public WebhookExecutionStrategy webhookExecutionStrategy(com.fasterxml.jackson.databind.ObjectMapper objectMapper, JobInfraProperties properties) {
        return new WebhookExecutionStrategy(objectMapper, properties);
    }

    @Bean
    public WorkerPool workerPool(JobQueue queue, List<JobExecutionStrategy> strategies, JobInfraProperties properties, org.springframework.context.ApplicationEventPublisher eventPublisher) {
        int poolSize = properties.getWorker().getCount();
        return new WorkerPool(queue, strategies, poolSize, eventPublisher);
    }

}
