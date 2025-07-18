package com.bytetrade.obridge.config;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SharedListConfig {
    @Bean
    public Map<String, Boolean> initSwapEventMap() {
        return new ConcurrentHashMap<>();
    }
}
