package com.bytetrade.obridge.bean.SingleSwap;

import lombok.*;
import lombok.experimental.Accessors;

import com.bytetrade.obridge.bean.SwapAssetInformation;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@EqualsAndHashCode(callSuper = true)
@ToString(callSuper = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class ExtendedSingleSwapAsset extends SwapAssetInformation {
    public ExtendedSingleSwapAsset() {
        super();
        setSwapType("SINGLECHAIN"); // 在构造函数中设置默认值
    }

    private Long expectedSingleStepTime;
}
