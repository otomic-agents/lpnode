package com.bytetrade.obridge.component;

import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.quartz.JobExecutionException;

public class RunnableJob implements Job {
    public static final String RUNNABLE_KEY = "runnable";

    @Override
    public void execute(JobExecutionContext context) throws JobExecutionException {
        Runnable task = (Runnable) context.getJobDetail().getJobDataMap().get(RUNNABLE_KEY);
        if (task != null) {
            task.run();
        }
    }
}
