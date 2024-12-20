package com.bytetrade.obridge.component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.ExecutorService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.annotation.Resource;

import org.springframework.data.redis.core.RedisTemplate;
import org.quartz.SchedulerException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bytetrade.obridge.component.client.request.CommandTransferInConfirm;
import com.bytetrade.obridge.component.client.request.CommandTransferIn;
import com.bytetrade.obridge.component.client.request.CommandTransferInRefund;
import com.bytetrade.obridge.component.client.request.Gas;
import com.bytetrade.obridge.component.client.request.RequestDoTransferIn;
import com.bytetrade.obridge.component.client.request.RequestDoTransferInConfirm;
import com.bytetrade.obridge.component.client.request.RequestDoTransferInRefund;
import com.bytetrade.obridge.bean.EventTransferOutBox;
import com.bytetrade.obridge.bean.EventTransferInBox;
import com.bytetrade.obridge.bean.EventTransferConfirmBox;
import com.bytetrade.obridge.bean.EventTransferRefundBox;
import com.bytetrade.obridge.bean.ExtendedAtomicSwapAsset;
import com.bytetrade.obridge.bean.EventTransferInConfirm;
import com.bytetrade.obridge.bean.EventTransferInRefund;
import com.bytetrade.obridge.bean.AskCmd;
import com.bytetrade.obridge.bean.AtomicBusinessFullData;
import com.bytetrade.obridge.bean.BusinessEvent;
import com.bytetrade.obridge.bean.BusinessEventItem;
import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.CmdEvent;
import com.bytetrade.obridge.bean.QuoteRemoveInfo;

import com.bytetrade.obridge.db.redis.RedisConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class AtomicLPController extends LpControllerBase {


    @Autowired
    private ExecutorService exePoolService;

    @Autowired
    private TaskSchedulerService taskSchedulerService;

    @Resource
    RedisConfig redisConfig;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    CommandWatcher cmdWatcher;

    @Autowired
    AtomicRestClient atomicRestClient;

    Map<String, Boolean> transferOutEventMap = new ConcurrentHashMap<String, Boolean>();

    Map<String, Boolean> transferOutConfirmEventMap = new ConcurrentHashMap<String, Boolean>();

    @Autowired
    private List<String> lockedBusinessList;

    List<String> transferOutIdList = new CopyOnWriteArrayList<String>(); // new ArrayList<String>();

    public LPBridge getBridgeFromChannel(String channel) {
        return lpBridgeService.getLPBridge(channel);
    }

    /**
     * from relay
     * 
     * @param askCmd
     */
    public void relayAskQuote(AskCmd askCmd) {
        log.info("<- [Relay HTTP call] on ask quote:" + askCmd.toString());
        log.info("maps {}", lpBridgeService.getLpBridgesChannelMap().toString());
        String[] parts = askCmd.getBridge().split("_");
        String key = parts[2] + "/" + parts[3] + "_" + parts[0] + "_" + parts[1];
        LPBridge lpBridge = lpBridgeService.getLPBridge(key + "_" + askCmd.getRelayApiKey());
        log.info("<- ask Quote from relay , relayApiKey:{} ,bridgeKey:{}", askCmd.getRelayApiKey(), key);

        CmdEvent cmdEvent = new CmdEvent()
                .setCmd(CmdEvent.CMD_ASK_QUOTE)
                .setCid(askCmd.getCid())
                .setLpId(lpBridge.getLpId())
                .setAmount(askCmd.getAmount());
        log.info("lpBridge:" + lpBridge.toString());
        log.info("cmdEvent:" + cmdEvent.toString());
        try {
            log.info("-> redis send message to {}", lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey());
            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey(),
                    cmdEvent);
        } catch (Exception e) {
            log.error("error", e);
        }
    }



    public Boolean onEventTransferOut(EventTransferOutBox eventBox) {
        BusinessEvent businessEvent = new BusinessEvent()
                .setBusinessEvent(BusinessEventItem.LP_EVENT_CHAIN_CLIENT_TRANSFER_OUT.getValue())
                .setSystem("LP_NODE");
        redisConfig.getRedisTemplate().convertAndSend(KEY_BUSINESS_EVENT, businessEvent);
        String eventBusinessId = getHexString(eventBox.getEventParse().getBidId());
        log.info("onEventTransferOut:" + eventBusinessId);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for (String businessId : lockedBusinessList) {
            log.info("Compare lockedBusinessId:{} eventBusinessId:{}", businessId, eventBusinessId);
            if (businessId.equalsIgnoreCase(eventBusinessId)) {
                transferOutEventMap.put(businessId, true);
                log.info("add transferOutId to transferOutEventMap:" + eventBox.getEventParse().getTransferId());
                System.out.println(gson.toJson(eventBox));
                transferOutIdList.add(eventBox.getEventParse().getTransferId());
                lockedBusinessList.remove(businessId);
                break;
            }
        }
        return true;
    }

    /**
     * This method is invoked by the relay.
     * relay -> update_business_transfer_out
     * 
     * @param bfd
     * @return
     */

    public Boolean onRelayTransferOut(AtomicBusinessFullData bfd) {
        exePoolService.submit(() -> {
            log.info("updateBusinessTransferOut The subsequent actions begin to execute. [{}]",
                    bfd.getPreBusiness().getHash());
            LPBridge lpBridge = getBridge(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName(),
                    bfd.getPreBusiness().getSwapAssetInformation().getQuote().getQuoteBase().getRelayApiKey());

            bfd.getPreBusiness().setOrderAppendData((String) redisConfig.getRedisTemplate().opsForHash()
                    .get(KEY_BUSINESS_APPEND, bfd.getPreBusiness().getHash()));
            CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_OUT);
            try {
                redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                        bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));

                long startTime = System.currentTimeMillis();
                ExtendedAtomicSwapAsset swapAsset = (ExtendedAtomicSwapAsset) bfd.getPreBusiness()
                        .getSwapAssetInformation();
                long maxTimeout = swapAsset.getExpectedSingleStepTime();
                long lastLogTime = System.currentTimeMillis(); // Record the time of the last log output
                long logInterval = 10000; // Set log output interval to 10 seconds in milliseconds
                Boolean doubleCheck = false;
                log.info("The transferOutEvent must be received within {} seconds ,businessHash: [{}] ", maxTimeout,
                        bfd.getPreBusiness().getHash());
                while (!doubleCheck) {
                    if ((System.currentTimeMillis() - startTime) > (maxTimeout * 1000)) {
                        throw new Exception(String.format("transferOutEvent Timeout businessHash: [%s]",
                                bfd.getPreBusiness().getHash()));
                    }
                    Boolean hit = transferOutEventMap.get(getHexString(bfd.getEventTransferOut().getBidId()));
                    doubleCheck = hit != null && hit == true;
                    // Check if logging is required based on the specified interval
                    log.info("waiting transferOutEventMap....bid:{}", bfd.getEventTransferOut().getBidId());
                    if ((System.currentTimeMillis() - lastLogTime) >= logInterval) {
                        log.info(String.format("Still waiting for the condition to be satisfied,bid:%s",
                                getHexString(bfd.getEventTransferOut().getBidId())));
                        lastLogTime = System.currentTimeMillis(); // Update the time of the last log output
                    }
                    try {
                        Thread.sleep(1000);
                    } catch (Exception e) {
                        log.error("error", e);
                    }
                }
                log.info("The transferOutEventMap is ready");
                log.info("Send Transaction out Event to Amm");
                redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey(),
                        cmdEvent);
                return true;
            } catch (Exception e) {
                log.error("error", e);
                return false;
            }
        });

        return true;
    }

    public void doTransferIn(AtomicBusinessFullData bfd, LPBridge lpBridge) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        log.info("do transfer in  businessHash: [{}]", bfd.getPreBusiness().getHash());
        CommandTransferIn commandTransferIn = new CommandTransferIn()
                .setBid(bfd.getBusiness().getBusinessHash())
                .setSenderWalletName(lpBridge.getWallet().getName())
                .setUserReceiverAddress(bfd.getEventTransferOut().getDstAddress())
                .setToken(bfd.getEventTransferOut().getDstToken())
                .setTokenAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstAmountNeed())
                .setEthAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstNativeAmountNeed())
                .setAgreementReachedTime(bfd.getEventTransferOut().getAgreementReachedTime())
                .setSrcChainId(lpBridge.getBridge().getSrcChainId())
                .setSrcTransferId(bfd.getEventTransferOut().getTransferId())
                .setAppendInformation(bfd.getPreBusiness().getSwapAssetInformation().getAppendInformation())
                .setExpectedSingleStepTime(bfd.getEventTransferOut().getExpectedSingleStepTime())
                .setTolerantSingleStepTime(bfd.getEventTransferOut().getTolerantSingleStepTime())
                .setEarliestRefundTime(bfd.getEventTransferOut().getEarliestRefundTime());

        Integer dstChainId = lpBridge.getBridge().getDstChainId();
        if (dstChainId == 9000 || dstChainId == 9006 || dstChainId == 60 || dstChainId == 966 || dstChainId == 614) {
            commandTransferIn.setHashLock(bfd.getPreBusiness().getHashlockEvm());
        } else if (dstChainId == 397) {
            commandTransferIn.setHashLock(bfd.getPreBusiness().getHashlockNear());
        } else if (dstChainId == 144) {
            commandTransferIn.setHashLock(bfd.getPreBusiness().getHashlockXrp());
        } else if (dstChainId == 501) {
            commandTransferIn.setHashLock(bfd.getPreBusiness().getHashlockSolana());
        }
        redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_ID_SHADOW, commandTransferIn.getHashLock(),
                commandTransferIn.getSrcTransferId());
        RequestDoTransferIn request = new RequestDoTransferIn()
                .setTransactionType("LOCAL_PADDING")
                .setCommandTransferIn(commandTransferIn);

        if (ChainSetting.getInstance().needGasSetting(dstChainId)) {
            Gas gas = new Gas().setGasPrice(Gas.GAS_PRICE_TYPE_FAST);
            request.setGas(gas);
        }
        String requestJson = gson.toJson(request);
        log.info("send request to trade [transferIn]");
        log.info("request body:{}", requestJson);
        try {
            log.info("RequestDoTransferIn:" + objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
        }

        String uri = lpBridge.getDstClientUri() + "/lpnode/transfer_in";
        log.info("uri:" + uri);
        String objectResponseEntity = restTemplate.postForObject(
                uri,
                request,
                String.class);

        log.info("response message:{}", objectResponseEntity);
    }

    public void onEventTransferIn(EventTransferInBox eventBox) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println("onTransferIn:");
        System.out.println(gson.toJson(eventBox));

        try {
            if (eventBox.getMatchingHashlock() != null && eventBox.getMatchingHashlock() == true) {
                // matching hashlock (XRP)
                String srcTransferId = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_ID_SHADOW,
                        eventBox.getEventParse().getHashLockOriginal());
                eventBox.getEventParse().setSrcTransferId(srcTransferId);
            }

            // fetch business
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE,
                    eventBox.getEventParse().getSrcTransferId());
            if (cacheData == null) {
                log.info("KEY_BUSINESS_CACHE find empty getSrcTransferId: {}-{}", KEY_BUSINESS_CACHE,
                        eventBox.getEventParse().getSrcTransferId());
                return;
            }
            AtomicBusinessFullData bfd = objectMapper.readValue(cacheData, AtomicBusinessFullData.class);
            if (eventBox.getMatchingHashlock() != null && eventBox.getMatchingHashlock() == true) {
                // sync business
                // LPBridge lpBridge =
                // lpBridges.get(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName());
                LPBridge lpBridge = getBridge(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName(),
                        bfd.getPreBusiness().getSwapAssetInformation().getQuote().getQuoteBase().getRelayApiKey());
                eventBox.getEventParse().setSrcChainId(lpBridge.getBridge().getSrcChainId());
            }
            bfd.getPreBusiness().setInTradeUuid(eventBox.getEventParse().getUuid());
            // update cache
            eventBox.getEventParse().setTransferInfo(eventBox.getTransferInfo());
            bfd.setEventTransferIn(eventBox.getEventParse());

            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                    bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                    eventBox.getEventParse().getTransferId(), objectMapper.writeValueAsString(bfd));

            log.info("TransferIn:" + bfd.getEventTransferIn());
            log.info("bfd:" + objectMapper.writeValueAsString(bfd));

            LPBridge lpBridge = getBridge(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName(),
                    bfd.getPreBusiness().getSwapAssetInformation().getQuote().getQuoteBase().getRelayApiKey());

            // exePoolService Task @ if user confirm timeout
            exePoolService.submit(() -> {
                log.info("🫎 execute check in new thread");
                refundTransferInOnUserConfirmTimeout(bfd, lpBridge);
            });

            CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_IN);
            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey(),
                    cmdEvent);

            String objectResponseEntity = atomicRestClient.doNotifyTransferIn(lpBridge, bfd);

            log.info("response message:{}", objectResponseEntity);
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    private void refundTransferInOnUserConfirmTimeout(AtomicBusinessFullData businessFullData, LPBridge lpBridge) {
        ExtendedAtomicSwapAsset swapAsset = (ExtendedAtomicSwapAsset) businessFullData.getPreBusiness()
                .getSwapAssetInformation();
        long agreementReachedTime = swapAsset.getAgreementReachedTime();
        long expectedSingleStepTime = swapAsset.getExpectedSingleStepTime();
        long tolerantSingleStepTime = swapAsset.getTolerantSingleStepTime();
        long earliestRefundTime = swapAsset.getEarliestRefundTime();
        long earliestRefundTimeMs = earliestRefundTime * 1000;

        long triggerTimeInMilliseconds = (agreementReachedTime * 1000) + (1000 * expectedSingleStepTime * 3)
                + (1000 * tolerantSingleStepTime * 4) + (1000 * 60 * 3);
        log.info("triggerTimeInMilliseconds:{} ", triggerTimeInMilliseconds);
        long executeAfter = earliestRefundTimeMs - System.currentTimeMillis() + 1000 * 30;
        if (executeAfter <= 0) {
            executeAfter = 1000;
        }
        if (triggerTimeInMilliseconds <= earliestRefundTimeMs) {
            log.warn(
                    "🪰 The refund time cannot be earlier than the earliest refund time Scheduled Time {} , Earliest Time {}",
                    triggerTimeInMilliseconds, earliestRefundTimeMs);
        }
        log.info("🪰 Set up a timer to prepare for checking the timeout situation of user ConfirmOut ,exec after:{}",
                executeAfter);
        long executeAfterMillis = executeAfter;
        // Calculate hours, minutes, and seconds
        long hours = executeAfterMillis / (1000 * 60 * 60);
        long minutes = (executeAfterMillis / (1000 * 60)) % 60;
        long seconds = (executeAfterMillis / 1000) % 60;

        // Display the result
        System.out.printf("%d hours %d minutes %d seconds later%n", hours, minutes, seconds);

        log.info(
                "If the user does not confirm after {} hours {} minutes {} seconds , then a refund will be issued.",
                hours, minutes, seconds);
        try {
            taskSchedulerService.scheduleTask(() -> {
                try {
                    log.info("Time's up...");
                    String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE,
                            businessFullData.getEventTransferOut().getTransferId());
                    AtomicBusinessFullData bfd = objectMapper.readValue(cacheData, AtomicBusinessFullData.class);
                    if (bfd.getEventTransferInConfirm() != null) {
                        log.info("lp has already confirmed in ");
                        return;
                    }
                    if (bfd.getEventTransferOutConfirm() == null
                            || bfd.getEventTransferOutConfirm().getTransferId() == null
                            || bfd.getEventTransferOutConfirm().getTransferId() == "") {
                        log.info("user not confirmOut");
                        doTransferInRefund(businessFullData, lpBridge);
                        return;
                    }
                    log.info("user confirm out all right");
                } catch (Exception e) {
                    log.error("process user confirm timeoutt error:{}", e.getMessage());
                }
                return;
            }, executeAfter);
        } catch (SchedulerException e) {
            log.error("Failed to schedule task: {}", e.getMessage());
        }
    }

    private void onEventConfirm(EventTransferConfirmBox eventBox) {
        String transferId = eventBox.getEventParse().getTransferId();
        for (String tId : transferOutIdList) {
            if (tId.equalsIgnoreCase(transferId)) {
                transferOutConfirmEventMap.put(transferId, true);
                log.info("✅ Confirm and push to transferOutConfirmEventMap {}", transferId);
                transferOutIdList.remove(transferId);
                break;
            }
        }
    }

    public Boolean onRelayTransferOutConfirm(AtomicBusinessFullData bfdFromRelay) {
        // LPBridge lpBridge =
        // lpBridges.get(bfdFromRelay.getPreBusiness().getSwapAssetInformation().getBridgeName());
        LPBridge lpBridge = getBridge(bfdFromRelay.getPreBusiness().getSwapAssetInformation().getBridgeName(),
                bfdFromRelay.getPreBusiness().getSwapAssetInformation().getQuote().getQuoteBase().getRelayApiKey());
        try {
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE,
                    bfdFromRelay.getEventTransferOut().getTransferId());
            AtomicBusinessFullData bfd = objectMapper.readValue(cacheData, AtomicBusinessFullData.class);
            bfd.setEventTransferOutConfirm(bfdFromRelay.getEventTransferOutConfirm());
            bfd.setPreBusiness(bfdFromRelay.getPreBusiness());
            bfd.getPreBusiness().setOrderAppendData((String) redisConfig.getRedisTemplate().opsForHash()
                    .get(KEY_BUSINESS_APPEND, bfd.getPreBusiness().getHash()));
            CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_OUT_CONFIRM);

            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                    bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                    bfd.getEventTransferIn().getTransferId(), objectMapper.writeValueAsString(bfd));

            Boolean doubleCheck = false;
            // Record start time
            long startTime = System.currentTimeMillis();
            // Set timeout to 5 minutes (in milliseconds)
            long timeout = 300000;
            while (!doubleCheck) {
                // Check if timeout exceeded
                if (System.currentTimeMillis() - startTime > timeout) {
                    log.info("⏰ Timeout exceeded while waiting for transfer out confirmation.");
                    break;
                }
                Boolean hit = transferOutConfirmEventMap.get(bfd.getEventTransferOutConfirm().getTransferId());
                doubleCheck = hit != null && hit == true;
                try {
                    log.info("⌛ Waiting for transfer out confirm map bid:{} , OutTransferId:{}",
                            bfdFromRelay.getPreBusiness().getHash(), bfd.getEventTransferOutConfirm().getTransferId());
                    Thread.sleep(500);
                } catch (Exception e) {
                    log.error("❌ error", e);
                }
            }
            log.info("📤 send TxOutConfirm EVENT, channel: {},",
                    lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey());
            log.info("🔗 businessHash: {}", bfdFromRelay.getPreBusiness().getHash());
            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey(),
                    cmdEvent);
        } catch (Exception e) {
            log.error("error", e);
            return false;
        }

        return true;
    }

    public void doTransferInConfirm(AtomicBusinessFullData bfd, LPBridge lpBridge) {
        log.info("🔄 do transfer in confirm businessHash: [{}]", bfd.getPreBusiness().getHash());
        CommandTransferInConfirm commandTransferInConfirm = new CommandTransferInConfirm()
                .setBid(bfd.getPreBusiness().getHash())
                .setUuid(bfd.getEventTransferIn().getTransferId())
                .setSenderWalletName(lpBridge.getWallet().getName())
                .setUserReceiverAddress(bfd.getEventTransferOut().getDstAddress())
                .setToken(bfd.getEventTransferOut().getDstToken())
                .setTokenAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstAmountNeed())
                .setEthAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstNativeAmountNeed())
                .setHashLock(bfd.getEventTransferOut().getHashLock())
                .setAgreementReachedTime(bfd.getEventTransferOut().getAgreementReachedTime())
                .setExpectedSingleStepTime(bfd.getEventTransferOut().getExpectedSingleStepTime())
                .setTolerantSingleStepTime(bfd.getEventTransferOut().getTolerantSingleStepTime())
                .setEarliestRefundTime(bfd.getEventTransferOut().getEarliestRefundTime())
                .setPreimage(bfd.getEventTransferOutConfirm().getPreimage())
                .setAppendInformation(bfd.getPreBusiness().getSwapAssetInformation().getAppendInformation())
                .setTransferId(bfd.getEventTransferIn().getTransferId());
        Gas gas = new Gas().setGasPrice(Gas.GAS_PRICE_TYPE_STANDARD);
        RequestDoTransferInConfirm request = new RequestDoTransferInConfirm()
                .setTransactionType("LOCAL_PADDING")
                .setCommandTransferInConfirm(commandTransferInConfirm)
                .setGas(gas);

        Integer dstChainId = lpBridge.getBridge().getDstChainId();
        if (dstChainId == 9000 || dstChainId == 9006 || dstChainId == 60 || dstChainId == 966 || dstChainId == 614) {
            commandTransferInConfirm.setHashLock(bfd.getPreBusiness().getHashlockEvm());
        } else if (dstChainId == 397) {
            commandTransferInConfirm.setHashLock(bfd.getPreBusiness().getHashlockNear());
        } else if (dstChainId == 144) {
            commandTransferInConfirm.setHashLock(bfd.getPreBusiness().getHashlockXrp());
        }

        try {
            log.info("RequestDoTransferIn:" + objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String objectResponseEntity = restTemplate.postForObject(
                lpBridge.getDstClientUri() + "/lpnode/confirm",
                request,
                String.class);

        log.info("response message:{}", objectResponseEntity.toString());
    }

    /**
     * chain-client
     * 
     * @param eventBox
     */
    public void onConfirm(EventTransferConfirmBox eventBox) {
        try {
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE,
                    eventBox.getEventParse().getTransferId());
            if (cacheData == null) {
                log.info("KEY_BUSINESS_CACHE find empty: {}-{}", KEY_BUSINESS_CACHE,
                        eventBox.getEventParse().getTransferId());
                return;
            }
            AtomicBusinessFullData bfd = objectMapper.readValue(cacheData, AtomicBusinessFullData.class);

            // Divert TransferOutConfirm and TransferInConfirm
            // TransferOutConfirm
            if (!eventBox.getEventParse().getTransferId().equalsIgnoreCase(bfd.getEventTransferIn().getTransferId())) {
                log.info("not hit Transfer in ,is outConfirm");
                log.info("TransferIn TransferId:" + bfd.getEventTransferIn().getTransferId());
                log.info("Event id:" + eventBox.getEventParse().getTransferId());
                log.info("Business Id:{}", bfd.getPreBusiness().getHash());
                log.info("EventTransferConfirmBox:" + eventBox.toString());
                log.info("BusinessFullData:" + bfd.toString());
                onEventConfirm(eventBox);
                return;
            }

            // TransferInConfirm
            // update cache
            EventTransferInConfirm eventTransferInConfirm = new EventTransferInConfirm()
                    .setBusinessId(bfd.getBusiness().getBusinessId())
                    .setTransferInfo(eventBox.getTransferInfo())
                    .setTransferId(eventBox.getEventParse().getTransferId())
                    .setPreimage(eventBox.getEventParse().getPreimage());
            bfd.setEventTransferInConfirm(eventTransferInConfirm);

            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                    bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                    eventBox.getEventParse().getTransferId(), objectMapper.writeValueAsString(bfd));

            log.info("TransferInConfirm:" + bfd.getEventTransferIn());

            log.info("bfd:" + objectMapper.writeValueAsString(bfd));

            // call relay
            // LPBridge lpBridge =
            // lpBridges.get(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName());

            LPBridge lpBridge = getBridge(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName(),
                    bfd.getPreBusiness().getSwapAssetInformation().getQuote().getQuoteBase().getRelayApiKey());

            CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_IN_CONFIRM);
            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey(),
                    cmdEvent);

            String objectResponseEntity = atomicRestClient.doNotifyTransferInConfirm(lpBridge, bfd);

            log.info("response message:{}", objectResponseEntity.toString());
        } catch (Exception e) {
            log.error("error", e);
            return;
        }
    }

    public Boolean onRelayTransferOutRefund(AtomicBusinessFullData bfdFromRelay) {
        // LPBridge lpBridge =
        // lpBridges.get(bfdFromRelay.getPreBusiness().getSwapAssetInformation().getBridgeName());
        LPBridge lpBridge = getBridge(bfdFromRelay.getPreBusiness().getSwapAssetInformation().getBridgeName(),
                bfdFromRelay.getPreBusiness().getSwapAssetInformation().getQuote().getQuoteBase().getRelayApiKey());
        try {
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE,
                    bfdFromRelay.getEventTransferOut().getTransferId());
            AtomicBusinessFullData bfd = objectMapper.readValue(cacheData, AtomicBusinessFullData.class);
            bfd.setEventTransferOutRefund(bfdFromRelay.getEventTransferOutRefund());
            bfd.setPreBusiness(bfdFromRelay.getPreBusiness());
            bfd.getPreBusiness().setOrderAppendData((String) redisConfig.getRedisTemplate().opsForHash()
                    .get(KEY_BUSINESS_APPEND, bfd.getPreBusiness().getHash()));
            CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_OUT_REFUND);

            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                    bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                    bfd.getEventTransferIn().getTransferId(), objectMapper.writeValueAsString(bfd));

            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey(),
                    cmdEvent);
        } catch (Exception e) {
            log.error("error", e);
            return false;
        }

        return true;
    }

    public void doTransferInRefund(AtomicBusinessFullData bfd, LPBridge lpBridge) {
        log.info("do transfer in refund businessHash: [{}]", bfd.getPreBusiness().getHash());
        log.info("tx_in info:{}", bfd.getEventTransferIn().toString());
        CommandTransferInRefund commandTransferInRefund = new CommandTransferInRefund()
                .setBid(bfd.getPreBusiness().getHash())
                .setUuid(bfd.getEventTransferIn().getTransferId())
                .setSenderWalletName(lpBridge.getWallet().getName())
                .setUserReceiverAddress(bfd.getEventTransferOut().getDstAddress())
                .setToken(bfd.getEventTransferOut().getDstToken())
                .setTokenAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstAmountNeed())
                .setEthAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstNativeAmountNeed())
                .setHashLock(bfd.getEventTransferOut().getHashLock())
                .setExpectedSingleStepTime(bfd.getEventTransferOut().getExpectedSingleStepTime())
                .setTolerantSingleStepTime(bfd.getEventTransferOut().getTolerantSingleStepTime())
                .setEarliestRefundTime(bfd.getEventTransferOut().getEarliestRefundTime())
                .setAgreementReachedTime(bfd.getEventTransferOut().getAgreementReachedTime())
                .setAppendInformation(bfd.getPreBusiness().getSwapAssetInformation().getAppendInformation());
        Gas gas = new Gas().setGasPrice(Gas.GAS_PRICE_TYPE_STANDARD);
        RequestDoTransferInRefund request = new RequestDoTransferInRefund()
                .setTransactionType("LOCAL_PADDING")
                .setCommandTransferRefund(commandTransferInRefund)
                .setGas(gas);

        try {
            log.info("RequestDoTransferInRefund:" + objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String objectResponseEntity = restTemplate.postForObject(
                lpBridge.getDstClientUri() + "/lpnode/refund",
                request,
                String.class);

        log.info("response message:{}", objectResponseEntity.toString());
    }

    public void onEventRefund(EventTransferRefundBox eventBox) {
        try {
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE,
                    eventBox.getEventParse().getTransferId());
            if (cacheData == null) {
                log.info("KEY_BUSINESS_CACHE find empty -- {}-{}", KEY_BUSINESS_CACHE,
                        eventBox.getEventParse().getTransferId());
                return;
            }
            AtomicBusinessFullData bfd = objectMapper.readValue(cacheData, AtomicBusinessFullData.class);

            if (!eventBox.getEventParse().getTransferId().equalsIgnoreCase(bfd.getEventTransferIn().getTransferId())) {
                log.info("not hit Transfer in");
                log.info("bfd id:" + bfd.getEventTransferIn().getTransferId());
                log.info("event id:" + eventBox.getEventParse().getTransferId());
                log.info("EventTransferConfirmBox:", eventBox.toString());
                log.info("BusinessFullData:", bfd.toString());
                return;
            }
            // update cache
            EventTransferInRefund eventTransferInRefund = new EventTransferInRefund()
                    .setBusinessId(bfd.getBusiness().getBusinessId())
                    .setTransferInfo(eventBox.getTransferInfo())
                    .setTransferId(eventBox.getEventParse().getTransferId());
            bfd.setEventTransferInRefund(eventTransferInRefund);

            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                    bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE,
                    eventBox.getEventParse().getTransferId(), objectMapper.writeValueAsString(bfd));

            log.info("TransferInRefund:" + bfd.getEventTransferIn());

            log.info("bfd:" + objectMapper.writeValueAsString(bfd));

            // call relay
            // LPBridge lpBridge =
            // lpBridges.get(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName());
            LPBridge lpBridge = getBridge(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName(),
                    bfd.getPreBusiness().getSwapAssetInformation().getQuote().getQuoteBase().getRelayApiKey());
            CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_IN_REFUND);
            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey(),
                    cmdEvent);

            String objectResponseEntity = atomicRestClient.doNotifyTransferInRefund(lpBridge, bfd);

            log.info("response message:{}", objectResponseEntity.toString());
        } catch (Exception e) {
            log.error("error", e);
            return;
        }
    }

}