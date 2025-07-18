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
public class EventConfirmSwap {
    String transferId;
    String transferInfo;
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
