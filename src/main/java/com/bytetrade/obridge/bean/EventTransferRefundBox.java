package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.bytetrade.obridge.bean.EventRefund;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventTransferRefundBox {

    String chainId;

    EventRefund eventParse;

    String transferInfo;

    String eventRaw;
}
