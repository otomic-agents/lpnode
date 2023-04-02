package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.bytetrade.obridge.bean.EventTransferOut;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Accessors(chain = true)
public class EventTransferOutBox {

    String chainId;

    EventTransferOut eventParse;

    String transferInfo;

    String eventRaw;
}
