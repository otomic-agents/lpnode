package com.bytetrade.obridge.component.client.request;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class SignData {

    Integer srcChainId;

    String srcAddress;

    String srcToken;

    String srcAmount;

    Integer dstChainId;

    String dstAddress;

    String dstToken;

    String dstAmount;

    String dstNativeAmount;

    String requestor;

    String lpId;

    Long agreementReachedTime;

    Long expectedSingleStepTime;

    Long tolerantSingleStepTime;

    Long earliestRefundTime;

}
