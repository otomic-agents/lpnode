package com.bytetrade.obridge.db;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@NoArgsConstructor
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Document(collection = "swap_orders")
public class SwapOrder {

    @Id
    private String id;

    private int srcChain;
    private int dstChain;
    private String quoteHash;
    private String businessId;
    private String srcAddress;
    private String dstAddress;
    private String swapType;
    private String srcAmount;
    private String dstAmount;
    private String dstSystemFeeAmount;
    private String srcToken;
    private String dstToken;
    private String srcDstPrice;
    private String srcNativeTokenUsdtPrice;
    private String dstNativeTokenUsdtPrice;
    private String transferOutId;
    private String transferOutConfirmId;
    private String transferInId;
    private String transferInConfirmId;
    private String refundOutId;
    private String refundInId;
    private String initSwapId;
    private String swapConfirmId;
    private String swapRefundId;
}