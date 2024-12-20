package com.bytetrade.obridge.bean;

import lombok.*;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExtendedAtomicSwapAsset extends SwapAssetInformation {
    public ExtendedAtomicSwapAsset() {
        super();
        setSwapType("ATOMIC");
    }

    private Long expectedSingleStepTime;
    private Long tolerantSingleStepTime;
    private Long earliestRefundTime;
}