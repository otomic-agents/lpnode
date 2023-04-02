package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@Accessors(chain = true)
public class EventTransferIn {
    
    String transferId;

    String sender;

    String receiver;

    String token;

    String tokenAmount;

    String ethAmount;

    String hashLock;

    String hashLockOriginal;

    Long timeLock;

    Integer srcChainId;

    String srcTransferId;

    String transferInfo;
}
