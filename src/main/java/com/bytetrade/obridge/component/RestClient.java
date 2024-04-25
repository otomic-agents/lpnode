package com.bytetrade.obridge.component;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;  
import com.bytetrade.obridge.bean.BusinessFullData;
import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.QuoteBase;
import com.bytetrade.obridge.bean.RealtimeQuote;
import org.apache.http.impl.client.HttpClientBuilder;  
import org.apache.http.impl.client.HttpClients;
import org.apache.http.client.config.RequestConfig;  
import org.apache.http.impl.client.DefaultHttpRequestRetryHandler;  
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class RestClient {
    
    @Autowired
    RestTemplate restTemplate;

    @Value("${relay.uri}")
	private String relayUri;
    private RestTemplate createRestTemplateWithTimeout(int timeout) {  
        HttpClientBuilder httpClientBuilder = HttpClients.custom();  
        httpClientBuilder.setRetryHandler(new DefaultHttpRequestRetryHandler(0,false));
        org.apache.http.client.HttpClient httpClient = httpClientBuilder.setDefaultRequestConfig(  
                RequestConfig.custom()  
                        .setConnectTimeout(timeout)  
                        .setConnectionRequestTimeout(timeout)  
                        .setSocketTimeout(timeout)  
                        .build()  
        ).build();  
      
        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(httpClient);  
        return new RestTemplate(requestFactory);  
    }  
  

    @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
    public String doNotifyTransferInRefund(LPBridge lpBridge, BusinessFullData bfd) {
        return restTemplate.postForObject(
            relayUri + "/lpnode/" + lpBridge.getRelayApiKey() + "/on_transfer_in_refund", 
            bfd, String.class);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
    public String doNotifyTransferInConfirm(LPBridge lpBridge, BusinessFullData bfd) {
        return restTemplate.postForObject(
            relayUri + "/lpnode/" + lpBridge.getRelayApiKey() + "/on_transfer_in_confirm", 
            bfd, String.class);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
    public String doNotifyTransferIn(LPBridge lpBridge, BusinessFullData bfd) {
        return restTemplate.postForObject(
            relayUri + "/lpnode/" + lpBridge.getRelayApiKey() + "/on_transfer_in", 
            bfd, String.class);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
    public String doNotifyRealtimeQuote(RealtimeQuote realtimeQuote, LPBridge lpBridge) {
        return restTemplate.postForObject(
            relayUri + "/lpnode/" + lpBridge.getRelayApiKey() + "/realtime_quote", 
            realtimeQuote, String.class);
    }

    @Retryable(value = {Exception.class}, maxAttempts = 3, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
    public String doNotifyBridgeLive(List<QuoteBase> quotes, LPBridge lpBridge) {
        RestTemplate timedRestTemplate = createRestTemplateWithTimeout(6000); 
        return timedRestTemplate.postForObject(
            relayUri + "/lpnode/" + lpBridge.getRelayApiKey() + "/quote_and_live_confirmation", 
            quotes, String.class);
    }
}
