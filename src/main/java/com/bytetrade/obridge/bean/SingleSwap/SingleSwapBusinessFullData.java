package com.bytetrade.obridge.bean.SingleSwap;

import lombok.*;
import lombok.experimental.Accessors;

import com.bytetrade.obridge.bean.BusinessFullData;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = false)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SingleSwapBusinessFullData extends BusinessFullData<SingleSwapBusinessFullData> {
    private EventInitSwap eventInitSwap;
    private EventConfirmSwap eventConfirmSwap;
    private EventRefundSwap eventRefundSwap;
}
