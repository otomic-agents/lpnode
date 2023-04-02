package com.bytetrade.obridge.component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.Random;
import java.math.BigInteger;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
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
import com.bytetrade.obridge.bean.EventTransferInConfirm;
import com.bytetrade.obridge.bean.EventTransferInRefund;
import com.bytetrade.obridge.bean.AskCmd;
import com.bytetrade.obridge.bean.BusinessFullData;
import com.bytetrade.obridge.bean.PreBusiness;
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

        String objectResponseEntity = restTemplate.postForObject(
            relayUri + "/lpnode/" + lpBridge.getRelayApiKey() + "/quote_and_live_confirmation", 
            quotes, String.class);


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

        String objectResponseEntity = restTemplate.postForObject(
            relayUri + "/lpnode/" + lpBridge.getRelayApiKey() + "/realtime_quote", 
            realtimeQuote, String.class);

        log.info(objectResponseEntity);
    }

    public boolean updateConfig(List<LPBridge> bridges) {
        return updateConfig(bridges, true);
    }

    public boolean updateConfig(List<LPBridge> bridges, boolean writeCache) {

        cmdWatcher.exitWatch();
        byte[][] channels = new byte[bridges.size()][];
        int i = 0;
        for(LPBridge lpBridge : bridges) {
            lpBridgesChannelMap.put(lpBridge.getMsmqName(), lpBridge);
            channels[i] = lpBridge.getMsmqName().getBytes();
            i++;
        }
        log.info("channels:" + channels);
        cmdWatcher.watchCmds((byte[][])channels, this);

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
        CmdEvent cmdEvent = new CmdEvent().setPreBusiness(preBusiness).setCmd(CmdEvent.EVENT_LOCK_QUOTE);
        try {
            redisConfig.getRedisTemplate().convertAndSend(lpBridge.getMsmqName(), cmdEvent);
        } catch (Exception e) {
            log.error("error", e);
        }

        //wait callback
        CmdEvent callbackEvent = null;
        while(callbackEvent == null) {
            try {
                Thread.sleep(1000);
            } catch (Exception e) {
                log.error("error", e);
            }
            
            log.info("check key:" + preBusiness.getHash() + "_" + CmdEvent.CALLBACK_LOCK_QUOTE);
            callbackEvent = callbackEventMap.get(preBusiness.getHash() + "_" + CmdEvent.CALLBACK_LOCK_QUOTE);
        }

        //create Salt
        log.info(callbackEvent.toString());
        String salt = getRandomString(10);
        log.info("salt:" + salt);

        PreBusiness resultBusiness = callbackEvent.getPreBusiness();

        if(resultBusiness.getLocked() == true) {
            lockedBusinessList.add(resultBusiness.getHash());
            log.info("add business in cache:" + resultBusiness.getHash());
        }
        resultBusiness.setLpSalt(salt);

        redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_APPEND, resultBusiness.getHash(), resultBusiness.getOrderAppendData());


        return resultBusiness;
    }

    public Boolean onEventTransferOut(EventTransferOutBox eventBox) {

        String bidId = getHexString(eventBox.getEventParse().getBidId());
        log.info("onEventTransferOut:" + bidId);
        for(String businessId : lockedBusinessList) {
            if(businessId.equalsIgnoreCase(bidId)){
                transferOutEventMap.put(businessId, true);

                log.info("add transferOutId:" + eventBox.getEventParse().getTransferId());
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

            Boolean doubleCheck = false;
            while(!doubleCheck) {
                Boolean hit = transferOutEventMap.get(getHexString(bfd.getEventTransferOut().getBidId()));
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

    public void newCallback(String key, CmdEvent cmdEvent) {
        log.info("newCallback:" + key);
        callbackEventMap.put(key, cmdEvent);
    }

    public void transferIn(BusinessFullData bfd, LPBridge lpBridge) {

        log.info("do transfer in");
        CommandTransferIn commandTransferIn = new CommandTransferIn()
            .setSenderWalletName(lpBridge.getWallet().getName())
            .setUserReceiverAddress(bfd.getEventTransferOut().getDstAddress())
            .setToken(bfd.getEventTransferOut().getDstToken())
            .setTokenAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstAmountNeed())
            .setEthAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstNativeAmountNeed())
            .setTimeLock(bfd.getEventTransferOut().getTimeLock())
            .setSrcChainId(lpBridge.getBridge().getSrcChainId())
            .setSrcTransferId(bfd.getEventTransferOut().getTransferId())
            .setAppendInformation(bfd.getPreBusiness().getSwapAssetInformation().getAppendInformation());

        if(lpBridge.getBridge().getDstChainId() == 9000 || lpBridge.getBridge().getDstChainId() == 9006){
            commandTransferIn.setHashLock(bfd.getPreBusiness().getHashlockEvm());
        } else if (lpBridge.getBridge().getDstChainId() == 397) {
            commandTransferIn.setHashLock(bfd.getPreBusiness().getHashlockNear());
        } else if (lpBridge.getBridge().getDstChainId() == 144) {
            commandTransferIn.setHashLock(bfd.getPreBusiness().getHashlockXrp());
        }
        
        redisConfig.getRedisTemplate().opsForHash().put(KEY_BUSINESS_ID_SHADOW, commandTransferIn.getHashLock(), commandTransferIn.getSrcTransferId());

        Gas gas = new Gas().setGasPrice(Gas.GAS_PRICE_TYPE_STANDARD);
        RequestDoTransferIn request = new RequestDoTransferIn()
            .setTransactionType("LOCAL_PADDING")
            .setCommandTransferIn(commandTransferIn)
            .setGas(gas);

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

            String objectResponseEntity = restTemplate.postForObject(
                relayUri + "/lpnode/" + lpBridge.getRelayApiKey() + "/on_transfer_in", 
                bfd, String.class);

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
        CommandTransferInConfirm commandTransferInConfirm = new CommandTransferInConfirm()
            .setSenderWalletName(lpBridge.getWallet().getName())
            .setUserReceiverAddress(bfd.getEventTransferOut().getDstAddress())
            .setToken(bfd.getEventTransferOut().getDstToken())
            .setTokenAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstAmountNeed())
            .setEthAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstNativeAmountNeed())
            .setHashLock(bfd.getEventTransferOut().getHashLock())
            .setTimeLock(bfd.getEventTransferOut().getTimeLock())
            .setPreimage(bfd.getEventTransferOutConfirm().getPreimage())
            .setAppendInformation(bfd.getPreBusiness().getSwapAssetInformation().getAppendInformation())
            .setTransferId(bfd.getEventTransferIn().getTransferId());
        Gas gas = new Gas().setGasPrice(Gas.GAS_PRICE_TYPE_STANDARD);
        RequestDoTransferInConfirm request = new RequestDoTransferInConfirm()
            .setTransactionType("LOCAL_PADDING")
            .setCommandTransferInConfirm(commandTransferInConfirm)
            .setGas(gas);

        if(lpBridge.getBridge().getDstChainId() == 9000 || lpBridge.getBridge().getDstChainId() == 9006){
            commandTransferInConfirm.setHashLock(bfd.getPreBusiness().getHashlockEvm());
        } else if (lpBridge.getBridge().getDstChainId() == 397) {
            commandTransferInConfirm.setHashLock(bfd.getPreBusiness().getHashlockNear());
        } else if (lpBridge.getBridge().getDstChainId() == 144) {
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
            
            String objectResponseEntity = restTemplate.postForObject(
                relayUri + "/lpnode/" + lpBridge.getRelayApiKey() + "/on_transfer_in_confirm", 
                bfd, String.class);

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
        log.info("do transfer in refund");
        CommandTransferInRefund commandTransferInRefund = new CommandTransferInRefund()
            .setSenderWalletName(lpBridge.getWallet().getName())
            .setUserReceiverAddress(bfd.getEventTransferOut().getDstAddress())
            .setToken(bfd.getEventTransferOut().getDstToken())
            .setTokenAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstAmountNeed())
            .setEthAmount(bfd.getPreBusiness().getSwapAssetInformation().getDstNativeAmountNeed())
            .setHashLock(bfd.getEventTransferOut().getHashLock())
            .setTimeLock(bfd.getEventTransferOut().getTimeLock())
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
            
            String objectResponseEntity = restTemplate.postForObject(
                relayUri + "/lpnode/" + lpBridge.getRelayApiKey() + "/on_transfer_in_refund", 
                bfd, String.class);

            log.info("response message:", objectResponseEntity);
        } catch (Exception e) {
            log.error("error", e);
            return ;
        }
    }
}