package com.bytetrade.obridge.component.client.request;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class CommandConfirmSwap {
    String bid;

    String sender;

    String senderWalletName;

    String userReceiverAddress;

    String token;

    String tokenAmount;

    String dstToken;

    String dstAmount;

    Long agreementReachedTime;

    Long expectedSingleStepTime;
}
