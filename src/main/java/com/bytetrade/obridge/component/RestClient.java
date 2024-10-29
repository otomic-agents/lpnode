package com.bytetrade.obridge.component;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import com.bytetrade.obridge.bean.BusinessFullData;
import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.QuoteBase;
import com.bytetrade.obridge.bean.RealtimeQuote;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;
import reactor.netty.http.client.HttpClientRequest;

import org.springframework.web.reactive.function.client.WebClient;

@Slf4j
@Component
public class RestClient {
    private final WebClient webClient;

    public RestClient(WebClient webClient) {
        this.webClient = webClient;
    }

    @Autowired
    RestTemplate restTemplate;

    @Retryable(value = {
            Exception.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
    public String doNotifyTransferInRefund(LPBridge lpBridge, BusinessFullData bfd) {
        return restTemplate.postForObject(
                lpBridge.getRelayUri() + "/relay" + "/lpnode/" + lpBridge.getRelayApiKey()
                        + "/on_transfer_in_refund",
                bfd, String.class);
    }

    @Retryable(value = {
            Exception.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
    public String doNotifyTransferInConfirm(LPBridge lpBridge, BusinessFullData bfd) {
        return restTemplate.postForObject(
                lpBridge.getRelayUri() + "/relay" + "/lpnode/" + lpBridge.getRelayApiKey()
                        + "/on_transfer_in_confirm",
                bfd, String.class);
    }

    @Retryable(value = {
            Exception.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
    public String doNotifyTransferIn(LPBridge lpBridge, BusinessFullData bfd) {
        String uri = lpBridge.getRelayUri() + "/relay" + "/lpnode/" + lpBridge.getRelayApiKey()
                + "/on_transfer_in";
        log.info("request: " + uri);
        return restTemplate.postForObject(
                uri,
                bfd, String.class);
    }

    @Retryable(value = {
            Exception.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
    public String doNotifyRealtimeQuote(RealtimeQuote realtimeQuote, LPBridge lpBridge) {
        String requestUrl = lpBridge.getRelayUri() + "/relay" + "/lpnode/" + lpBridge.getRelayApiKey()
                + "/realtime_quote";
        log.info(" ..,urlIs:{}", requestUrl);
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
