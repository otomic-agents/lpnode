package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "swap_type", defaultImpl = ExtendedAtomicSwapAsset.class)
@JsonSubTypes({
        @JsonSubTypes.Type(value = ExtendedAtomicSwapAsset.class, name = "ATOMIC"),
        @JsonSubTypes.Type(value = ExtendedSingleSwapAsset.class, name = "SINGLECHAIN")
})
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true) // 忽略未知字段，防止 super 嵌套
public class SwapAssetInformation {

    public static final String SWAP_TYPE_ATOMIC = "ATOMIC";

    public static final String SWAP_TYPE_SINGLECHAIN = "SINGLECHAIN";
    @JsonProperty("swap_type")
    private String swapType;
    String bridgeName;

    String lpIdFake;
    String sender;// address, user src address
    String amount;// uint256, token src amount
    String dstAddress;
    String dstAmount;
    String dstNativeAmount;
    Integer systemFeeSrc;
    Integer systemFeeDst;
    String dstAmountNeed;
    String dstNativeAmountNeed;
    Long agreementReachedTime;
    Quote quote;
    String appendInformation;
    String jws;
    String did;
    String requestor;
    String userSign;
    String lpSign;
    String LpSignAddress;
}