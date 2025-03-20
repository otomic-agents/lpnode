package com.bytetrade.obridge.component.controller;

import java.math.BigInteger;
import java.util.Map;

import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import com.bytetrade.obridge.bean.ExtendedAtomicSwapAsset;
import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.PreBusiness;
import com.bytetrade.obridge.bean.SwapAssetInformation;
import com.bytetrade.obridge.bean.SingleSwap.ExtendedSingleSwapAsset;
import com.bytetrade.obridge.component.service.LPBridgeService;
import com.bytetrade.obridge.component.utils.AddressHelper;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LpControllerBase {

    protected static final String KEY_BUSINESS_EVENT = "KEY_BUSINESS_EVENT";
    protected static final String KEY_CONFIG_CACHE = "KEY_CONFIG_CACHE";
    protected static final String KEY_BUSINESS_CACHE = "KEY_BUSINESS_CACHE";
    public static final String KEY_BUSINESS_ID_SHADOW = "KEY_BUSINESS_ID_SHADOW";
    protected static final String KEY_BUSINESS_APPEND = "KEY_BUSINESS_APPEND";
    protected static final String KEY_LOCKED_BUSINESS = "KEY_LOCKED_BUSINESS"; // Redis key for locked business hashes
    @Autowired
    protected LPBridgeService lpBridgeService;

    public String getHexString(String numStr) {

        if (numStr.startsWith("0x")) {
            return numStr;
        }

        return "0x" + new BigInteger(numStr).toString(16);
    }

    public String normalizeHex(String hexStr) {
        if (hexStr.startsWith("0x")) {
            return hexStr;
        }
        return "0x" + hexStr;
    }

    protected LPBridge getBridge(String bridgeName, String relayApiKey) {
        if (relayApiKey == null) {
            log.info("🔑 Invalid API Key: null");
            return null;
        }
        if (relayApiKey.length() <= 3) {
            log.info("⚠️ API Key too short: {}", relayApiKey);
            return null;
        }
        // log.info("✅ Valid API Key: {}", relayApiKey);
        String[] parts = bridgeName.split("_");
        String key = parts[2] + "/" + parts[3] + "_" + parts[0] + "_" + parts[1];
        LPBridge lpBridge = lpBridgeService.getLPBridge(key + "_" + relayApiKey);
        return lpBridge;
    }

    protected Runnable reportBridge(Map<String, LPBridge> lpBridgesChannelMap) {
        Runnable printKeysTask = () -> {
            try {
                while (!Thread.currentThread().isInterrupted()) {
                    System.out.println("Current bridges channel");
                    lpBridgesChannelMap.keySet().forEach(System.out::println);
                    Thread.sleep(60000);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                System.out.println("Task was interrupted.");
            }
        };
        return printKeysTask;

    }

    protected String getBusinessId(PreBusiness resultBusiness, LPBridge lpBridge) {
        SwapAssetInformation swapAsset = resultBusiness.getSwapAssetInformation();

        JSONObject bidComponents = new JSONObject();

        bidComponents.put("agreementReachedTime", swapAsset.getAgreementReachedTime().toString());
        bidComponents.put("srcChainId", lpBridge.getBridge().getSrcChainId().toString());
        bidComponents.put("srcAddress", AddressHelper.getDecimalAddress(
                swapAsset.getQuote().getQuoteBase().getLpBridgeAddress(),
                swapAsset.getQuote().getQuoteBase().getBridge().getSrcChainId()));
        bidComponents.put("srcToken", lpBridge.getBridge().getSrcToken().toString());
        bidComponents.put("dstChainId", lpBridge.getBridge().getDstChainId().toString());
        bidComponents.put("dstAddress", AddressHelper.getDecimalAddress(
                swapAsset.getDstAddress(),
                swapAsset.getQuote().getQuoteBase().getBridge().getDstChainId()));
        bidComponents.put("dstToken", lpBridge.getBridge().getDstToken().toString());
        bidComponents.put("srcAmount", swapAsset.getAmount().toString());
        bidComponents.put("dstAmount", swapAsset.getDstAmount().toString());
        bidComponents.put("dstNativeAmount", swapAsset.getDstNativeAmount().toString());
        bidComponents.put("requestor", swapAsset.getRequestor().toString());
        bidComponents.put("lpId", lpBridge.getLpId());
        bidComponents.put("userSign", swapAsset.getUserSign().toString());

        bidComponents.put("expectedSingleStepTime", "0");
        bidComponents.put("tolerantSingleStepTime", "0");
        bidComponents.put("earliestRefundTime", "0");
        bidComponents.put("lpSign", "");

        if (swapAsset instanceof ExtendedAtomicSwapAsset) {
            ExtendedAtomicSwapAsset atomicSwapAsset = (ExtendedAtomicSwapAsset) swapAsset;
            if (atomicSwapAsset.getExpectedSingleStepTime() != null) {
                bidComponents.put("expectedSingleStepTime", atomicSwapAsset.getExpectedSingleStepTime().toString());
            }
            if (atomicSwapAsset.getTolerantSingleStepTime() != null) {
                bidComponents.put("tolerantSingleStepTime", atomicSwapAsset.getTolerantSingleStepTime().toString());
            }
            if (atomicSwapAsset.getEarliestRefundTime() != null) {
                bidComponents.put("earliestRefundTime", atomicSwapAsset.getEarliestRefundTime().toString());
            }
        } else if (swapAsset instanceof ExtendedSingleSwapAsset) {
            ExtendedSingleSwapAsset singleSwapAsset = (ExtendedSingleSwapAsset) swapAsset;
            if (singleSwapAsset.getExpectedSingleStepTime() != null) {
                bidComponents.put("expectedSingleStepTime", singleSwapAsset.getExpectedSingleStepTime().toString());
            }
        }

        if (swapAsset.getLpSign() != null) {
            bidComponents.put("lpSign", swapAsset.getLpSign().toString());
        }

        log.info("Business ID components: {}", bidComponents.toJSONString());

        StringBuilder contractOrderBidString = new StringBuilder()
                .append(bidComponents.getString("agreementReachedTime"))
                .append(bidComponents.getString("srcChainId"))
                .append(bidComponents.getString("srcAddress"))
                .append(bidComponents.getString("srcToken"))
                .append(bidComponents.getString("dstChainId"))
                .append(bidComponents.getString("dstAddress"))
                .append(bidComponents.getString("dstToken"))
                .append(bidComponents.getString("srcAmount"))
                .append(bidComponents.getString("dstAmount"))
                .append(bidComponents.getString("dstNativeAmount"))
                .append(bidComponents.getString("requestor"))
                .append(bidComponents.getString("lpId"))
                .append(bidComponents.getString("expectedSingleStepTime"))
                .append(bidComponents.getString("tolerantSingleStepTime"))
                .append(bidComponents.getString("earliestRefundTime"))
                .append(bidComponents.getString("userSign"))
                .append(bidComponents.getString("lpSign"));

        return contractOrderBidString.toString();
    }

    protected String getBusinessId_Pre(PreBusiness resultBusiness, LPBridge lpBridge) {
        if ("ATOMIC".equals(resultBusiness.getSwapAssetInformation().getSwapType())) {
            ExtendedAtomicSwapAsset swapAsset = (ExtendedAtomicSwapAsset) resultBusiness.getSwapAssetInformation();
            return resultBusiness.getSwapAssetInformation().getAgreementReachedTime().toString() +
                    lpBridge.getBridge().getSrcChainId().toString() +
                    AddressHelper.getDecimalAddress(
                            resultBusiness.getSwapAssetInformation().getQuote().getQuoteBase().getLpBridgeAddress(),
                            lpBridge.getBridge().getSrcChainId())
                    +
                    lpBridge.getBridge().getSrcToken().toString() +
                    lpBridge.getBridge().getDstChainId().toString() +
                    new BigInteger(resultBusiness.getSwapAssetInformation().getDstAddress().substring(2), 16).toString()
                    +
                    lpBridge.getBridge().getDstToken().toString() +
                    resultBusiness.getSwapAssetInformation().getAmount().toString() +
                    resultBusiness.getSwapAssetInformation().getDstAmount().toString() +
                    resultBusiness.getSwapAssetInformation().getDstNativeAmount().toString() +
                    resultBusiness.getSwapAssetInformation().getRequestor().toString() +
                    lpBridge.getLpId() +
                    swapAsset.getExpectedSingleStepTime().toString() +
                    swapAsset.getTolerantSingleStepTime().toString() +
                    swapAsset.getEarliestRefundTime().toString() +
                    resultBusiness.getSwapAssetInformation().getUserSign().toString();
        } else if ("SINGLECHAIN".equals(resultBusiness.getSwapAssetInformation().getSwapType())) {
            ExtendedSingleSwapAsset swapAsset = (ExtendedSingleSwapAsset) resultBusiness.getSwapAssetInformation();
            return resultBusiness.getSwapAssetInformation().getAgreementReachedTime().toString() +
                    lpBridge.getBridge().getSrcChainId().toString() +
                    AddressHelper.getDecimalAddress(
                            resultBusiness.getSwapAssetInformation().getQuote().getQuoteBase().getLpBridgeAddress(),
                            lpBridge.getBridge().getSrcChainId())
                    +
                    lpBridge.getBridge().getSrcToken().toString() +
                    lpBridge.getBridge().getDstChainId().toString() +
                    new BigInteger(resultBusiness.getSwapAssetInformation().getDstAddress().substring(2), 16).toString()
                    +
                    lpBridge.getBridge().getDstToken().toString() +
                    resultBusiness.getSwapAssetInformation().getAmount().toString() +
                    resultBusiness.getSwapAssetInformation().getDstAmount().toString() +
                    resultBusiness.getSwapAssetInformation().getDstNativeAmount().toString() +
                    resultBusiness.getSwapAssetInformation().getRequestor().toString() +
                    lpBridge.getLpId() +
                    swapAsset.getExpectedSingleStepTime().toString() +
                    resultBusiness.getSwapAssetInformation().getUserSign().toString();
        }
        return "";
    }
}