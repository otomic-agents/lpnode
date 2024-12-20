package com.bytetrade.obridge.component.client.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategies;

public abstract class AbstractSignMessage {
    @JsonProperty("sign_data")
    protected Object signData;
    @JsonProperty("wallet_name")
    protected String walletName;

    public AbstractSignMessage setSignData(Object signData) {
        this.signData = signData;
        return this;
    }

    public AbstractSignMessage setWalletName(String walletName) {
        this.walletName = walletName;
        return this;
    }

    public String toSnakeCaseJson() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        mapper.setPropertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE);
        mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
        return mapper.writerWithDefaultPrettyPrinter().writeValueAsString(this);
    }

    public abstract String generateSign(String baseUri, Integer chainId);
}
