package com.bytetrade.obridge.bean.chain_transaction;

import java.util.ArrayList;

import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.index.Indexed;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chain_transactions")
public class ChainTransaction {
    @Id
    private ObjectId id;

    @Indexed
    private long systemChainId;

    @Indexed
    private long chainId;

    private String eventName;
    private Long lastSend;
    private String gasPrice;

    @Indexed
    private String businessId;

    private Long createdAt;
    private Long updatedAt;

    @Indexed
    private String status;

    // Add the send list to track each send attempt
    private List<SendRecord> sendList = new ArrayList<>();

    // Add the gas list to track gas information
    private List<GasRecord> gasList = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRecord {
        private Long timestamp;
        private String gasPrice;
        private String transactionHash;
        private String status;
        // Optional additional fields
        private String error;
        private Integer retryCount;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GasRecord {
        private Long timestamp;
        private String transactionHash;
        private String gasPrice;
    }

    private TransferOut transferOut;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferOut {
        private String uuid;
        private Integer transferOutId;
        private String transferId;
        private String sender;
        private String receiver;
        private String token;
        private String amount;
        private String hashLock;
        private Long agreementReachedTime;
        private Long expectedSingleStepTime;
        private Long tolerantSingleStepTime;
        private Long earliestRefundTime;
        private Integer dstChainId;
        private String dstAddress;
        private String bidId;
        private String dstToken;
        private String dstAmount;
    }

    private TransferInfo transferInfo;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TransferInfo {
        private Object provider;
        private String to;
        private String from;
        private String contractAddress;
        private String hash;
        private Integer index;
        private String blockHash;
        private String blockNumber;
        private String logsBloom;
        private String gasUsed;
        private String blobGasUsed;
        private String cumulativeGasUsed;
        private String gasPrice;
        private String blobGasPrice;
        private Integer type;
        private String status;
        private String transactionHash;
    }

    public void addSendRecord(String gasPrice, String transactionHash, String status) {
        if (this.sendList == null) {
            this.sendList = new ArrayList<>();
        }

        SendRecord record = new SendRecord();
        record.setTimestamp(System.currentTimeMillis());
        record.setGasPrice(gasPrice);
        record.setTransactionHash(transactionHash);
        record.setStatus(status);

        this.sendList.add(record);
        this.lastSend = record.getTimestamp();
    }

    public void addGasRecord(String transactionHash, String gasPrice, String gasLimit, String status) {
        if (this.gasList == null) {
            this.gasList = new ArrayList<>();
        }

        GasRecord record = new GasRecord();
        record.setTimestamp(System.currentTimeMillis());
        record.setTransactionHash(transactionHash);
        record.setGasPrice(gasPrice);

        this.gasList.add(record);
    }
}
