package com.sairaj.jobinfra.queue;
import com.sairaj.jobinfra.core.Job;

public interface JobQueue{
    void enqueue(Job job);
    Job dequeue() throws InterruptedException;
    int size();
}
