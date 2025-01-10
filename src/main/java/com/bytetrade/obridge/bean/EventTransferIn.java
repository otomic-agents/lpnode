package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Accessors(chain = true)
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventTransferIn {

    String uuid;

    String transferId;

    String sender;

    String receiver;

    String token;

    String tokenAmount;

    String ethAmount;

    String hashLock;

    String hashLockOriginal;

    Long agreementReachedTime;

    Long expectedSingleStepTime;

    Long tolerantSingleStepTime;

    Long earliestRefundTime;

    Integer srcChainId;

    String srcTransferId;

    String transferInfo;
}
