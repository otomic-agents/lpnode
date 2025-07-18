package com.bytetrade.obridge.component.chain_events_loop;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;
import com.bytetrade.obridge.bean.EventTransferConfirmBox;
import com.bytetrade.obridge.bean.EventTransferInBox;
import com.bytetrade.obridge.bean.EventTransferOutBox;
import com.bytetrade.obridge.bean.EventTransferRefundBox;
import com.bytetrade.obridge.bean.SingleSwap.EventConfirmSwapBox;
import com.bytetrade.obridge.bean.SingleSwap.EventInitSwapBox;
import com.bytetrade.obridge.bean.SingleSwap.EventRefundSwapBox;
import com.bytetrade.obridge.component.client_service.ClientService;
import com.bytetrade.obridge.component.controller.AtomicLPController;
import com.bytetrade.obridge.component.controller.SingleSwapLpController;
import com.bytetrade.obridge.db.redis.RedisLocalDb;
import com.bytetrade.obridge.utils.JsonUtils;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import javax.annotation.PostConstruct;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import javax.annotation.PreDestroy;

@Slf4j
@Component
public class EventProcessor {
    private static final String CURSOR_KEY = "event_cursor";

    @Autowired
    private SingleSwapLpController singleSwapLpController;

    @Autowired
    private AtomicLPController atomicLPController;

    @Autowired
    private ClientService clientService;

    @Autowired
    private RedisLocalDb redisLocalDb;

    private List<String> baseUrls;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ExecutorService exePoolService;

    private long lastCursorLogTime = 0;

    @Data
    private class CursorInfo {
        private String baseUrl;
        private String cursorKey;

        public CursorInfo(String baseUrl, String cursorKey) {
            this.baseUrl = baseUrl;
            this.cursorKey = cursorKey;
            log.debug("Initialized cursor info with baseUrl: {} and cursorKey: {}", baseUrl, cursorKey);
        }

        public EventCursor getCurrentCursor() {
            String cursorStr = (String) redisLocalDb.get().opsForValue().get(cursorKey);
            return !StringUtils.hasLength(cursorStr) ? null
                    : JsonUtils.parseObject(cursorStr, EventCursor.class);
        }

        public void updateCursor(EventData eventData) {
            EventCursor cursor = new EventCursor(eventData.getBlockNumber(), eventData.getTransactionIndex());
            redisLocalDb.get().opsForValue().set(cursorKey, JsonUtils.toJsonString(cursor));
        }
    }

    private List<CursorInfo> cursors;

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    static class EventCursor {
        private Long blockNumber;
        private Integer transactionIndex;
    }

    @Data
    static class EventResponse {
        private Integer code;
        private EventData data;
    }

    @Data
    @JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
    static class EventData {
        private String _id;
        private Long blockNumber;
        private String transactionHash;
        private Integer transactionIndex;
        private String eventName;
        private String transferId;
        private JsonNode parsedData;
        private String transferInfo;
        private String eventRaw;
        private JsonNode eventParse;
        private String chainId;
        private String taskId;
        private String createdAt;
    }

    private final AtomicBoolean running = new AtomicBoolean(true);

    @PostConstruct
    public void init() {
        baseUrls = clientService.getAllUrls();
        if (baseUrls == null || baseUrls.isEmpty()) {
            log.error("No base URLs found from ClientService. Please check chain service configuration.");
            throw new IllegalStateException("No base URLs configured");
        }

        log.info("Initialized EventProcessor with {} base URLs: {}", baseUrls.size(), baseUrls);

        cursors = baseUrls.stream()
                .map(url -> new CursorInfo(url, CURSOR_KEY + "_" + url.hashCode()))
                .collect(Collectors.toList());

        cursors.forEach(cursorInfo -> {
            exePoolService.submit(() -> processEventsForTarget(cursorInfo));
        });
    }

    private void processEventsForTarget(CursorInfo cursorInfo) {
        while (running.get()) {
            try {
                processNextEventForTarget(cursorInfo);
                long currentTime = System.currentTimeMillis();
                if (currentTime - lastCursorLogTime > 60000) {
                    EventCursor cursor = cursorInfo.getCurrentCursor();
                    if (cursor != null) {
                        log.info("🎯 Current cursor position for {}- Block: {}, TxIndex: {}",
                                cursorInfo.getBaseUrl(), cursor.getBlockNumber(), cursor.getTransactionIndex());
                    }
                    lastCursorLogTime = currentTime;
                }

                Thread.sleep(500);
            } catch (Exception e) {
                log.error("❌ Error processing event for " + cursorInfo.getBaseUrl(), e);
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException ignored) {
                }
            }
        }
    }

    private void processNextEventForTarget(CursorInfo cursorInfo) {
        EventCursor cursor = cursorInfo.getCurrentCursor();
        ResponseEntity<EventResponse> response;
    
        try {
            // Check if the latest event needs to be fetched
            boolean needLatestEvent = cursor == null || 
                                    cursor.getBlockNumber() == null || 
                                    cursor.getTransactionIndex() == null;
    
            if (needLatestEvent) {
                // Fetch the latest event
                response = getLatestEvent(cursorInfo.baseUrl);
                log.info("🔍 Fetching latest event for {}", cursorInfo.baseUrl);
    
                // Validate the response
                EventData latestEventData = validateAndGetEventData(response);
                if (latestEventData == null || 
                    latestEventData.getBlockNumber() == null || 
                    latestEventData.getTransactionIndex() == null) {
                    log.warn("⚠️ Latest event data is incomplete from {}. Will retry getting latest event.", 
                            cursorInfo.baseUrl);
                    return; // Return directly, the next loop will continue to try fetching the latest event
                }
    
                // Process the event and update the cursor
                handleEventAndUpdateCursor(latestEventData, cursorInfo);
            } else {
                // If a valid cursor exists, fetch the next event
                response = getNextEvent(cursorInfo.baseUrl, cursor);
                EventData nextEventData = validateAndGetEventData(response);
                
                if (nextEventData != null) {
                    handleEventAndUpdateCursor(nextEventData, cursorInfo);
                }
            }
    
        } catch (Exception e) {
            log.error("💥 Error in event processing loop for {} with cursor {}: {}",
                    cursorInfo.baseUrl,
                    cursor != null ? JsonUtils.toJsonString(cursor) : "null",
                    e.getMessage());
            // If an error occurs during processing, do not update the cursor
            throw e;
        }
    }
    
    // Validate the response and get event data
    private EventData validateAndGetEventData(ResponseEntity<EventResponse> response) {
        if (response == null) {
            log.warn("⚠️ Response is null");
            return null;
        }
    
        EventResponse body = response.getBody();
        if (body == null) {
            log.warn("⚠️ Response body is null. Response status: {}", response.getStatusCode());
            return null;
        }
    
        EventData eventData = body.getData();
        if (eventData == null) {
            // log.warn("⚠️ Event data is null");
            return null;
        }
    
        // Validate key fields
        if (eventData.getBlockNumber() == null || eventData.getTransactionIndex() == null) {
            log.warn("⚠️ Event data is incomplete: blockNumber={}, transactionIndex={}", 
                    eventData.getBlockNumber(), 
                    eventData.getTransactionIndex());
            return null;
        }
    
        return eventData;
    }
    
    // Process the event and update the cursor
    private void handleEventAndUpdateCursor(EventData eventData, CursorInfo cursorInfo) {
        try {
            // Process the event
            handleEvent(eventData);
            
            // Update the cursor after successful processing
            cursorInfo.updateCursor(eventData);
            
            log.info("✅ Successfully processed event and updated cursor for {} at block {} tx {}",
                    cursorInfo.getBaseUrl(),
                    eventData.getBlockNumber(),
                    eventData.getTransactionIndex());
        } catch (Exception e) {
            log.error("💥 Error handling event for {} at block {} tx {}: {}",
                    cursorInfo.getBaseUrl(),
                    eventData.getBlockNumber(),
                    eventData.getTransactionIndex(),
                    e.getMessage(),
                    e);
            throw e;
        }
    }

    @PreDestroy
    public void shutdown() {
        log.info("🔄 Initiating graceful shutdown of event processor...");
        running.set(false);
        try {
            // Allow time for in-progress tasks to complete
            Thread.sleep(1000);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        log.info("✅ Event processor shutdown complete");
    }

    private ResponseEntity<EventResponse> getLatestEvent(String baseUrl) {
        String url = baseUrl + "/producer/getLatestEvent";
        return restTemplate.getForEntity(url, EventResponse.class);
    }

    private ResponseEntity<EventResponse> getNextEvent(String baseUrl, EventCursor cursor) {
        String url = baseUrl + "/producer/getNextEvent?blockNumber=" + cursor.getBlockNumber() +
                "&transactionIndex=" + cursor.getTransactionIndex();
        return restTemplate.getForEntity(url, EventResponse.class);
    }

    private void handleEvent(EventData eventData) {
        try {
            log.info("🔔 Processing event: {} [Block: {}, TxHash: {}]",
                    eventData.getEventName(),
                    eventData.getBlockNumber(),
                    eventData.getTransactionHash());

            switch (eventData.getEventName()) {
                case "LogNewTransferOut" -> {
                    log.info("📤 Processing Transfer Out Event [TxHash: {}]", eventData.getTransactionHash());
                    log.info("Original input data: {}", JsonUtils.toJsonString(eventData));

                    // eventData.setEventParse(eventData.parsedData);
                    String jsonString = JsonUtils.convertToSnakeCase(JsonUtils.toJsonString(eventData));
                    EventTransferOutBox event = JsonUtils.parseObject(jsonString, EventTransferOutBox.class);

                    log.info("Processed event data: chainId={}, transferInfo={}",
                            event.getChainId(),
                            JsonUtils.toJsonString(event.getTransferInfo()));
                    event.getEventParse().setTransferInfo(event.getTransferInfo());
                    atomicLPController.onEventTransferOut(event);
                }
                case "LogTransferOutConfirmed" -> {
                    log.info("✅ Processing Transfer Out Confirm Event [TxHash: {}]", eventData.getTransactionHash());
                    log.info("Original input data: {}", JsonUtils.toJsonString(eventData));

                    // eventData.setEventParse(eventData.parsedData);
                    String jsonString = JsonUtils.convertToSnakeCase(JsonUtils.toJsonString(eventData));
                    EventTransferConfirmBox event = JsonUtils.parseObject(jsonString, EventTransferConfirmBox.class);

                    log.info("Processed event data: chainId={}, transferInfo={}",
                            event.getChainId(),
                            JsonUtils.toJsonString(event.getTransferInfo()));
                    event.getEventParse().setTransferInfo(event.getTransferInfo());
                    atomicLPController.onConfirm(event);
                }
                case "LogTransferOutRefunded" -> {
                    log.info("↩️ Processing Transfer Out Refund Event [TxHash: {}]", eventData.getTransactionHash());
                    log.info("Original input data: {}", JsonUtils.toJsonString(eventData));

                    // eventData.setEventParse(eventData.parsedData);
                    String jsonString = JsonUtils.convertToSnakeCase(JsonUtils.toJsonString(eventData));
                    EventTransferRefundBox event = JsonUtils.parseObject(jsonString, EventTransferRefundBox.class);

                    log.info("Processed event data: chainId={}, transferInfo={}",
                            event.getChainId(),
                            JsonUtils.toJsonString(event.getTransferInfo()));
                    event.getEventParse().setTransferInfo(event.getTransferInfo());
                    atomicLPController.onEventRefund(event);
                }
                case "LogNewTransferIn" -> {
                    log.info("📥 Processing Transfer In Event [TxHash: {}]", eventData.getTransactionHash());
                    log.info("Original input data: {}", JsonUtils.toJsonString(eventData));

                    // eventData.setEventParse(eventData.parsedData);
                    String jsonString = JsonUtils.convertToSnakeCase(JsonUtils.toJsonString(eventData));
                    EventTransferInBox event = JsonUtils.parseObject(jsonString, EventTransferInBox.class);
                
                    log.info("Processed event data: chainId={}, transferInfo={}",
                            event.getChainId(),
                            JsonUtils.toJsonString(event.getTransferInfo()));
                    event.getEventParse().setTransferInfo(event.getTransferInfo());    
                    atomicLPController.onEventTransferIn(event);
                }
                case "LogTransferInConfirmed" -> {
                    log.info("✅ Processing Transfer In Confirm Event [TxHash: {}]", eventData.getTransactionHash());
                    log.info("Original input data: {}", JsonUtils.toJsonString(eventData));

                    // eventData.setEventParse(eventData.parsedData);
                    String jsonString = JsonUtils.convertToSnakeCase(JsonUtils.toJsonString(eventData));
                    EventTransferConfirmBox event = JsonUtils.parseObject(jsonString, EventTransferConfirmBox.class);

                    log.info("Processed event data: chainId={}, transferInfo={}",
                            event.getChainId(),
                            JsonUtils.toJsonString(event.getTransferInfo()));
                    event.getEventParse().setTransferInfo(event.getTransferInfo());    
                    atomicLPController.onConfirm(event);
                }
                case "LogTransferInRefunded" -> {
                    log.info("↩️ Processing Transfer In Refund Event [TxHash: {}]", eventData.getTransactionHash());
                    log.info("Original input data: {}", JsonUtils.toJsonString(eventData));

                    // eventData.setEventParse(eventData.parsedData);
                    String jsonString = JsonUtils.convertToSnakeCase(JsonUtils.toJsonString(eventData));
                    EventTransferRefundBox event = JsonUtils.parseObject(jsonString, EventTransferRefundBox.class);

                    log.info("Processed event data: chainId={}, transferInfo={}",
                            event.getChainId(),
                            JsonUtils.toJsonString(event.getTransferInfo()));
                    event.getEventParse().setTransferInfo(event.getTransferInfo());    
                    atomicLPController.onEventRefund(event);
                }
                case "LogInitSwap" -> {
                    log.info("🔄 Processing Init Swap Event [TxHash: {}]", eventData.getTransactionHash());
                    log.info("Original input data: {}", JsonUtils.toJsonString(eventData));

                    // eventData.setEventParse(eventData.parsedData);
                    String jsonString = JsonUtils.convertToSnakeCase(JsonUtils.toJsonString(eventData));
                    EventInitSwapBox event = JsonUtils.parseObject(jsonString, EventInitSwapBox.class);

                    log.info("Processed event data: chainId={}, transferInfo={}",
                            event.getChainId(),
                            JsonUtils.toJsonString(event.getTransferInfo()));
                    event.getEventParse().setTransferInfo(event.getTransferInfo());    
                    singleSwapLpController.onEventInitSwap(event);
                }
                case "LogSwapConfirmed" -> {
                    log.info("✅ Processing Confirm Swap Event [TxHash: {}]", eventData.getTransactionHash());
                    log.info("Original input data: {}", JsonUtils.toJsonString(eventData));

                    // eventData.setEventParse(eventData.parsedData);
                    String jsonString = JsonUtils.convertToSnakeCase(JsonUtils.toJsonString(eventData));
                    EventConfirmSwapBox event = JsonUtils.parseObject(jsonString, EventConfirmSwapBox.class);

                    log.info("Processed event data: chainId={}, transferInfo={}",
                            event.getChainId(),
                            JsonUtils.toJsonString(event.getTransferInfo()));
                    event.getEventParse().setTransferInfo(event.getTransferInfo());    
                    singleSwapLpController.onEventConfirmSwap(event);
                }
                case "LogSwapRefunded" -> {
                    log.info("↩️ Processing Refund Swap Event [TxHash: {}]", eventData.getTransactionHash());
                    log.info("Original input data: {}", JsonUtils.toJsonString(eventData));

                    // eventData.setEventParse(eventData.parsedData);
                    String jsonString = JsonUtils.convertToSnakeCase(JsonUtils.toJsonString(eventData));
                    EventRefundSwapBox event = JsonUtils.parseObject(jsonString, EventRefundSwapBox.class);

                    log.info("Processed event data: chainId={}, transferInfo={}",
                            event.getChainId(),
                            JsonUtils.toJsonString(event.getTransferInfo()));
                    event.getEventParse().setTransferInfo(event.getTransferInfo());    
                    singleSwapLpController.onEventRefundSwap(event);
                }
                default -> log.warn("⚠️ Unknown event type: {}", eventData.getEventName());
            }
            log.info("✨ Successfully processed event: {} [TxHash: {}]",
                    eventData.getEventName(),
                    eventData.getTransactionHash());

        } catch (Exception e) {
            log.error("💥 Error handling event: {} [TxHash: {}]",
                    eventData.getEventName(),
                    eventData.getTransactionHash(),
                    e);
        }
    }

}
