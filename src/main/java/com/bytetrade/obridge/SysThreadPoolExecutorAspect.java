package com.bytetrade.obridge;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.ThreadPoolExecutor;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Configuration
public class SysThreadPoolExecutorAspect {
    private static final int CORE_POOL_SIZE = 5;
    private static final int MAX_POOL_SIZE = 30;
    private static final long KEEP_ALIVE_TIME = 60L;

    private static final TimeUnit TIME_UNIT = TimeUnit.SECONDS;
    private static final int QUEUE_CAPACITY = 20;

    private static ExecutorService executorService;

    static {
        executorService = new ThreadPoolExecutor(
                CORE_POOL_SIZE,
                MAX_POOL_SIZE,
                KEEP_ALIVE_TIME,
                TIME_UNIT,
                new LinkedBlockingQueue<>(QUEUE_CAPACITY));
    }

    @Bean
    public ExecutorService exeService() {
        return executorService;
    }
}
