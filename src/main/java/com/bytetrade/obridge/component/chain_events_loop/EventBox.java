package com.bytetrade.obridge.component.chain_events_loop;

import com.fasterxml.jackson.databind.JsonNode;

public class EventBox {
    private String _id;
    private Long blockNumber;
    private String transactionHash;
    private Integer transactionIndex;
    private String eventName;
    private String transferId;
    private JsonNode parsedData;
    private String taskId;
    private String createdAt;

    public EventBox() {
    }

    // Getters  Setters
    public String get_id() {
        return _id;
    }

    public void set_id(String _id) {
        this._id = _id;
    }

    public Long getBlockNumber() {
        return blockNumber;
    }

    public void setBlockNumber(Long blockNumber) {
        this.blockNumber = blockNumber;
    }

    public String getTransactionHash() {
        return transactionHash;
    }

    public void setTransactionHash(String transactionHash) {
        this.transactionHash = transactionHash;
    }

    public Integer getTransactionIndex() {
        return transactionIndex;
    }

    public void setTransactionIndex(Integer transactionIndex) {
        this.transactionIndex = transactionIndex;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public String getTransferId() {
        return transferId;
    }

    public void setTransferId(String transferId) {
        this.transferId = transferId;
    }

    public JsonNode getParsedData() {
        return parsedData;
    }

    public void setParsedData(JsonNode parsedData) {
        this.parsedData = parsedData;
    }

    public String getTaskId() {
        return taskId;
    }

    public void setTaskId(String taskId) {
        this.taskId = taskId;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }
}
