package com.bytetrade.obridge.component.client.request;

import lombok.*;
import lombok.experimental.Accessors;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RequestSignMessageSolana implements RequestSignMessage {

    String walletName;

    AtomicSignData signData;
}
