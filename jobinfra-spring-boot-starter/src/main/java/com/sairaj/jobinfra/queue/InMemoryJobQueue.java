package com.sairaj.jobinfra.queue;
import com.sairaj.jobinfra.core.Job;
import com.sairaj.jobinfra.core.JobStatus;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class InMemoryJobQueue implements JobQueue{
    private final BlockingQueue<Job> queue = new LinkedBlockingQueue<>();

    public void enqueue(Job job){
        job.setStatus(JobStatus.QUEUED);
        queue.offer(job);
    }
    public Job dequeue() throws InterruptedException{
        return queue.take();
    }
    public int size() {
        return queue.size();
    }
}