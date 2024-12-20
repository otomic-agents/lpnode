package com.bytetrade.obridge.component;

import com.bytetrade.obridge.bean.ExtendedAtomicSwapAsset;
import com.bytetrade.obridge.bean.ExtendedSingleSwapAsset;
import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.bean.PreBusiness;
import com.bytetrade.obridge.component.client.request.AtomicSignData;
import com.bytetrade.obridge.component.client.request.SingleSignData;

public class SignDataFactory {
    public static Object createSignData(String swapType, PreBusiness preBusiness, LPBridge lpBridge) {
        if ("SINGLECHAIN".equalsIgnoreCase(swapType)) {
            return createSingleSignData(preBusiness, lpBridge);
        } else if ("ATOMIC".equalsIgnoreCase(swapType)) {
            return createAtomicSignData(preBusiness, lpBridge);
        }
        throw new IllegalArgumentException("Unsupported swap type: " + swapType);
    }

    private static SingleSignData createSingleSignData(PreBusiness preBusiness, LPBridge lpBridge) {

        ExtendedSingleSwapAsset singleSwapAsset = (ExtendedSingleSwapAsset) preBusiness.getSwapAssetInformation();
        return new SingleSignData()
                .setSrcChainId(lpBridge.getBridge().getSrcChainId())
                .setSrcAddress(lpBridge.getLpReceiverAddress())
                .setSrcToken(lpBridge.getBridge().getSrcToken())
                .setSrcAmount(singleSwapAsset.getAmount())
                .setDstChainId(lpBridge.getBridge().getDstChainId())
                .setDstAddress(singleSwapAsset.getDstAddress())
                .setDstToken(lpBridge.getBridge().getDstToken())
                .setDstAmount(singleSwapAsset.getDstAmount())
                .setDstNativeAmount(singleSwapAsset.getDstNativeAmount())
                .setRequestor(singleSwapAsset.getRequestor())
                .setLpId(lpBridge.getLpId())
                .setAgreementReachedTime(singleSwapAsset.getAgreementReachedTime())
                .setExpectedSingleStepTime(singleSwapAsset.getExpectedSingleStepTime());
    }

    private static AtomicSignData createAtomicSignData(PreBusiness preBusiness, LPBridge lpBridge) {
        ExtendedAtomicSwapAsset atomicSwapAsset = (ExtendedAtomicSwapAsset) preBusiness.getSwapAssetInformation();
        return new AtomicSignData()
                .setSrcChainId(lpBridge.getBridge().getSrcChainId())
                .setSrcAddress(lpBridge.getLpReceiverAddress())
                .setSrcToken(lpBridge.getBridge().getSrcToken())
                .setSrcAmount(preBusiness.getSwapAssetInformation().getAmount())
                .setDstChainId(lpBridge.getBridge().getDstChainId())
                .setDstAddress(preBusiness.getSwapAssetInformation().getDstAddress())
                .setDstToken(lpBridge.getBridge().getDstToken())
                .setDstAmount(preBusiness.getSwapAssetInformation().getDstAmount())
                .setDstNativeAmount(preBusiness.getSwapAssetInformation().getDstNativeAmount())
                .setRequestor(preBusiness.getSwapAssetInformation().getRequestor())
                .setLpId(lpBridge.getLpId())
                .setAgreementReachedTime(preBusiness.getSwapAssetInformation().getAgreementReachedTime())
                .setExpectedSingleStepTime(atomicSwapAsset.getExpectedSingleStepTime())
                .setTolerantSingleStepTime(atomicSwapAsset.getTolerantSingleStepTime())
                .setEarliestRefundTime(atomicSwapAsset.getEarliestRefundTime());
    }
}
