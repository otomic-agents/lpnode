package com.bytetrade.obridge;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableAsync
@EnableScheduling
@SpringBootApplication
@EnableRetry
public class ObridgeApplication {

	public static void main(String[] args) {
		System.setProperty("sun.naming.dns", "127.0.0.1");
		SpringApplication.run(ObridgeApplication.class, args);

	}
}
