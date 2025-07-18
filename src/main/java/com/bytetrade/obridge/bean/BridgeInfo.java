package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
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

    @JsonProperty("bridge_name")
    String bridgeName;

    public String getBridgeName() {
        if (bridgeName == null) {
            return srcChainId.toString() + '_' + dstChainId.toString() + '_' + srcToken + '_' + dstToken;
        }
        return bridgeName;
        
    }
}
