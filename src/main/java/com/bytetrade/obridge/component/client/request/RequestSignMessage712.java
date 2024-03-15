package com.bytetrade.obridge.component.client.request;

import lombok.*;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class RequestSignMessage712 {
    
    String walletName;

    SignData signData;
}