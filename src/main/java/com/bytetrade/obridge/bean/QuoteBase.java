package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import java.util.List;
@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@JsonTypeInfo(use = JsonTypeInfo.Id.NONE)
public class QuoteBase {
    BridgeInfo bridge;

    String lpBridgeAddress;

    // hex
    String price;

    // hex
    String nativeTokenPrice;

    String nativeTokenMax;

    String nativeTokenMin;

    // hex
    String capacity;

    String lpNodeUri;

    String quoteHash;

    String relayApiKey;
    
    String capabilities;
}