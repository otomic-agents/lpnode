package com.bytetrade.obridge.component;

import java.math.BigInteger;
import java.util.Map;
import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.PreBusiness;

public class LpControllerBase {
    public String getHexString(String numStr) {

        if (numStr.startsWith("0x")) {
            return numStr;
        }

        return "0x" + new BigInteger(numStr).toString(16);
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
        String bidIdString = resultBusiness.getSwapAssetInformation().getAgreementReachedTime().toString() +
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
                resultBusiness.getSwapAssetInformation().getExpectedSingleStepTime().toString() +
                resultBusiness.getSwapAssetInformation().getTolerantSingleStepTime().toString() +
                resultBusiness.getSwapAssetInformation().getEarliestRefundTime().toString() +
                resultBusiness.getSwapAssetInformation().getUserSign().toString();
        return bidIdString;
    }
}
