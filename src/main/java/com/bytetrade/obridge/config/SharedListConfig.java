package com.bytetrade.obridge.config;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SharedListConfig {
    @Bean
    public List<String> lockedBusinessList() {
        return new CopyOnWriteArrayList<>();
    }

    @Bean
    public Map<String, Boolean> initSwapEventMap() {
        return new ConcurrentHashMap<>();
    }
}
