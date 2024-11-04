package com.bytetrade.obridge;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.aspectj.lang.annotation.Aspect;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Aspect
@Component
@Configuration
public class SysThreadPoolExecutorAspect {
    private static final ExecutorService executorService = Executors.newVirtualThreadPerTaskExecutor();
    static {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down ExecutorService");
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(1, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
            }
        }));
    }

    static void submit(Callable<Object> task) {
        executorService.submit(task);
    }

    @Bean
    public ExecutorService exeService() {
        return executorService;
    }
}