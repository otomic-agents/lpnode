package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.bytetrade.obridge.bean.EventTransferOut;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@Accessors(chain = true)
public class EventTransferOutBox {

    String chainId;

    EventTransferOut eventParse;

    String transferInfo;

    String eventRaw;
}
