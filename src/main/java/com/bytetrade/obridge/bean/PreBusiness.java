package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Accessors(chain = true)
public class PreBusiness {

    String uuid;

    SwapAssetInformation swapAssetInformation;

    String hash;

    String lpSalt;

    String hashlockEvm;

    String hashlockXrp;

    String hashlockNear;

    String hashlockSolana;

    Boolean locked;

    String lockMessage;

    Long timestamp;

    String orderAppendData;

    Boolean isKyc;

    Boolean sameDid;

    KycInfo kycInfo;
}