package com.bytetrade.obridge.bean.SingleSwap;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Accessors(chain = true)
public class EventRefundSwapBox {

    String chainId;
    EventRefundSwap eventParse;
    String transferInfo;

    String eventRaw;
}
