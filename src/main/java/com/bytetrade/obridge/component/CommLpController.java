package com.bytetrade.obridge.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.web3j.crypto.Hash;

import com.bytetrade.obridge.bean.CmdEvent;
import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.LPConfigCache;
import com.bytetrade.obridge.bean.PreBusiness;
import com.bytetrade.obridge.bean.QuoteAuthenticationLimiter;
import com.bytetrade.obridge.bean.QuoteBase;
import com.bytetrade.obridge.bean.QuoteData;
import com.bytetrade.obridge.bean.QuoteRemoveInfo;
import com.bytetrade.obridge.bean.RealtimeQuote;
import com.bytetrade.obridge.component.client.request.AbstractSignMessage;
import com.bytetrade.obridge.component.client.request.SignMessageFactory;
import com.bytetrade.obridge.component.service.CommandWatcher;
import com.bytetrade.obridge.component.service.LockedBusinessService;
import com.bytetrade.obridge.db.redis.RedisConfig;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
public class CommLpController extends LpControllerBase {
    @Value("${lpnode.uri}")
    private String selfUri;

    Map<String, LPBridge> lpBridges = new ConcurrentHashMap<String, LPBridge>();

    Map<String, CmdEvent> callbackEventMap = new ConcurrentHashMap<String, CmdEvent>();

    @Autowired
    private LockedBusinessService lockedBusinessService;

    @Autowired
    private ExecutorService exePoolService;

    @Resource
    RedisConfig redisConfig;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    CommandWatcher cmdWatcher;

    @Autowired
    AtomicRestClient atomicRestClient;

    @Autowired
    CommRestClient commRestClient;

    @Autowired
    private SignMessageFactory signMessageFactory;

    @PostConstruct
    public void init() {
        log.info("LPController init");
        log.info("start watchdog...");
        Thread watchdog = Thread.startVirtualThread(() -> {
            try {
                Thread.sleep(60000);
                log.error("Init failed to complete in 60 seconds, exiting application");
                Runtime.getRuntime().halt(1);
            } catch (InterruptedException e) {
                // Normal exit path - init completed successfully
            }
        });
        
        // redisConfig.getRedisTemplate().delete(KEY_CONFIG_CACHE);
        String configStr = "";
        try {
            configStr = (String) redisConfig.getRedisTemplate().opsForValue().get(KEY_CONFIG_CACHE);
        } catch (Exception e) {
            log.error("Application initialization failed", e);
            System.exit(1); // Non-zero exit code indicates failure
        }
        configStr = (configStr == null || configStr.isEmpty()) ? "{\"bridges\":[]}" : configStr;
        log.info("LPController configStr:" + configStr);
        try {
            LPConfigCache bridgesBox = objectMapper.readValue(configStr, LPConfigCache.class);
            log.info("LPController bridges:" + bridgesBox.toString());
            updateConfig(bridgesBox.getBridges(), false);
            cmdWatcher.watchCmds();
            exePoolService.submit(this.reportBridge(lpBridgeService.getLpBridgesChannelMap()));
            log.info("exit watchdog...");
            watchdog.interrupt();
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    public String printBridgeList() {
        try {
            String jsonOutput = objectMapper.writerWithDefaultPrettyPrinter()
                    .writeValueAsString(lpBridgeService.getLpBridgesChannelMap());
            return jsonOutput;
        } catch (Exception e) {
            return "";
        }
    }

    /**
     * from relay
     * 
     * @param quoteRemoveInfoList
     */
    public void onQuoteRemoved(List<QuoteRemoveInfo> quoteRemoveInfoList) {
        RedisTemplate redisTemplate = redisConfig.getRedisTemplate();
        for (QuoteRemoveInfo quoteRemoveInfo : quoteRemoveInfoList) {
            // LPBridge lpBridge =
            // lpBridges.get(quoteRemoveInfo.getQuoteBase().getBridge().getBridgeName());
            LPBridge lpBridge = getBridge(quoteRemoveInfo.getQuoteBase().getBridge().getBridgeName(),
                    quoteRemoveInfo.getQuoteBase().getRelayApiKey());
            CmdEvent cmdEvent = new CmdEvent().setQuoteRemoveInfo(quoteRemoveInfo).setCmd(CmdEvent.EVENT_QUOTE_REMOVER);

            try {
                redisTemplate.convertAndSend(lpBridge.getMsmqName(), cmdEvent);
            } catch (Exception e) {
                log.error("error", e);
            }
        }
    }

    public boolean updateConfig(List<LPBridge> bridges) {
        boolean updated = updateConfig(bridges, true);
        return updated;
    }

    public boolean updateConfig(List<LPBridge> bridges, boolean writeCache) {
        log.info("in coming bridge config, bridge size:{} ,write cache:{}", bridges.size(), writeCache);
        byte[][] channels = new byte[bridges.size() + 1][];
        int i = 0;
        for (LPBridge lpBridge : bridges) {
            String channelKey = lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey();
            lpBridgeService.addLPBridge(channelKey, lpBridge);
            channels[i] = channelKey.getBytes();
            i++;
        }
        if (writeCache) {
            try {
                redisConfig.getRedisTemplate().opsForValue().set(KEY_CONFIG_CACHE,
                        objectMapper.writeValueAsString(new LPConfigCache().setBridges(bridges)));
            } catch (Exception e) {
                log.error("error", e);
                return false;
            }
        }
        channels[i] = "SYSTEM_PING_CHANNEL".getBytes();
        log.info("channels size:{}", channels.length);
        log.info("channels:" + channels);
        cmdWatcher.updateWatch((byte[][]) channels);

        return true;
    }

    public void updateQuoteToRelay(QuoteData quoteData, LPBridge lpBridge) {
        lpBridges.put(lpBridge.getBridge().getBridgeName(), lpBridge);

        QuoteBase quoteBase = new QuoteBase().setBridge(lpBridge.getBridge())
                .setPrice(quoteData.getPrice())
                .setNativeTokenPrice(quoteData.getNativeTokenPrice())
                .setNativeTokenMax(quoteData.getNativeTokenMax())
                .setNativeTokenMin(quoteData.getNativeTokenMin())
                .setCapacity(quoteData.getCapacity())
                .setLpNodeUri(selfUri)
                .setLpBridgeAddress(lpBridge.getLpReceiverAddress())
                .setRelayApiKey(lpBridge.getRelayApiKey());

        List<QuoteBase> quotes = new ArrayList<QuoteBase>();
        quotes.add(quoteBase);
        commRestClient.doNotifyBridgeLive(quotes, lpBridge);
    }

    /**
     * from amm redis channel
     * 
     * @param cid
     * @param quoteData
     * @param lpBridge
     */
    public void askReplyToRelay(String cid, QuoteData quoteData, LPBridge lpBridge) {
        QuoteBase quoteBase = new QuoteBase().setBridge(lpBridge.getBridge())
                .setPrice(quoteData.getPrice())
                .setNativeTokenPrice(quoteData.getNativeTokenPrice())
                .setNativeTokenMax(quoteData.getNativeTokenMax())
                .setNativeTokenMin(quoteData.getNativeTokenMin())
                .setCapacity(quoteData.getCapacity())
                .setQuoteHash(quoteData.getQuoteHash())
                .setLpNodeUri(selfUri)
                
                .setLpBridgeAddress(lpBridge.getLpReceiverAddress());
        if (quoteBase.getBridge().getSrcChainId().equals(quoteBase.getBridge().getDstChainId())){
            quoteBase.setCapabilities("single_swap");
        }
        
        log.info("capabilities set value: {}", quoteBase.getCapabilities());

        RealtimeQuote realtimeQuote = new RealtimeQuote()
                .setQuoteBase(quoteBase)
                .setCid(cid);
        if (lpBridge.getAuthenticationLimiter() == null) {
            realtimeQuote.setAuthenticationLimiter(new QuoteAuthenticationLimiter().setLimiterState("off"));
        } else {
            realtimeQuote.setAuthenticationLimiter(lpBridge.getAuthenticationLimiter());
        }
        log.info("-> ask quote message realtimeQuote:{} lpBridge:{}", realtimeQuote, lpBridge);
        String objectResponseEntity = commRestClient.doNotifyRealtimeQuote(realtimeQuote, lpBridge);

        log.info(objectResponseEntity);
    }

    /**
     * from relay lock quote
     * * @param preBusiness
     * 
     * @return
     */
    public PreBusiness onRelayLockQuote(PreBusiness preBusiness) {
        System.out.println("PreBusiness: " + preBusiness);
        LPBridge lpBridge = getBridge(preBusiness.getSwapAssetInformation().getBridgeName(),
                preBusiness.getSwapAssetInformation().getQuote().getQuoteBase().getRelayApiKey());
        // check limit
        if (lpBridge.getAuthenticationLimiter().getLimiterState().equals("on")) {
            if (lpBridge.getAuthenticationLimiter().getCountryWhiteList().equals("")) {
                if (lpBridge.getAuthenticationLimiter().getCountryBlackList().toLowerCase()
                        .contains(preBusiness.getKycInfo().getCountry().toLowerCase())) {

                    preBusiness.setLocked(false);
                    return preBusiness;
                } else {
                    // pass
                }
            } else {
                if (lpBridge.getAuthenticationLimiter().getCountryWhiteList().toLowerCase()
                        .contains(preBusiness.getKycInfo().getCountry().toLowerCase())) {
                    // pass
                } else {
                    preBusiness.setLocked(false);
                    return preBusiness;
                }
            }
        }
        preBusiness.getSwapAssetInformation().getQuote().getQuoteBase().setCapabilities(null);
        CmdEvent cmdEvent = new CmdEvent().setPreBusiness(preBusiness).setCmd(CmdEvent.EVENT_LOCK_QUOTE);
        // send Event to Amm application
        try {
            String channel = lpBridge.getMsmqName() + "_" + lpBridge.getRelayApiKey();
            log.info("send LOCK message To {}", channel);
            redisConfig.getRedisTemplate().convertAndSend(channel,
                    cmdEvent);
        } catch (Exception e) {
            log.error("error", e);
        }
        long startTime = System.currentTimeMillis();
        long maxTimeout = 1000 * 20; // After a period of time, timeout
        long lastLogTime = System.currentTimeMillis(); // Record the time of the last log output
        long logInterval = 5000; // Set log output interval to 10 seconds in milliseconds
        // Wait callback
        CmdEvent callbackEvent = null;
        long executeStartTime = System.nanoTime();
        while (callbackEvent == null && (System.currentTimeMillis() - startTime) < maxTimeout) {
            if ((System.currentTimeMillis() - lastLogTime) >= logInterval) {
                log.info("Still waiting for callback event");
                lastLogTime = System.currentTimeMillis(); // Update the time of the last log output
            }
            try {
                Thread.sleep(100);
            } catch (Exception e) {
                Thread.currentThread().interrupt();
                log.error("callbackEvent check Loop interrupt" + e.toString());
            }
            String callbackKey = preBusiness.getHash() + "_" + CmdEvent.CALLBACK_LOCK_QUOTE;
            log.info("Check Lock response message:{} ", callbackKey);
            callbackEvent = callbackEventMap.get(callbackKey);
        }
        if (callbackEvent == null) {
            log.error("Obtain the event timeout from the callbackEventMap");
            return null;
        }
        long executeEndTime = System.nanoTime();
        long duration = executeEndTime - executeStartTime;
        double durationInMilliseconds = duration / 1_000_000.0;
        log.info("Wait LockCallback Execution time: " + durationInMilliseconds + " milliseconds");
        PreBusiness resultBusiness = callbackEvent.getPreBusiness();

        if (resultBusiness.getLocked() != true) {
            resultBusiness.setLocked(false);
            return resultBusiness;
        }
        Object signData = SignDataFactory.createSignData(
                preBusiness.getSwapAssetInformation().getSwapType(),
                preBusiness,
                lpBridge);
        AbstractSignMessage signMessage = signMessageFactory.createSignMessage(
                preBusiness.getSwapAssetInformation().getSwapType());
        signMessage.setSignData(signData)
                .setWalletName(lpBridge.getWallet().getName());

        String baseUri = String.format("%s/lpnode", lpBridge.getDstClientUri());

        String sign = signMessage.generateSign(baseUri, lpBridge.getBridge().getDstChainId());
        log.info("Generated sign: {}", sign);

        resultBusiness.getSwapAssetInformation().setLpSign(sign);
        String bidIdString = getBusinessId(resultBusiness, lpBridge);

        log.info(AddressHelper.getDecimalAddress(
                resultBusiness.getSwapAssetInformation().getQuote().getQuoteBase().getLpBridgeAddress(),
                lpBridge.getBridge().getSrcChainId()));
        log.info("Business Id Source str: " + bidIdString.toString());

        String businessHash = Hash.sha3String(bidIdString);
        resultBusiness.setHash(businessHash);
        // Persist to Redis with 12-hour expiration
        redisConfig.getRedisTemplate().opsForValue().set(KEY_LOCKED_BUSINESS + ":" + businessHash, "true", 12,
                TimeUnit.HOURS);
        lockedBusinessService.addLockedBusiness(resultBusiness.getHash());
        log.info("businessHash:" + resultBusiness.getHash().toString());
        log.info("Add business in cache 【lockedBusinessList】:" + resultBusiness.getHash());

        redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_APPEND, resultBusiness.getHash(),
                resultBusiness.getOrderAppendData());

        // log.info("Lock quote business result :{}", resultBusiness.toString());
        return resultBusiness;
    }

    public void newQuoteCallback(String key, CmdEvent cmdEvent) {
        log.info("Lock quote callback received - key: {}, eventType: {}, timestamp: {}",
                key,
                cmdEvent.getCmd(),
                System.currentTimeMillis());
        callbackEventMap.put(key, cmdEvent);
    }
}
