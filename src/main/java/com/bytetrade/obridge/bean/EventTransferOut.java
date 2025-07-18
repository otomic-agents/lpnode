package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventTransferOut {

    String transferInfo;
    
    String uuid;

    Long transferOutId;

    Long businessId;

    String transferId;

    String sender;

    String receiver;

    String token;

    String amount;

    String hashLock;

    Long agreementReachedTime;

    Long expectedSingleStepTime;

    Long tolerantSingleStepTime;

    Long earliestRefundTime;

    Long dstChainId;

    String dstAddress;

    String bidId;

    String dstToken;

    String dstAmount;
}