package com.sairaj.jobinfra.server.service;

import org.springframework.stereotype.Service;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.LongAdder;

@Service
public class MetricsRegistry {

    private final long startTime = System.currentTimeMillis();

    private final LongAdder jobsSubmitted = new LongAdder();
    private final AtomicLong jobsRunning = new AtomicLong(0);
    private final LongAdder jobsCompleted = new LongAdder();
    private final LongAdder jobsFailed = new LongAdder();
    private final LongAdder jobsCancelled = new LongAdder();

    private final LongAdder requestsServed = new LongAdder();
    private final LongAdder totalResponseTimeMs = new LongAdder();
    private final AtomicLong maxResponseTimeMs = new AtomicLong(0);

    public void incrementJobsSubmitted() {
        jobsSubmitted.increment();
    }

    public void incrementJobsRunning() {
        jobsRunning.incrementAndGet();
    }

    public void decrementJobsRunning() {
        jobsRunning.decrementAndGet();
    }

    public void incrementJobsCompleted() {
        jobsCompleted.increment();
    }

    public void incrementJobsFailed() {
        jobsFailed.increment();
    }

    public void incrementJobsCancelled() {
        jobsCancelled.increment();
    }

    public void recordRequest(long durationMs) {
        requestsServed.increment();
        totalResponseTimeMs.add(durationMs);
        long currentMax = maxResponseTimeMs.get();
        if (durationMs > currentMax) {
            maxResponseTimeMs.compareAndSet(currentMax, durationMs);
        }
    }

    public long getUptimeMs() {
        return System.currentTimeMillis() - startTime;
    }

    public long getJobsSubmitted() {
        return jobsSubmitted.sum();
    }

    public long getJobsRunning() {
        return jobsRunning.get();
    }

    public long getJobsCompleted() {
        return jobsCompleted.sum();
    }

    public long getJobsFailed() {
        return jobsFailed.sum();
    }

    public long getJobsCancelled() {
        return jobsCancelled.sum();
    }

    public long getRequestsServed() {
        return requestsServed.sum();
    }

    public long getAverageResponseTimeMs() {
        long count = requestsServed.sum();
        return count == 0 ? 0 : totalResponseTimeMs.sum() / count;
    }

    public long getMaxResponseTimeMs() {
        return maxResponseTimeMs.get();
    }

    @org.springframework.context.event.EventListener
    public void onJobLifecycleEvent(com.sairaj.jobinfra.core.JobLifecycleEvent event) {
        switch (event.getEventType()) {
            case STARTED:
                incrementJobsRunning();
                break;
            case COMPLETED:
                decrementJobsRunning();
                incrementJobsCompleted();
                break;
            case FAILED:
                decrementJobsRunning();
                incrementJobsFailed();
                break;
        }
    }
}
