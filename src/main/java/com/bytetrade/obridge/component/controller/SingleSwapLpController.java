package com.bytetrade.obridge.component.controller;

import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bytetrade.obridge.bean.CmdEvent;
import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.SingleSwap.EventConfirmSwap;
import com.bytetrade.obridge.bean.SingleSwap.EventConfirmSwapBox;
import com.bytetrade.obridge.bean.SingleSwap.EventInitSwapBox;
import com.bytetrade.obridge.bean.SingleSwap.EventRefundSwapBox;
import com.bytetrade.obridge.bean.SingleSwap.ExtendedSingleSwapAsset;
import com.bytetrade.obridge.bean.SingleSwap.SingleSwapBusinessFullData;
import com.bytetrade.obridge.component.ChainSetting;
import com.bytetrade.obridge.component.HealthReport;
import com.bytetrade.obridge.component.client.SingleSwapRestClient;
import com.bytetrade.obridge.component.client.request.CommandConfirmSwap;
import com.bytetrade.obridge.component.client.request.Gas;
import com.bytetrade.obridge.component.client.request.RequestDoConfirmSwap;
import com.bytetrade.obridge.component.service.InitSwapEventService;
import com.bytetrade.obridge.db.SwapOrder;
import com.bytetrade.obridge.db.redis.RedisConfig;
import com.bytetrade.obridge.utils.JsonUtils;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class SingleSwapLpController extends LpControllerBase {
    private static final String KEY_BUSINESS_CACHE = "KEY_BUSINESS_CACHE";
    private static final String KEY_LOCKED_BUSINESS = "KEY_LOCKED_BUSINESS";
    @Autowired
    private ExecutorService exePoolService;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Resource
    RedisConfig redisConfig;

    @Autowired
    SingleSwapRestClient singleSwapRestClient;

    @Autowired
    InitSwapEventService initSwapEventService;

    @Autowired
    HealthReport healthReport;

    public Boolean onRelayInitSwap(SingleSwapBusinessFullData bfd) {
        exePoolService.submit(() -> {
            log.info("🚀onRelayInitSwap The subsequent actions begin to execute. [{}]",
                    bfd.getPreBusiness().getHash());
            LPBridge lpBridge = getBridge(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName(),
                    bfd.getPreBusiness().getSwapAssetInformation().getQuote().getQuoteBase().getRelayApiKey());
            try {
                log.info("🔍 Debug: bfd raw data: {}", objectMapper.writeValueAsString(bfd));
                // Serialize and store business data to Redis cache 💾
                redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                        bfd.getEventInitSwap().getTransferId(), objectMapper.writeValueAsString(bfd));
                String appendData = (String) redisConfig.getRedisTemplate().opsForHash()
                        .get(KEY_BUSINESS_APPEND, bfd.getPreBusiness().getHash());
                bfd.getPreBusiness().setOrderAppendData(appendData);
                log.info("save KEY_BUSINESS_CACHE transferId:{}", bfd.getEventInitSwap().getTransferId());

                // Record the start time ⏱️
                long startTime = System.currentTimeMillis();
                // Get swap asset information
                ExtendedSingleSwapAsset swapAsset = (ExtendedSingleSwapAsset) bfd.getPreBusiness()
                        .getSwapAssetInformation();
                // Get the timeout duration
                long maxTimeout = swapAsset.getExpectedSingleStepTime();
                // Record the last log time
                long lastLogTime = System.currentTimeMillis();
                // Set the log output interval to 10 seconds
                long logInterval = 10000;
                Boolean doubleCheck = false;

                // Log that the transferOutEvent must be received within the specified time ⏳
                log.info("⏳ The transferOutEvent must be received within {} seconds ,businessHash: [{}] ",
                        maxTimeout, bfd.getPreBusiness().getHash());

                // Loop to check if the transferOutEvent is completed
                while (!doubleCheck) {
                    // Check if timeout has occurred
                    if ((System.currentTimeMillis() - startTime) > (maxTimeout * 1000)) {
                        throw new Exception(String.format("⏰ transferOutEvent Timeout businessHash: [%s]",
                                bfd.getPreBusiness().getHash()));
                    }

                    // Check if the transferOutEvent is completed
                    Boolean hit = initSwapEventService
                            .checkInitSwapEvent(normalizeHex(bfd.getEventInitSwap().getBidId()));
                    doubleCheck = hit != null && hit == true;

                    // Log waiting status 😴
                    log.info("😴 waiting initSwapEventMap....bid:{}", bfd.getEventInitSwap().getBidId());

                    // Output waiting log at specified intervals
                    if ((System.currentTimeMillis() - lastLogTime) >= logInterval) {
                        log.info(String.format("Still waiting for the condition to be satisfied,bid:%s",
                                normalizeHex(bfd.getEventInitSwap().getBidId())));
                        lastLogTime = System.currentTimeMillis();
                    }

                    // Sleep for 1 second
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        log.error("error", e);
                    }
                }
                log.info("✅ The initSwapEventMap is ready");
                log.info("✅ ✅ ConfirmInitSwap");
                notifyEventToAmm(bfd, CmdEvent.EVENT_INIT_SWAP);
                createSwapOrder(bfd);
                exePoolService.submit(() -> {
                    // log.info("Temporarily no-op");
                    doConfirmSwap(bfd, lpBridge);
                });
                return true;
            } catch (Exception e) {
                log.error("error", e);
                return false;
            }
        });
        return true;
    }

    private boolean notifyEventToAmm(SingleSwapBusinessFullData bfdFromRelay, String cmdStr) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            LPBridge lpBridge = getBridge(bfdFromRelay.getPreBusiness().getSwapAssetInformation().getBridgeName(),
                    bfdFromRelay.getPreBusiness().getSwapAssetInformation().getQuote().getQuoteBase().getRelayApiKey());
            CmdEvent<SingleSwapBusinessFullData> cmdEvent = new CmdEvent<SingleSwapBusinessFullData>();
            cmdEvent.setBusinessFullData(bfdFromRelay);
            cmdEvent.setCmd(cmdStr);
            log.info("🚀 Preparing to send CmdEvent: {}", cmdStr);
            log.info("🆔 Swap ID: {}", bfdFromRelay.getPreBusiness().getHash());
            log.info("🌉 Bridge Name: {}", lpBridge.getMsmqName());
            log.info("🔑 Relay API Key: {}", lpBridge.getRelayApiKey());
            log.info("📨 Sending to Queue: {}", lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey());
            log.info("📝 CmdEvent Content: {}", gson.toJson(cmdEvent));

            redisConfig.getRedisTemplate().convertAndSend(
                    lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey(),
                    cmdEvent);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean createSwapOrder(SingleSwapBusinessFullData bfdFromRelay) {
        log.info("bfd:{}", JsonUtils.toCompactJsonString(bfdFromRelay));
        SwapOrder swapOrder = new SwapOrder()
                .setBusinessId(bfdFromRelay.getBusiness().getBusinessHash());
        return false;
    }

    public void doConfirmSwap(SingleSwapBusinessFullData bfd, LPBridge lpBridge) {
        String businessHash = bfd.getPreBusiness().getHash();
        log.info("🚀 Starting doConfirmSwap for business hash: [{}]", businessHash);

        try {
            log.info("📝 Creating CommandConfirmSwap");
            CommandConfirmSwap commandConfirmSwap = createCommandConfirmSwap(bfd, lpBridge);
            log.info("✅ CommandConfirmSwap created successfully");

            Integer dstChainId = lpBridge.getBridge().getDstChainId();
            RequestDoConfirmSwap request = createRequestDoConfirmSwap(commandConfirmSwap, dstChainId);

            log.info("📤 Preparing to send request for doConfirmSwap");
            String requestJson = new GsonBuilder().setPrettyPrinting().create().toJson(request);
            log.info("📄 Request body:\n{}", requestJson);

            try {
                log.info("🔍 RequestDoConfirmSwap (ObjectMapper):\n{}",
                        objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(request));
            } catch (JsonProcessingException e) {
                log.warn("⚠️ Failed to serialize request with ObjectMapper", e);
            }

            String uri = lpBridge.getDstClientUri() + "/lpnode/single_swap/confirm_swap";
            log.info("🔗 Target URI: {}", uri);

            log.info("📡 Sending request to trade server");
            String response = restTemplate.postForObject(uri, request, String.class);
            log.info("📨 Received response: {}", response);

            log.info("🎉 doConfirmSwap completed successfully for business hash: [{}]", businessHash);
        } catch (Exception e) {
            log.error("❌ Error occurred during doConfirmSwap for business hash: [{}]", businessHash, e);
        }
    }

    private CommandConfirmSwap createCommandConfirmSwap(SingleSwapBusinessFullData bfd, LPBridge lpBridge) {
        return new CommandConfirmSwap()
                .setBid(bfd.getBusiness().getBusinessHash())
                .setSender(bfd.getEventInitSwap().getSender())
                .setSenderWalletName(lpBridge.getWallet().getName())
                .setUserReceiverAddress(bfd.getEventInitSwap().getReceiver())
                .setToken(bfd.getEventInitSwap().getToken())
                .setTokenAmount(bfd.getEventInitSwap().getAmount())
                .setDstToken(bfd.getEventInitSwap().getDstToken())
                .setDstAmount(bfd.getEventInitSwap().getDstAmount())
                .setAgreementReachedTime(bfd.getEventInitSwap().getAgreementReachedTime())
                .setExpectedSingleStepTime(bfd.getEventInitSwap().getExpectedSingleStepTime());
    }

    private RequestDoConfirmSwap createRequestDoConfirmSwap(CommandConfirmSwap commandConfirmSwap, Integer dstChainId) {
        RequestDoConfirmSwap request = new RequestDoConfirmSwap()
                .setTransactionType("LOCAL_PADDING")
                .setCommandConfirmSwap(commandConfirmSwap);

        if (ChainSetting.getInstance().needGasSetting(dstChainId)) {
            Gas gas = new Gas().setGasPrice(Gas.GAS_PRICE_TYPE_FAST);
            request.setGas(gas);
            log.info("⛽ Gas settings applied for destination chain ID: {}", dstChainId);
        }

        return request;
    }

    public Boolean onEventInitSwap(EventInitSwapBox eventBox) {
        String eventBusinessId = normalizeHex(eventBox.getEventParse().getBidId());
        log.info("onEventInitSwap:" + eventBusinessId);

        String redisKey = KEY_LOCKED_BUSINESS + ":" + eventBusinessId;
        String redisValue = (String) redisConfig.getRedisTemplate().opsForValue().get(redisKey);
        if (redisValue != null && redisValue.equals("true")) {
            log.info("✅ Found locked business in Redis, proceeding with transfer out logic. Key: {}", redisKey);
            initSwapEventService.addInitSwapEvent(eventBusinessId, true);
        } else {
            log.warn("⚠️ Locked business not found or not valid in Redis. Key: {}, Value: {}", redisKey, redisValue);

        }

        String transferId = eventBox.getEventParse().getTransferId();

        if (transferId != null && !transferId.startsWith("0x")) {
            transferId = "0x" + transferId;
        }
        String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE, transferId);
        if (cacheData == null) {
            log.info("KEY_BUSINESS_CACHE find empty -- {}-{}", KEY_BUSINESS_CACHE,
                    transferId);
            return false;
        }
        return true;

    }

    public void onEventConfirmSwap(EventConfirmSwapBox eventBox) {
        try {
            log.info("EventConfirmSwapBox:{}", eventBox.toString());
            log.info("🚀 Start processing event with transferId: {}", eventBox.getEventParse().getTransferId());

            // fetch business from redis
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(
                    KEY_BUSINESS_CACHE,
                    eventBox.getEventParse().getTransferId());

            // Check if cache data exists
            if (cacheData == null) {
                log.warn("⚠️ Business cache not found - Key: {}, TransferId: {}",
                        KEY_BUSINESS_CACHE,
                        eventBox.getEventParse().getTransferId());
                return;
            }

            log.info("📝 Successfully retrieved cache data for transferId: {}",
                    eventBox.getEventParse().getTransferId());

            // Parse business data
            SingleSwapBusinessFullData bfd = objectMapper.readValue(cacheData, SingleSwapBusinessFullData.class);
            log.info("✅ Business data parsed successfully - BridgeName: {}, RelayApiKey: {}",
                    bfd.getPreBusiness().getSwapAssetInformation().getBridgeName(),
                    bfd.getPreBusiness().getSwapAssetInformation().getQuote().getQuoteBase().getRelayApiKey());

            // Get bridge instance
            String bridgeName = bfd.getPreBusiness().getSwapAssetInformation().getBridgeName();
            String relayApiKey = bfd.getPreBusiness().getSwapAssetInformation().getQuote().getQuoteBase()
                    .getRelayApiKey();
            LPBridge lpBridge = getBridge(bridgeName, relayApiKey);
            log.info("🌉 Bron_transfer_inidge instance created for bridge: {}", bridgeName);

            EventConfirmSwap eventConfirmSwap = eventBox.getEventParse();

            bfd.setEventConfirmSwap(eventConfirmSwap);
            String appendData = (String) redisConfig.getRedisTemplate().opsForHash()
                    .get(KEY_BUSINESS_APPEND, bfd.getPreBusiness().getHash());
            bfd.getPreBusiness().setOrderAppendData(appendData);
            String updatedData = objectMapper.writeValueAsString(bfd);
            redisConfig.getRedisTemplate().opsForHash().put(
                    KEY_BUSINESS_CACHE,
                    eventBox.getEventParse().getTransferId(),
                    updatedData);
            log.info("💾 Updated business data saved back to Redis - TransferId: {}",
                    eventBox.getEventParse().getTransferId());

            // Notify confirm swap
            log.info("📤 Sending confirm swap notification to LP Bridge...");
            notifyEventToAmm(bfd, CmdEvent.EVENT_CONFIRM_SWAP);
            singleSwapRestClient.NotifyConfirmSwap(lpBridge, bfd);
            log.info("🎉 Successfully sent confirm swap notification for transferId: {}",
                    eventBox.getEventParse().getTransferId());

        } catch (JsonProcessingException e) {
            log.error("❌ Failed to parse business data - TransferId: {} - Error: {}",
                    eventBox.getEventParse().getTransferId(),
                    e.getMessage(),
                    e);
        } catch (Exception e) {
            healthReport.reportError("onEventConfirmSwap error:" + e.getMessage(), "lpnode:error:report:event_process");
            log.error("💥 Unexpected error while processing event - TransferId: {} - Error: {}",
                    eventBox.getEventParse().getTransferId(),
                    e.getMessage(),
                    e);
        }
    }

    public void onEventRefundSwap(EventRefundSwapBox eventBox) {
        try {
            log.info("event box is:{}", JsonUtils.toCompactJsonString(eventBox));
            // Log the start of the event processing
            log.info("🔄 Processing refund swap event - TransferId: {}", eventBox.getEventParse().getTransferId());

            // Fetch business from redis
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(
                    KEY_BUSINESS_CACHE,
                    eventBox.getEventParse().getTransferId());

            // Check if cache data exists
            if (cacheData == null) {
                log.warn("⚠️ Business cache not found - Key: {}, TransferId: {}",
                        KEY_BUSINESS_CACHE,
                        eventBox.getEventParse().getTransferId());
                return;
            }

            // Log successful cache retrieval
            log.info("✅ Successfully retrieved business cache - TransferId: {}",
                    eventBox.getEventParse().getTransferId());

            // Deserialize the cache data
            SingleSwapBusinessFullData bfd = objectMapper.readValue(cacheData, SingleSwapBusinessFullData.class);
            String appendData = (String) redisConfig.getRedisTemplate().opsForHash()
                    .get(KEY_BUSINESS_APPEND, bfd.getPreBusiness().getHash());
            bfd.getPreBusiness().setOrderAppendData(appendData);
            bfd.setEventRefundSwap(eventBox.getEventParse());
            // Log successful deserialization
            log.info("✅ Successfully deserialized business data - TransferId: {}",
                    eventBox.getEventParse().getTransferId());

            // Notify the event to AMM
            notifyEventToAmm(bfd, CmdEvent.EVENT_REFUND_SWAP);

            // Log successful event notification
            log.info("✅ Successfully notified AMM of refund swap event - TransferId: {}",
                    eventBox.getEventParse().getTransferId());

        } catch (Exception e) {
            healthReport.reportError("onEventRefundSwap error:" + e.getMessage(), "lpnode:error:report:event_process");
            // Log the exception with detailed information
            log.error("❌ Error processing refund swap event - TransferId: {}, Error: {}",
                    eventBox.getEventParse().getTransferId(), e.getMessage(), e);
        }
    }

}