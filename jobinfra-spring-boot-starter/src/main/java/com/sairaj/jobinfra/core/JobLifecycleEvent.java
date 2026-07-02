package com.sairaj.jobinfra.core;

import org.springframework.context.ApplicationEvent;

public class JobLifecycleEvent extends ApplicationEvent {
    
    public enum EventType {
        STARTED, COMPLETED, FAILED
    }

    private final Job job;
    private final EventType eventType;

    public JobLifecycleEvent(Object source, Job job, EventType eventType) {
        super(source);
        this.job = job;
        this.eventType = eventType;
    }

    public Job getJob() {
        return job;
    }

    public EventType getEventType() {
        return eventType;
    }
}
