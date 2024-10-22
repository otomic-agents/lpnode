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
public class BridgeInfo {
    Integer srcChainId;
    Integer dstChainId;

    // hex
    String srcToken;

    // hex
    String dstToken;

    public String getBridgeName() {
        return srcChainId.toString() + '_' + dstChainId.toString() + '_' + srcToken + '_' + dstToken;
    }
}
