package com.bytetrade.obridge.bean.SingleSwap;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
public class EventInitSwap {

    String transferInfo;

    String transferId;

    String sender;

    String receiver;

    String token;

    String amount;

    String dstToken;

    String dstAmount;

    Long expectedSingleStepTime;

    Long agreementReachedTime;

    String bidId;

    String requestor;

    String lpId;

}
