package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
@NoArgsConstructor
public class Quote {
    
    QuoteBase quoteBase;

    String quoteName;

    Long timestamp;
}
