package com.bytetrade.obridge.component.client.request;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public interface RequestSignMessage {
    RequestSignMessage setWalletName(String walletName);

    RequestSignMessage setSignData(SignData signData);
}
