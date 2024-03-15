package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@NoArgsConstructor
public class EventTransferOut {

    Long transferOutId;

    Long businessId;

    String transferInfo;

    String transferId;

    String sender;

    String receiver;

    String token;

    String amount;

    String hashLock;

    Long stepTimeLock;

    Long agreementReachedTime;

    Long dstChainId;

    String dstAddress;

    String bidId;

    String dstToken;

    String dstAmount;
}