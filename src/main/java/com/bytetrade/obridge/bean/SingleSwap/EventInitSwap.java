package com.bytetrade.obridge.bean.SingleSwap;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class EventInitSwap {

    String transferInfo;

    String transferId;

    String sender;

    String receiver;

    String token;

    String amount;

    String dstToken;

    String dstAmount;

    Long expectedSingleStepTime;

    Long agreementReachedTime;

    String bidId;

    String requestor;

    String lpId;

    public String getTransferId() {
        if (transferId != null && !transferId.startsWith("0x")) {
            return "0x" + transferId;
        }
        return transferId;
    }
}
