package com.bytetrade.obridge.component.client.request;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

@Component
public class SignMessageFactory {

    @Autowired
    private ApplicationContext applicationContext;

    public AbstractSignMessage createSignMessage(String swapType) {
        if ("SINGLECHAIN".equalsIgnoreCase(swapType)) {
            return applicationContext.getBean(SingleChainSignMessage.class);
        } else if ("ATOMIC".equalsIgnoreCase(swapType)) {
            return applicationContext.getBean(AtomicSignMessage.class);
        } else {
            throw new IllegalArgumentException("Unsupported swapType: " + swapType);
        }
    }
}