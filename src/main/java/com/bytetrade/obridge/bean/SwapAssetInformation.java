package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@NoArgsConstructor
public class SwapAssetInformation {
    
    String bridgeName;

    String lpIdFake;

    String sender;//address, user src address

    String amount;//uint256, token src amount

    String dstAddress;

    String dstAmount;

    String dstNativeAmount;

    Integer systemFeeSrc;

    Integer systemFeeDst;

    String dstAmountNeed;

    String dstNativeAmountNeed;

    Long agreementReachedTime;
    
    Long expectedSingleStepTime;

    Long tolerantSingleStepTime;

    Long earliestRefundTime;

    Quote quote;

    String appendInformation;
    
    String jws;

    String did;

    String requestor;

    String userSign;

    String lpSign;

    String LpSignAddress;
}