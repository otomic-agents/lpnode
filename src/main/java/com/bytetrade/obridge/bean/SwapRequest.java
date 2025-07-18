package com.bytetrade.obridge.bean;

import com.bytetrade.obridge.bean.SingleSwap.ExtendedSingleSwapAsset;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;

@Data
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SwapRequest {
    @NotNull(message = "swap_type is required")
    @Pattern(regexp = "^(SINGLECHAIN|ATOMIC)$", message = "Invalid swap type")
    private String swapType;

    private Quote quote;
    private String bridgeName;
    private String lpIdFake;
    private String sender;
    private String amount;
    private String dstAddress;
    private String dstAmount;
    private String dstNativeAmount;
    private Integer systemFeeSrc;
    private Integer systemFeeDst;
    private String dstAmountNeed;
    private String dstNativeAmountNeed;
    private Long agreementReachedTime;
    private String appendInformation;
    private String jws;
    private String did;
    private String requestor;
    private String userSign;
    private String lpSign;
    private Long expectedSingleStepTime;
    private Long tolerantSingleStepTime;
    private Long earliestRefundTime;

    public SwapAssetInformation toSwapAsset() {
        validateRequest();

        return switch (swapType) {
            case "SINGLECHAIN" -> createSingleChainSwap();
            case "ATOMIC" -> createAtomicSwap();
            default -> throw new IllegalArgumentException("Unknown swap type: " + swapType);
        };
    }

    private void validateRequest() {
        if (swapType == null) {
            throw new IllegalArgumentException("swap_type cannot be null");
        }
        if (quote == null) {
            throw new IllegalArgumentException("quote cannot be null");
        }
        if (StringUtils.isBlank(bridgeName)) {
            throw new IllegalArgumentException("bridge_name cannot be blank");
        }
    }

    private ExtendedSingleSwapAsset createSingleChainSwap() {
        ExtendedSingleSwapAsset swap = new ExtendedSingleSwapAsset();
        copyCommonFields(swap);
        swap.setExpectedSingleStepTime(expectedSingleStepTime);
        return swap;
    }

    private ExtendedAtomicSwapAsset createAtomicSwap() {
        ExtendedAtomicSwapAsset swap = new ExtendedAtomicSwapAsset();
        copyCommonFields(swap);
        swap.setExpectedSingleStepTime(expectedSingleStepTime);
        swap.setTolerantSingleStepTime(tolerantSingleStepTime);
        swap.setEarliestRefundTime(earliestRefundTime);
        return swap;
    }

    private void copyCommonFields(SwapAssetInformation target) {
        target.setSwapType(swapType);
        target.setQuote(quote);
        target.setBridgeName(bridgeName);
        target.setLpIdFake(lpIdFake);
        target.setSender(sender);
        target.setAmount(amount);
        target.setDstAddress(dstAddress);
        target.setDstAmount(dstAmount);
        target.setDstNativeAmount(dstNativeAmount);
        target.setSystemFeeSrc(systemFeeSrc);
        target.setSystemFeeDst(systemFeeDst);
        target.setDstAmountNeed(dstAmountNeed);
        target.setDstNativeAmountNeed(dstNativeAmountNeed);
        target.setAgreementReachedTime(agreementReachedTime);
        target.setAppendInformation(appendInformation);
        target.setJws(jws);
        target.setDid(did);
        target.setRequestor(requestor);
        target.setUserSign(userSign);
        target.setLpSign(lpSign);
    }
}