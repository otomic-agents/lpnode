package com.bytetrade.obridge.bean.SingleSwap;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
@JsonIgnoreProperties(ignoreUnknown = true)
@NoArgsConstructor
public class EventRefundSwap {
    String transferInfo;
    String transferId;

    public String getTransferId() {
        return formatHexValue(transferId);
    }

    private String formatHexValue(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        if (!value.startsWith("0x")) {
            return "0x" + value;
        }
        return value;
    }
}
