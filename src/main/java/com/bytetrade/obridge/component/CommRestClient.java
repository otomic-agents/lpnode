package com.bytetrade.obridge.component;

import java.time.Duration;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.reactive.function.client.WebClient;

import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.QuoteBase;
import com.bytetrade.obridge.bean.RealtimeQuote;

import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientRequest;

@Slf4j
@Component
public class CommRestClient {
    private final WebClient webClient;

    @Autowired
    RestTemplate restTemplate;

    public CommRestClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Retryable(value = {
            Exception.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
    public String doNotifyRealtimeQuote(RealtimeQuote realtimeQuote, LPBridge lpBridge) {
        String requestUrl = lpBridge.getRelayUri() + "/relay" + "/lpnode/" + lpBridge.getRelayApiKey()
                + "/realtime_quote";
        log.info("Sending real-time price to relay, url is: {}", requestUrl);
        Mono<String> result = webClient.post()
                .uri(requestUrl)
                .httpRequest(httpRequest -> {
                    HttpClientRequest reactorRequest = httpRequest.getNativeRequest();
                    reactorRequest.responseTimeout(Duration.ofSeconds(2));
                })
                .body(Mono.just(realtimeQuote), RealtimeQuote.class)
                .retrieve()
                .onStatus(e -> e.isError(), resp -> {
                    System.out.println("request error: " + resp.statusCode().value() + " "
                            + resp.statusCode());
                    return Mono.error(new RuntimeException("request error"));
                })
                .bodyToMono(String.class);
        String resultText = result.block();
        log.info("realtime_quote response:{} ", resultText);
        return resultText;
    }

    public String doNotifyBridgeLive(List<QuoteBase> quotes, LPBridge lpBridge) {
        String notifyUrl = lpBridge.getRelayUri() + "/relay" + "/lpnode/" + lpBridge.getRelayApiKey()
                + "/quote_and_live_confirmation";
        // log.info("send quote to relay , url is :{}", notifyUrl);
        String url = notifyUrl;
        Mono<String> result = webClient.post()
                .uri(url)
                .httpRequest(httpRequest -> {
                    HttpClientRequest reactorRequest = httpRequest.getNativeRequest();
                    reactorRequest.responseTimeout(Duration.ofSeconds(2));
                })
                .body(Mono.just(quotes), QuoteBase.class)
                .retrieve()
                .onStatus(e -> e.isError(), resp -> {
                    System.out.println("request error: " + resp.statusCode().value() + " "
                            + resp.statusCode());
                    return Mono.error(new RuntimeException("request error"));
                })
                .bodyToMono(String.class);
        String resultText = result.block();
        return resultText;
    }
}
