package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class QuoteData {

    private int errorCode;
    
    private String errorMessage;

    private String price;

    private String nativeTokenPrice;

    private String nativeTokenMax;

    private String nativeTokenMin;

    private String capacity;

    private String quoteHash;
}
