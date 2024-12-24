package com.bytetrade.obridge.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.SingleSwap.SingleSwapBusinessFullData;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SingleSwapRestClient {
    @Autowired
    RestTemplate restTemplate;

    public String NotifyConfirmSwap(LPBridge lpBridge, SingleSwapBusinessFullData bfd) {
        return restTemplate.postForObject(
                lpBridge.getRelayUri() + "/relay" + "/lpnode/single_swap/" + lpBridge.getRelayApiKey()
                        + "/on_confirm_swap",
                bfd, String.class);
    }
}
