package com.bytetrade.obridge.bean;

import com.alibaba.fastjson.annotation.JSONField;

public class OtmoicSystemEventBusMessage {
    @JSONField(name = "type")
    private String type;

    @JSONField(name = "messageId")
    private String messageId;

    @JSONField(name = "payload")
    private Object payload;

    public OtmoicSystemEventBusMessage() {
    }

    public OtmoicSystemEventBusMessage(String type, Object payload) {
        this.type = type;
        this.payload = payload;
    }

    // Getters and Setters
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public void setMessageId(String id) {
        this.messageId = id;
    }

    public String getMessageId() {
        return messageId;
    }
    public Object getPayload() {
        return payload;
    }

    public void setPayload(Object payload) {
        this.payload = payload;
    }

    @Override
    public String toString() {
        return "OtmoicSystemEventBusMessage{" +
                "type='" + type + '\'' +
                ", messageId='" + messageId + '\'' +
                ", payload=" + payload +
                '}';
    }
}
