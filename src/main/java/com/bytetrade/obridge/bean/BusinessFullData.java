package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@NoArgsConstructor
public class BusinessFullData {

    PreBusiness preBusiness;

    Business business;

    EventTransferOut eventTransferOut;

    EventTransferIn eventTransferIn;

    EventTransferOutConfirm eventTransferOutConfirm;

    EventTransferInConfirm eventTransferInConfirm;

    EventTransferOutRefund eventTransferOutRefund;

    EventTransferInRefund eventTransferInRefund;
}