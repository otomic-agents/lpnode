package com.bytetrade.obridge.component.client.request;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class Gas {

    public static final String GAS_PRICE_TYPE_FAST = "FAST";

    public static final String GAS_PRICE_TYPE_STANDARD = "STANDARD";

    public static final String GAS_PRICE_TYPE_SAFELOW = "SAFELOW";

    String gasPrice;
}