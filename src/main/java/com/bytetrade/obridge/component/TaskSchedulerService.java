package com.bytetrade.obridge.component;

import org.quartz.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bytetrade.obridge.component.utils.RunnableJob;

import java.util.Date;

@Service
public class TaskSchedulerService {

    @Autowired
    private Scheduler scheduler;

    public void scheduleTask(Runnable task, long delayInMillis) throws SchedulerException {
        JobDataMap jobDataMap = new JobDataMap();
        jobDataMap.put(RunnableJob.RUNNABLE_KEY, task);

        JobDetail jobDetail = JobBuilder.newJob(RunnableJob.class)
                .usingJobData(jobDataMap)
                .build();

        Trigger trigger = TriggerBuilder.newTrigger()
                .startAt(new Date(System.currentTimeMillis() + delayInMillis))
                .build();

        scheduler.scheduleJob(jobDetail, trigger);
    }
}