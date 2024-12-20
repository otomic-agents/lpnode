package com.bytetrade.obridge.component.client.request;

import java.security.SignatureException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bytetrade.obridge.component.client.response.ResponseSignMessage;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SingleChainSignMessage extends AbstractSignMessage {
    @Autowired
    private RestTemplate restTemplate;

    @Override
    public String generateSign(String baseUri, Integer chainId) {
        if (!(signData instanceof SingleSignData)) {
            throw new IllegalArgumentException("Invalid signData type for SingleSignData");
        }

        try {
            String uri = baseUri + getSignMessageSubPath(chainId);
            log.info("request url:{}", uri);
            ObjectMapper mapper = new ObjectMapper()
                    .setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
                    .enable(SerializationFeature.INDENT_OUTPUT);
            log.info("Serialized JSON:\n{}", mapper.writeValueAsString(this));

            long signStartTime = System.nanoTime();

            ResponseSignMessage response = restTemplate.postForObject(
                    uri,
                    this,
                    ResponseSignMessage.class);

            double signDurationInMilliseconds = (System.nanoTime() - signStartTime) / 1_000_000.0;
            log.info("Sign execution time: {} ms", signDurationInMilliseconds);

            if (response == null || response.getSigned() == null) {
                log.error("Failed to get signature: response is null or signature is missing");
                throw new SignatureException("Failed to generate signature");
            }

            log.info("Response message: {}", response);
            log.info("The signature of lp has been completed, signData: {}", response.getSigned());

            return response.getSigned();

        } catch (Exception e) {
            log.error("Error generating atomic signature", e);
            throw new RuntimeException("Failed to generate atomic signature", e);
        }
    }

    private String getSignMessageSubPath(Integer chainId) {
        switch (chainId) {
            case 9006:
            case 9000:
            case 60:
            case 614:
            case 966:
            case 397:
                return "/single_chain/sign_message_712";
            case 501:
                return "/single_chain/sign_message";
            default:
                throw new IllegalArgumentException("Invalid chainId: " + chainId);
        }
    }
}
