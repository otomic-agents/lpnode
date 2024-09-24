package com.bytetrade.obridge.component;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration

public class WebConfiguration {

    @Bean

    public RestTemplate restTemplate() {

        RestTemplate restTemplate = new RestTemplate();
        return restTemplate;
    }
    @Bean
    public WebClient webClient() {
        return WebClient.builder()
                .build();
    }    
}
