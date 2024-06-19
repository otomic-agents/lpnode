package com.bytetrade.obridge.component.client.request;

import lombok.*;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class CommandTransferIn {
    
    String uuid; // Solana transactions have UUID

    String senderWalletName;

    String userReceiverAddress;

    String token;

    String tokenAmount;

    String ethAmount;

    String hashLock;

    Long stepTimeLock;

    Long agreementReachedTime;

    Integer srcChainId;

    String srcTransferId;

    String appendInformation;
}