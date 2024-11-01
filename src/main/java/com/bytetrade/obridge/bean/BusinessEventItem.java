package com.bytetrade.obridge.bean;

public enum BusinessEventItem {
    LP_EVENT_CHAIN_CLIENT_TRANSFER_OUT,
    LP_EVENT_CHAIN_CLIENT_TRANSFER_IN,
    LP_EVENT_CHAIN_CLIENT_DEPOSIT,
    LP_EVENT_RELAY_NOTIFY_TRANSFER_OUT,
    LP_EVENT_CHAIN_CLIENT_WITHDRAW;

    public String getValue() {
        return this.name();
    }

    public static BusinessEventItem fromValue(String value) {
        return valueOf(value);
    }
}
