package com.bytetrade.obridge.component;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bytetrade.obridge.bean.AtomicBusinessFullData;
import com.bytetrade.obridge.bean.LPBridge;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AtomicRestClient {

	@Autowired
	RestTemplate restTemplate;

	@Retryable(value = {
			Exception.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
	public String doNotifyTransferInRefund(LPBridge lpBridge, AtomicBusinessFullData bfd) {
		return restTemplate.postForObject(
				lpBridge.getRelayUri() + "/relay" + "/lpnode/" + lpBridge.getRelayApiKey()
						+ "/on_transfer_in_refund",
				bfd, String.class);
	}

	@Retryable(value = {
			Exception.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
	public String doNotifyTransferInConfirm(LPBridge lpBridge, AtomicBusinessFullData bfd) {
		return restTemplate.postForObject(
				lpBridge.getRelayUri() + "/relay" + "/lpnode/" + lpBridge.getRelayApiKey()
						+ "/on_transfer_in_confirm",
				bfd, String.class);
	}

	@Retryable(value = {
			Exception.class }, maxAttempts = 10, backoff = @Backoff(delay = 1000, maxDelay = 6000, multiplier = 2))
	public String doNotifyTransferIn(LPBridge lpBridge, AtomicBusinessFullData bfd) {
		String uri = lpBridge.getRelayUri() + "/relay" + "/lpnode/" + lpBridge.getRelayApiKey() + "/on_transfer_in";
		log.info("request: " + uri);

		try {
			ObjectMapper objectMapper = new ObjectMapper();
			String jsonString = objectMapper.writeValueAsString(bfd);
			System.out.println("Serialized JSON:");
			System.out.println(jsonString);

			AtomicBusinessFullData copiedData = objectMapper.readValue(jsonString, AtomicBusinessFullData.class);
			copiedData.getEventTransferIn().setHashLock("");
			copiedData.getEventTransferOut().setHashLock("");
			ObjectMapper objectMapperLog = new ObjectMapper();
			objectMapperLog.enable(SerializationFeature.INDENT_OUTPUT);
			String formattedJson = objectMapperLog.writeValueAsString(copiedData);
			log.info("🍨 request body:\n{}", formattedJson);
		} catch (Exception e) {
			log.error("Failed to format object for logging", e);
		}
		return restTemplate.postForObject(uri, bfd, String.class);
	}
}
