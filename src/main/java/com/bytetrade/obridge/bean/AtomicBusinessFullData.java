package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@Builder(toBuilder = true)
public class AtomicBusinessFullData extends BusinessFullData<AtomicBusinessFullData> {
    private EventTransferOut eventTransferOut;
    private EventTransferIn eventTransferIn;
    private EventTransferOutConfirm eventTransferOutConfirm;
    private EventTransferInConfirm eventTransferInConfirm;
    private EventTransferOutRefund eventTransferOutRefund;
    private EventTransferInRefund eventTransferInRefund;
}
