package com.bytetrade.obridge.bean;

import com.bytetrade.obridge.bean.PreBusiness;
import com.bytetrade.obridge.bean.Business;
import com.bytetrade.obridge.bean.EventTransferOut;
import com.bytetrade.obridge.bean.EventTransferIn;
import com.bytetrade.obridge.bean.EventTransferOutConfirm;
import com.bytetrade.obridge.bean.EventTransferInConfirm;
import com.bytetrade.obridge.bean.EventTransferOutRefund;
import com.bytetrade.obridge.bean.EventTransferInRefund;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
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