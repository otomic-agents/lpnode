package com.bytetrade.obridge.component;

import java.math.BigInteger;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;

import com.bytetrade.obridge.bean.ExtendedAtomicSwapAsset;
import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.PreBusiness;
import com.bytetrade.obridge.bean.SingleSwap.ExtendedSingleSwapAsset;
import com.bytetrade.obridge.component.service.LPBridgeService;

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