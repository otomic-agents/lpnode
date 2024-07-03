package com.bytetrade.obridge.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.math.BigInteger;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import org.web3j.crypto.Hash;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import com.bytetrade.obridge.component.client.request.CommandTransferInConfirm;
import com.bytetrade.obridge.component.client.request.CommandTransferIn;
import com.bytetrade.obridge.component.client.request.CommandTransferInRefund;
import com.bytetrade.obridge.component.client.request.Gas;
import com.bytetrade.obridge.component.client.request.RequestDoTransferIn;
import com.bytetrade.obridge.component.client.request.RequestDoTransferInConfirm;
import com.bytetrade.obridge.component.client.request.RequestDoTransferInRefund;
import com.bytetrade.obridge.component.client.request.RequestSignMessage712;
import com.bytetrade.obridge.component.client.request.RequestUUIDSolana;
import com.bytetrade.obridge.component.client.request.RequestSignMessage;
import com.bytetrade.obridge.component.client.request.SignData;
import com.bytetrade.obridge.component.client.request.SignMessageFactory;
import com.bytetrade.obridge.component.client.response.ResponseUUIDSolana;
import com.bytetrade.obridge.component.client.response.ResponseSignMessage;

import com.bytetrade.obridge.bean.EventTransferOutBox;
import com.bytetrade.obridge.bean.EventTransferInBox;
import com.bytetrade.obridge.bean.EventTransferConfirmBox;
import com.bytetrade.obridge.bean.EventTransferRefundBox;
import com.bytetrade.obridge.bean.EventTransferInConfirm;
import com.bytetrade.obridge.bean.EventTransferInRefund;
import com.bytetrade.obridge.bean.AskCmd;
import com.bytetrade.obridge.bean.BusinessFullData;
import com.bytetrade.obridge.bean.PreBusiness;
import com.bytetrade.obridge.bean.QuoteAuthenticationLimiter;
import com.bytetrade.obridge.bean.LPConfigCache;
import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.QuoteBase;
import com.bytetrade.obridge.bean.QuoteData;
import com.bytetrade.obridge.bean.CmdEvent;
import com.bytetrade.obridge.bean.QuoteRemoveInfo;
import com.bytetrade.obridge.bean.RealtimeQuote;
import com.bytetrade.obridge.db.redis.RedisConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;


@Slf4j
@Component
public class LPController {

    public static final String KEY_CONFIG_CACHE = "KEY_CONFIG_CACHE";
    public static final String KEY_BUSINESS_CACHE = "KEY_BUSINESS_CACHE";
    public static final String KEY_BUSINESS_ID_SHADOW = "KEY_BUSINESS_ID_SHADOW";
    public static final String KEY_BUSINESS_APPEND = "KEY_BUSINESS_APPEND";

    @Resource
    RedisConfig redisConfig;

    @Autowired
    ObjectMapper objectMapper;

    @Autowired
    RestTemplate restTemplate;

    @Autowired
    LPCommandWatcher cmdWatcher;

    @Autowired
    RestClient restClient;

    @Value("${relay.uri}")
	private String relayUri;

    @Value("${lpnode.uri}")
	private String selfUri;

    Map<String, LPBridge> lpBridges = new HashMap<String, LPBridge>();

    Map<String, LPBridge> lpBridgesChannelMap = new HashMap<String, LPBridge>();

    Map<String, CmdEvent> callbackEventMap = new HashMap<String, CmdEvent>();

    Map<String, Boolean> transferOutEventMap = new HashMap<String, Boolean>();

    Map<String, Boolean> transferOutConfirmEventMap = new HashMap<String, Boolean>();

    List<String> lockedBusinessList = new ArrayList<String>();

    List<String> transferOutIdList = new ArrayList<String>();

    public String getHexString(String numStr){

        if (numStr.startsWith("0x")) {
            return numStr;
        }

        return "0x" + new BigInteger(numStr).toString(16);
    }

    @PostConstruct
    public void init () {
        log.info("LPController init");
        
        String configStr = (String) redisConfig.getRedisTemplate().opsForValue().get(KEY_CONFIG_CACHE);
        log.info("LPController configStr:" + configStr);
        try {
            LPConfigCache bridgesBox = objectMapper.readValue(configStr, LPConfigCache.class);
            log.info("LPController bridges:" + bridgesBox.toString());
            updateConfig(bridgesBox.getBridges(), false);
        } catch (Exception e) {
            log.error("error", e);
        }

    }

    public static String getRandomString(int length){
        String str="abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
        Random random=new Random();
        StringBuffer sb=new StringBuffer();
        for(int i=0;i<length;i++){
        int number=random.nextInt(62);
        sb.append(str.charAt(number));
        }
        return sb.toString();
    }

    public LPBridge getBridgeFromChannel(String channel) {
        // log.info("lpBridgesChannelMap:", lpBridgesChannelMap.toString());
        return lpBridgesChannelMap.get(channel);
    }

    public void updateQuote(QuoteData quoteData, LPBridge lpBridge) {
        lpBridges.put(lpBridge.getBridge().getBridgeName(), lpBridge);

        QuoteBase quoteBase = new QuoteBase().setBridge(lpBridge.getBridge())
            .setPrice(quoteData.getPrice())
            .setNativeTokenPrice(quoteData.getNativeTokenPrice())
            .setNativeTokenMax(quoteData.getNativeTokenMax())
            .setNativeTokenMin(quoteData.getNativeTokenMin())
            .setCapacity(quoteData.getCapacity())
            .setLpNodeUri(selfUri)
            .setLpBridgeAddress(lpBridge.getLpReceiverAddress());
        
        // try {
        //     log.info("quoteBase:");
        //     log.info(objectMapper.writeValueAsString(quoteBase));
        // } catch (Exception e) {
        //     log.error("error", e);
        // }
        List<QuoteBase> quotes = new ArrayList<QuoteBase>();
        quotes.add(quoteBase);

        
        String objectResponseEntity = restClient.doNotifyBridgeLive(quotes, lpBridge);


        // log.info("response message:", objectResponseEntity);
    }

    public void askQuote(AskCmd askCmd) {
        log.info("on ask quote:" + askCmd.toString());
        LPBridge lpBridge = lpBridges.get(askCmd.getBridge());
        CmdEvent cmdEvent = new CmdEvent()
            .setCmd(CmdEvent.CMD_ASK_QUOTE)
            .setCid(askCmd.getCid())
            .setAmount(askCmd.getAmount());
        log.info("lpBridge:" + lpBridge.toString());
        log.info("cmdEvent:" + cmdEvent.toString());
        try {
            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName(), cmdEvent);
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    public void askReply(String cid, QuoteData quoteData, LPBridge lpBridge) {
        QuoteBase quoteBase = new QuoteBase().setBridge(lpBridge.getBridge())
            .setPrice(quoteData.getPrice())
            .setNativeTokenPrice(quoteData.getNativeTokenPrice())
            .setNativeTokenMax(quoteData.getNativeTokenMax())
            .setNativeTokenMin(quoteData.getNativeTokenMin())
            .setCapacity(quoteData.getCapacity())
            .setQuoteHash(quoteData.getQuoteHash())
            .setLpNodeUri(selfUri)
            .setLpBridgeAddress(lpBridge.getLpReceiverAddress());

        RealtimeQuote realtimeQuote = new RealtimeQuote()
            .setQuoteBase(quoteBase)
            .setCid(cid);
        if (lpBridge.getAuthenticationLimiter() == null) {
            realtimeQuote.setAuthenticationLimiter(new QuoteAuthenticationLimiter().setLimiterState("off"));
        } else {
            realtimeQuote.setAuthenticationLimiter(lpBridge.getAuthenticationLimiter());
        }
        
        
        String objectResponseEntity = restClient.doNotifyRealtimeQuote(realtimeQuote, lpBridge);

        log.info(objectResponseEntity);
    }

    public boolean updateConfig(List<LPBridge> bridges) {
        return updateConfig(bridges, true);
    }

    public boolean updateConfig(List<LPBridge> bridges, boolean writeCache) {

        cmdWatcher.exitWatch();
        byte[][] channels = new byte[bridges.size() + 1][];
        int i = 0;
        for (LPBridge lpBridge : bridges) {
            lpBridgesChannelMap.put(lpBridge.getMsmqName(), lpBridge);
            channels[i] = lpBridge.getMsmqName().getBytes();
            i++;
        }
        channels[i] = "SYSTEM_PING_CHANNEL".getBytes();
        log.info("channels:" + channels);
        cmdWatcher.watchCmds((byte[][]) channels, this);

        if(writeCache) {
            try {
                redisConfig.getRedisTemplate().opsForValue().set(KEY_CONFIG_CACHE, 
                    objectMapper.writeValueAsString(new LPConfigCache().setBridges(bridges)));
            } catch (Exception e) {
                log.error("error", e);
                return false;
            }
        }

        return true;
    }

    public void onQuoteRemoved(List<QuoteRemoveInfo> quoteRemoveInfoList) {

        RedisTemplate redisTemplate = redisConfig.getRedisTemplate();

        for(QuoteRemoveInfo quoteRemoveInfo : quoteRemoveInfoList) {
            LPBridge lpBridge = lpBridges.get(quoteRemoveInfo.getQuoteBase().getBridge().getBridgeName());
            CmdEvent cmdEvent = new CmdEvent().setQuoteRemoveInfo(quoteRemoveInfo).setCmd(CmdEvent.EVENT_QUOTE_REMOVER);

            try {
                redisTemplate.convertAndSend(lpBridge.getMsmqName(), cmdEvent);
            } catch (Exception e) {
                log.error("error", e);
            }
            
        }
    }

    public PreBusiness onLockQuote(PreBusiness preBusiness) {
        //call lp
        LPBridge lpBridge = lpBridges.get(preBusiness.getSwapAssetInformation().getBridgeName());


        //check limit
        if (lpBridge.getAuthenticationLimiter().getLimiterState().equals("on")){

            if(lpBridge.getAuthenticationLimiter().getCountryWhiteList().equals("")) {
                if (lpBridge.getAuthenticationLimiter().getCountryBlackList().toLowerCase().contains(preBusiness.getKycInfo().getCountry().toLowerCase())) {
                    
                    preBusiness.setLocked(false);
                    return preBusiness;
                } else {
                    //pass
                    
                }
            } else {
                if (lpBridge.getAuthenticationLimiter().getCountryWhiteList().toLowerCase().contains(preBusiness.getKycInfo().getCountry().toLowerCase())) {
                    //pass
                } else {
                    
                    preBusiness.setLocked(false);
                    return preBusiness;
                }
            }
        }
        

        CmdEvent cmdEvent = new CmdEvent().setPreBusiness(preBusiness).setCmd(CmdEvent.EVENT_LOCK_QUOTE);
        try {
            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName(), cmdEvent);
        } catch (Exception e) {
            log.error("error", e);
        }
        long startTime = System.currentTimeMillis();
        long maxTimeout = 1000 * 60 * 3; // After a period of time, timeout
        long lastLogTime = System.currentTimeMillis(); // Record the time of the last log output
        long logInterval = 10000; // Set log output interval to 10 seconds in milliseconds
        // wait callback
        CmdEvent callbackEvent = null;
        while (callbackEvent == null && (System.currentTimeMillis() - startTime) < maxTimeout) {
            if ((System.currentTimeMillis() - lastLogTime) >= logInterval) {
                log.info("Still waiting for callback event");
                lastLogTime = System.currentTimeMillis(); // Update the time of the last log output
            }
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("error", e);
            }
            
            log.info("check key:" + preBusiness.getHash() + "_" + CmdEvent.CALLBACK_LOCK_QUOTE);
            callbackEvent = callbackEventMap.get(preBusiness.getHash() + "_" + CmdEvent.CALLBACK_LOCK_QUOTE);
        }

        PreBusiness resultBusiness = callbackEvent.getPreBusiness();

        // lp salt -> relay salt
        // //create Salt
        // log.info(callbackEvent.toString());
        // String salt = getRandomString(10);
        // log.info("salt:" + salt);
        // resultBusiness.setLpSalt(salt);
        

        if(resultBusiness.getLocked() == true) {

            SignData signData = new SignData()
                .setSrcChainId(lpBridge.getBridge().getSrcChainId())
                .setSrcAddress(lpBridge.getLpReceiverAddress())
                .setSrcToken(lpBridge.getBridge().getSrcToken())
                .setSrcAmount(resultBusiness.getSwapAssetInformation().getAmount())
                .setDstChainId(lpBridge.getBridge().getDstChainId())
                .setDstAddress(resultBusiness.getSwapAssetInformation().getDstAddress())
                .setDstToken(lpBridge.getBridge().getDstToken())
                .setDstAmount(resultBusiness.getSwapAssetInformation().getDstAmount())
                .setDstNativeAmount(resultBusiness.getSwapAssetInformation().getDstNativeAmount())
                .setRequestor(resultBusiness.getSwapAssetInformation().getRequestor())
                .setLpId(lpBridge.getLpId())
                .setStepTimeLock(resultBusiness.getSwapAssetInformation().getStepTimeLock())
                .setAgreementReachedTime(resultBusiness.getSwapAssetInformation().getAgreementReachedTime());

            RequestSignMessage request = SignMessageFactory.createSignMessage(lpBridge.getBridge().getDstChainId())
                    .setSignData(signData).setWalletName(lpBridge.getWallet().getName());



            String uri = String.format("%s/lpnode%s", lpBridge.getDstClientUri(),
                    this.getSignMessageSubPath(lpBridge.getBridge().getDstChainId()));
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            System.out.println("Signature data:");
            System.out.println(gson.toJson(request));
            log.info(String.format("request signature url is:%s", uri));
            ResponseSignMessage objectResponseEntity = restTemplate.postForObject(
                    uri,
                    request,
                    ResponseSignMessage.class);

            resultBusiness.getSwapAssetInformation().setLpSign(objectResponseEntity.getSigned());
            log.info("response message:", objectResponseEntity);
            log.info(String.format("The signature of lp has been completed,signData: %s",
                    objectResponseEntity.getSigned()));
            
            // TODO FIXME new bidid
            // bidid
            System.out.println("result business:");
            System.out.println(gson.toJson(resultBusiness));
            
            String bidIdString = 
                resultBusiness.getSwapAssetInformation().getAgreementReachedTime().toString() +
                lpBridge.getBridge().getSrcChainId().toString() +
                // new BigInteger(resultBusiness.getSwapAssetInformation().getSender().substring(2), 16).toString() + 
                AddressHelper.getDecimalAddress(resultBusiness.getSwapAssetInformation().getQuote().getQuoteBase().getLpBridgeAddress(), lpBridge.getBridge().getSrcChainId()) +
                lpBridge.getBridge().getSrcToken().toString() + 
                lpBridge.getBridge().getDstChainId().toString() + 
                new BigInteger(resultBusiness.getSwapAssetInformation().getDstAddress().substring(2), 16).toString() +
                lpBridge.getBridge().getDstToken().toString() + 
                resultBusiness.getSwapAssetInformation().getAmount().toString() + 
                resultBusiness.getSwapAssetInformation().getDstAmount().toString() +
                resultBusiness.getSwapAssetInformation().getDstNativeAmount().toString() +
                resultBusiness.getSwapAssetInformation().getRequestor().toString() +
                lpBridge.getLpId() + 
                resultBusiness.getSwapAssetInformation().getStepTimeLock().toString() +
                resultBusiness.getSwapAssetInformation().getUserSign().toString() +
                resultBusiness.getSwapAssetInformation().getLpSign().toString();
            log.info(AddressHelper.getDecimalAddress(resultBusiness.getSwapAssetInformation().getQuote().getQuoteBase().getLpBridgeAddress(), lpBridge.getBridge().getSrcChainId()));
            log.info("bidIdString:" + bidIdString.toString());

            String businessHash = Hash.sha3String(bidIdString);
            resultBusiness.setHash(businessHash);


            lockedBusinessList.add(resultBusiness.getHash());
            log.info("add business in cache:" + resultBusiness.getHash());
        }
        

        redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_APPEND, resultBusiness.getHash(), resultBusiness.getOrderAppendData());


        return resultBusiness;
    }
    private String getSignMessageSubPath(Integer chainId) {
        String path = "";
        switch (chainId) {
            case 9006:
            case 9000:
            case 60:
            case 614:
            case 966:
            case 397:
                path = "/sign_message_712";
                break;
            case 501:
                path = "/sign_message";
                break;
            default:
                throw new IllegalArgumentException("Invalid chainId");
        }
        return path;
    }
    public Boolean onEventTransferOut(EventTransferOutBox eventBox) {

        String bidId = getHexString(eventBox.getEventParse().getBidId());
        log.info("onEventTransferOut:" + bidId);
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        for(String businessId : lockedBusinessList) {
            if(businessId.equalsIgnoreCase(bidId)){
                transferOutEventMap.put(businessId, true);
                log.info("add transferOutId:" + eventBox.getEventParse().getTransferId());
                System.out.println(gson.toJson(eventBox));
                transferOutIdList.add(eventBox.getEventParse().getTransferId());
                lockedBusinessList.remove(businessId);
                break;
            }
        }
        return true;
    }

    public Boolean onTransferOut(BusinessFullData bfd) {

        LPBridge lpBridge = lpBridges.get(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName());
        bfd.getPreBusiness().setOrderAppendData( (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_APPEND, bfd.getPreBusiness().getHash()));
        CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_OUT);
        try {
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE, bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));

            long startTime = System.currentTimeMillis();
            long maxTimeout = 1000 * 60 * 3; // After a period of time, timeout
            long lastLogTime = System.currentTimeMillis(); // Record the time of the last log output
            long logInterval = 10000; // Set log output interval to 10 seconds in milliseconds
            Boolean doubleCheck = false;
            
            while (!doubleCheck && (System.currentTimeMillis() - startTime) < maxTimeout) {
                Boolean hit = transferOutEventMap.get(getHexString(bfd.getEventTransferOut().getBidId()));
                doubleCheck = hit != null && hit == true;
                // Check if logging is required based on the specified interval
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

            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName(), cmdEvent);
        } catch (Exception e) {
            log.error("error", e);
            return false;
        }

        return true;
    }

    public void newCallback(String key, CmdEvent cmdEvent) {
        log.info("newCallback:" + key);
        callbackEventMap.put(key, cmdEvent);
    }

    public void transferIn(BusinessFullData bfd, LPBridge lpBridge) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        log.info("do transfer in");
        CommandTransferIn commandTransferIn = new CommandTransferIn()
            .setSenderWalletName(lpBridge.getWallet().getName())
            .setUserReceiverAddress(bfd.getEventTransferOut().getDstAddress())
            .setToken(bfd.getEventTransferOut().getDstToken())
            .setTokenAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstAmountNeed())
            .setEthAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstNativeAmountNeed())
            .setStepTimeLock(bfd.getEventTransferOut().getStepTimeLock())
            .setAgreementReachedTime(bfd.getEventTransferOut().getAgreementReachedTime())
            .setSrcChainId(lpBridge.getBridge().getSrcChainId())
            .setSrcTransferId(bfd.getEventTransferOut().getTransferId())
            .setAppendInformation(bfd.getPreBusiness().getSwapAssetInformation().getAppendInformation());

        Integer dstChainId = lpBridge.getBridge().getDstChainId();
        if(dstChainId == 9000 || dstChainId == 9006 || dstChainId == 60 || dstChainId == 966){
            commandTransferIn.setHashLock(bfd.getPreBusiness().getHashlockEvm());
        } else if (dstChainId == 397) {
            commandTransferIn.setHashLock(bfd.getPreBusiness().getHashlockNear());
        } else if (dstChainId == 144) {
            commandTransferIn.setHashLock(bfd.getPreBusiness().getHashlockXrp());
        } else if (dstChainId == 501) {
            commandTransferIn.setHashLock(bfd.getPreBusiness().getHashlockSolana());
        }
        if (dstChainId == 501) {
            String uuidUri = lpBridge.getDstClientUri() + "/lpnode/get_uuid";
            RequestUUIDSolana requestUUIDSolana = new RequestUUIDSolana();
            ResponseUUIDSolana responseUUIDSolana = restTemplate.postForObject(
                    uuidUri,
                    requestUUIDSolana,
                    ResponseUUIDSolana.class);
            log.info(String.format("response a uuid:%s", responseUUIDSolana.getUuid()));
            commandTransferIn.setUuid(responseUUIDSolana.getUuid());
        }
        String json = gson.toJson(bfd);
        System.out.println(json);
        System.out.println(commandTransferIn);
        redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_ID_SHADOW, commandTransferIn.getHashLock(), commandTransferIn.getSrcTransferId());
        RequestDoTransferIn request = new RequestDoTransferIn()
                .setTransactionType("LOCAL_PADDING")
                .setCommandTransferIn(commandTransferIn);
        
        if (ChainSetting.getInstance().needGasSetting(dstChainId)) {
            Gas gas = new Gas().setGasPrice(Gas.GAS_PRICE_TYPE_FAST);
            request.setGas(gas);
        }

        String requestJson = gson.toJson(request);
        System.out.println("send request to trade [transferIn]");
        System.out.println(requestJson);

        try {
            log.info("RequestDoTransferIn:" + objectMapper.writeValueAsString(request));
        } catch (JsonProcessingException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        String uri = lpBridge.getDstClientUri() + "/lpnode/transfer_in";
        log.info("uri:" + uri);
        String objectResponseEntity = restTemplate.postForObject(
            uri,
            request,
            String.class
        );

        log.info("response message:", objectResponseEntity);
    }

    public void onTransferIn(EventTransferInBox eventBox) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println("onTransferIn:");
        System.out.println(gson.toJson(eventBox));

        try {
            if(eventBox.getMatchingHashlock() != null && eventBox.getMatchingHashlock() == true) {
                //matching hashlock (XRP) 
                String srcTransferId = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_ID_SHADOW, eventBox.getEventParse().getHashLockOriginal());
                eventBox.getEventParse().setSrcTransferId(srcTransferId);
            }

            //fetch business
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE, eventBox.getEventParse().getSrcTransferId());
            BusinessFullData bfd = objectMapper.readValue(cacheData, BusinessFullData.class);
            if(eventBox.getMatchingHashlock() != null && eventBox.getMatchingHashlock() == true) {
                //sync business
                LPBridge lpBridge = lpBridges.get(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName());
                eventBox.getEventParse().setSrcChainId(lpBridge.getBridge().getSrcChainId());
            }
            bfd.getPreBusiness().setInTradeUuid(eventBox.getEventParse().getUuid());
            //update cache
            eventBox.getEventParse().setTransferInfo(eventBox.getTransferInfo());
            bfd.setEventTransferIn(eventBox.getEventParse());
            
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE, bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE, eventBox.getEventParse().getTransferId(), objectMapper.writeValueAsString(bfd));

            log.info("TransferIn:" + bfd.getEventTransferIn());

            
            log.info("bfd:" + objectMapper.writeValueAsString(bfd));
            
            //call relay
            LPBridge lpBridge = lpBridges.get(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName());
            
            CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_IN);
            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName(), cmdEvent);

            
            String objectResponseEntity = restClient.doNotifyTransferIn(lpBridge, bfd);

            log.info("response message:", objectResponseEntity);
        } catch (Exception e) {
            log.error("error", e);
        }
    }

    private void onEventConfirm(EventTransferConfirmBox eventBox) {
        String transferId = eventBox.getEventParse().getTransferId();
        for(String tId : transferOutIdList) {
            if(tId.equalsIgnoreCase(transferId)){
                transferOutConfirmEventMap.put(transferId, true);

                transferOutIdList.remove(transferId);
                break;
            }
        }
    }

    public Boolean onTransferOutConfirm(BusinessFullData bfdFromRelay) {
        LPBridge lpBridge = lpBridges.get(bfdFromRelay.getPreBusiness().getSwapAssetInformation().getBridgeName());

        try {
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE, bfdFromRelay.getEventTransferOut().getTransferId());
            BusinessFullData bfd = objectMapper.readValue(cacheData, BusinessFullData.class);
            bfd.setEventTransferOutConfirm(bfdFromRelay.getEventTransferOutConfirm());
            bfd.setPreBusiness(bfdFromRelay.getPreBusiness());
            bfd.getPreBusiness().setOrderAppendData( (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_APPEND, bfd.getPreBusiness().getHash()));
            CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_OUT_CONFIRM);

            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE, bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE, bfd.getEventTransferIn().getTransferId(), objectMapper.writeValueAsString(bfd));

            Boolean doubleCheck = false;
            while(!doubleCheck) {
                Boolean hit = transferOutConfirmEventMap.get(bfd.getEventTransferOutConfirm().getTransferId());
                doubleCheck = hit != null && hit == true;

                try {
                    Thread.sleep(500);
                } catch (Exception e) {
                    log.error("error", e);
                }
            }

            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName(), cmdEvent);
        } catch (Exception e) {
            log.error("error", e);
            return false;
        }

        return true;
    }

    public void transferInConfirm(BusinessFullData bfd, LPBridge lpBridge){
        log.info("do transfer in confirm");
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println("do transfer in confirm:");
        System.out.println(gson.toJson(bfd));
        
        CommandTransferInConfirm commandTransferInConfirm = new CommandTransferInConfirm()
            .setUuid(bfd.getEventTransferIn().getTransferId())
            .setSenderWalletName(lpBridge.getWallet().getName())
            .setUserReceiverAddress(bfd.getEventTransferOut().getDstAddress())
            .setToken(bfd.getEventTransferOut().getDstToken())
            .setTokenAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstAmountNeed())
            .setEthAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstNativeAmountNeed())
            .setHashLock(bfd.getEventTransferOut().getHashLock())
            .setStepTimeLock(bfd.getEventTransferOut().getStepTimeLock())
            .setAgreementReachedTime(bfd.getEventTransferOut().getAgreementReachedTime())
            .setPreimage(bfd.getEventTransferOutConfirm().getPreimage())
            .setAppendInformation(bfd.getPreBusiness().getSwapAssetInformation().getAppendInformation())
            .setTransferId(bfd.getEventTransferIn().getTransferId());
        Gas gas = new Gas().setGasPrice(Gas.GAS_PRICE_TYPE_STANDARD);
        RequestDoTransferInConfirm request = new RequestDoTransferInConfirm()
            .setTransactionType("LOCAL_PADDING")
            .setCommandTransferInConfirm(commandTransferInConfirm)
            .setGas(gas);

        Integer dstChainId = lpBridge.getBridge().getDstChainId();
        if(dstChainId == 9000 || dstChainId == 9006 || dstChainId == 60 || dstChainId == 966){
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
            String.class
        );

        log.info("response message:", objectResponseEntity);
    }

    public void onConfirm(EventTransferConfirmBox eventBox) {
        try {
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE, eventBox.getEventParse().getTransferId());
            BusinessFullData bfd = objectMapper.readValue(cacheData, BusinessFullData.class);

            //Divert TransferOutConfirm and TransferInConfirm
            if(!eventBox.getEventParse().getTransferId().equalsIgnoreCase(bfd.getEventTransferIn().getTransferId())){
                log.info("not hit Transfer in");
                log.info("bfd id:" + bfd.getEventTransferIn().getTransferId());
                log.info("event id:" + eventBox.getEventParse().getTransferId());
                log.info("EventTransferConfirmBox:" + eventBox.toString());
                log.info("BusinessFullData:" + bfd.toString());
                onEventConfirm(eventBox);
                return;
            }

            //update cache
            EventTransferInConfirm eventTransferInConfirm = new EventTransferInConfirm()
                .setBusinessId(bfd.getBusiness().getBusinessId())
                .setTransferInfo(eventBox.getTransferInfo())
                .setTransferId(eventBox.getEventParse().getTransferId())
                .setPreimage(eventBox.getEventParse().getPreimage());
            bfd.setEventTransferInConfirm(eventTransferInConfirm);
            
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE, bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE, eventBox.getEventParse().getTransferId(), objectMapper.writeValueAsString(bfd));

            log.info("TransferInConfirm:" + bfd.getEventTransferIn());

            log.info("bfd:" + objectMapper.writeValueAsString(bfd));
            


            //call relay
            LPBridge lpBridge = lpBridges.get(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName());

            CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_IN_CONFIRM);
            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName(), cmdEvent);
            
            
            String objectResponseEntity = restClient.doNotifyTransferInConfirm(lpBridge, bfd);

            log.info("response message:", objectResponseEntity);
        } catch (Exception e) {
            log.error("error", e);
            return ;
        }
    }

    public Boolean onTransferOutRefund(BusinessFullData bfdFromRelay) {
        LPBridge lpBridge = lpBridges.get(bfdFromRelay.getPreBusiness().getSwapAssetInformation().getBridgeName());

        try {
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE, bfdFromRelay.getEventTransferOut().getTransferId());
            BusinessFullData bfd = objectMapper.readValue(cacheData, BusinessFullData.class);
            bfd.setEventTransferOutRefund(bfdFromRelay.getEventTransferOutRefund());
            bfd.setPreBusiness(bfdFromRelay.getPreBusiness());
            bfd.getPreBusiness().setOrderAppendData( (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_APPEND, bfd.getPreBusiness().getHash()));
            CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_OUT_REFUND);

            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE, bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE, bfd.getEventTransferIn().getTransferId(), objectMapper.writeValueAsString(bfd));

            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName(), cmdEvent);
        } catch (Exception e) {
            log.error("error", e);
            return false;
        }

        return true;
    }

    public void transferInRefund(BusinessFullData bfd, LPBridge lpBridge){

        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        System.out.println("transferInRefund info:");
        System.out.println(gson.toJson(bfd));
        log.info("do transfer in refund");
        CommandTransferInRefund commandTransferInRefund = new CommandTransferInRefund()
            .setUuid(bfd.getEventTransferIn().getTransferId())
            .setSenderWalletName(lpBridge.getWallet().getName())
            .setUserReceiverAddress(bfd.getEventTransferOut().getDstAddress())
            .setToken(bfd.getEventTransferOut().getDstToken())
            .setTokenAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstAmountNeed())
            .setEthAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstNativeAmountNeed())
            .setHashLock(bfd.getEventTransferOut().getHashLock())
            .setStepTimeLock(bfd.getEventTransferOut().getStepTimeLock())
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
            String.class
        );

        log.info("response message:", objectResponseEntity);
    }

    public void onRefund(EventTransferRefundBox eventBox) {
        try {
            String cacheData = (String) redisConfig.getRedisTemplate().opsForHash().get(KEY_BUSINESS_CACHE, eventBox.getEventParse().getTransferId());
            BusinessFullData bfd = objectMapper.readValue(cacheData, BusinessFullData.class);

            if(!eventBox.getEventParse().getTransferId().equalsIgnoreCase(bfd.getEventTransferIn().getTransferId())){
                log.info("not hit Transfer in");
                log.info("bfd id:" + bfd.getEventTransferIn().getTransferId());
                log.info("event id:" + eventBox.getEventParse().getTransferId());
                log.info("EventTransferConfirmBox:", eventBox.toString());
                log.info("BusinessFullData:", bfd.toString());
                return;
            }
            //update cache
            EventTransferInRefund eventTransferInRefund = new EventTransferInRefund()
                .setBusinessId(bfd.getBusiness().getBusinessId())
                .setTransferInfo(eventBox.getTransferInfo())
                .setTransferId(eventBox.getEventParse().getTransferId());
            bfd.setEventTransferInRefund(eventTransferInRefund);
            
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE, bfd.getEventTransferOut().getTransferId(), objectMapper.writeValueAsString(bfd));
            redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_CACHE, eventBox.getEventParse().getTransferId(), objectMapper.writeValueAsString(bfd));

            log.info("TransferInRefund:" + bfd.getEventTransferIn());
            
            log.info("bfd:" + objectMapper.writeValueAsString(bfd));
            

            //call relay
            LPBridge lpBridge = lpBridges.get(bfd.getPreBusiness().getSwapAssetInformation().getBridgeName());

            CmdEvent cmdEvent = new CmdEvent().setBusinessFullData(bfd).setCmd(CmdEvent.EVENT_TRANSFER_IN_REFUND);
            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName(), cmdEvent);
            
            
            String objectResponseEntity = restClient.doNotifyTransferInRefund(lpBridge, bfd);

            log.info("response message:", objectResponseEntity);
        } catch (Exception e) {
            log.error("error", e);
            return ;
        }
    }

}