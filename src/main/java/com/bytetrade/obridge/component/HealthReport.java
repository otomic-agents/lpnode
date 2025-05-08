package com.bytetrade.obridge.component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import com.alibaba.fastjson.JSONObject;
import javax.annotation.PostConstruct;
import java.util.concurrent.ExecutorService;
import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Autowired;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class HealthReport {
    private static final int INTERVAL_TIME = Integer.parseInt(System.getenv().getOrDefault(
        "HEALTH_INTERVAL_TIME", "60000")); 
    // Get METRICS_ENDPOINT from environment variable with a default value
    private static final String METRICS_ENDPOINT = System.getenv().getOrDefault(
            "METRICS_ENDPOINT",
            "");

    // Get INSTANCE_NAME from environment variable with a default value
    private static final String INSTANCE_NAME = System.getenv().getOrDefault(
            "INSTANCE_NAME",
            "");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();
    @Autowired
    private ExecutorService exePoolService;

    @PostConstruct
    public void init() {
        if (METRICS_ENDPOINT.isEmpty()) {
            log.info("METRICS_ENDPOINT environment variable is not set or empty. Health reporting is disabled.");
            return;
        } else {
            log.info("Health reporting enabled. METRICS_ENDPOINT set to: {}", METRICS_ENDPOINT);
            log.info("Using instance name: {}", INSTANCE_NAME);
        }
        if (INSTANCE_NAME.isEmpty()) {
            log.warn(
                    "INSTANCE_NAME environment variable is not set or empty. Health reporting will not function properly.");
            return;
        }
        exePoolService.submit(this::heartbeatReport);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            log.info("Shutting down ChainMessageChannelListener");
        }));
    }

    private void heartbeatReport() {
        while (true) {
            try {
                log.info("Heartbeat report executed");
                JSONObject metric = new JSONObject();

                // Set necessary metric fields
                metric.put("name", "lpnode:keepalive:time:status");
                metric.put("type", "counter");
                metric.put("timestamp", Instant.now().toString());

                JSONObject labels = new JSONObject();
                labels.put("time_type", "last_update");
                labels.put("instance", INSTANCE_NAME);
                metric.put("labels", labels);
                metric.put("value", System.currentTimeMillis());

                // Send metric to specified address
                try {
                    boolean success = sendMetric(metric);
                    if (success) {
                        log.info("Successfully sent metric to metrics-hub-service");
                    } else {
                        log.warn("Failed to send metric to metrics-hub-service");
                    }
                } catch (Exception e) {
                    // Catch all possible exceptions to ensure the loop continues
                    log.error("Exception in sendMetric, but continuing loop", e);
                }

                Thread.sleep(INTERVAL_TIME);
            } catch (InterruptedException e) {
                log.error("Heartbeat report thread interrupted", e);
                Thread.currentThread().interrupt();
                break; // Only exit the loop when the thread is interrupted
            } catch (Exception e) {
                // Catch all other exceptions but don't exit the loop
                log.error("Error in heartbeat report, continuing anyway", e);
                try {
                    // Add additional delay to prevent too frequent retries in error conditions
                    Thread.sleep(3000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
    }

    private boolean sendMetric(JSONObject metric) {
        log.info("sendMetric");
        try {
            // Verify METRICS_ENDPOINT is not empty
            if (METRICS_ENDPOINT == null || METRICS_ENDPOINT.trim().isEmpty()) {
                log.error("METRICS_ENDPOINT is null or empty");
                return false;
            }

            String jsonBody = metric.toJSONString();
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(METRICS_ENDPOINT))
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
                    .timeout(Duration.ofSeconds(3))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() >= 200 && response.statusCode() < 300) {
                return true;
            } else {
                log.error("Failed to send metric, status code: {}, response: {}",
                        response.statusCode(), response.body());
                return false;
            }
        } catch (IOException | InterruptedException e) {
            log.error("Exception while sending metric", e);
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            return false;
        } catch (Exception e) {
            // Catch all other possible exceptions, including address resolution errors
            log.error("Unexpected error while sending metric", e);
            return false;
        }
    }
}
