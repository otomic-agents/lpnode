package com.bytetrade.obridge.bean;

import lombok.*;
import lombok.experimental.Accessors;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

@Data
@Accessors(chain = true)
@NoArgsConstructor
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class AskCmd {

    public static final String CMD_ASK_QUOTE = "ASK_QUOTE";

    String cmd;

    String cid;

    String bridge;

    String amount;

    String relayApiKey;
}