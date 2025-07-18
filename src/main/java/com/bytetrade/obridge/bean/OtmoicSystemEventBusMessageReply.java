package com.bytetrade.obridge.bean;

import com.alibaba.fastjson.JSONObject;

public class OtmoicSystemEventBusMessageReply {
    private final String type;
    private final String messageId;
    private final JSONObject payload;

    public OtmoicSystemEventBusMessageReply(String messageId, String type, JSONObject payload) {
        this.type = type;
        this.messageId = messageId;
        this.payload = payload;
    }

    public String getMessageId() {
        return messageId;
    }

    public Object getPayload() {
        return payload;
    }
}
