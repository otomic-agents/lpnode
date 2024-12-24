package com.bytetrade.obridge.bean.SingleSwap;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Accessors(chain = true)
public class EventConfirmSwapBox {
    String chainId;

    EventConfirmSwap eventParse;

    String transferInfo;

    String eventRaw;
}
