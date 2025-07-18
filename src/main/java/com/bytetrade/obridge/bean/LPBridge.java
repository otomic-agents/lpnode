package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class LPBridge {

    BridgeInfo bridge;

    WalletInfo wallet;

    QuoteAuthenticationLimiter authenticationLimiter;

    String lpId;

    String lpReceiverAddress;

    String msmqName;

    String srcClientUri;

    String dstClientUri;

    String relayApiKey;

    String relayUri;
}
