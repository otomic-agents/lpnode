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
    private String businessId;
    private String eventName;
    private Integer chainId;
    private Long sendTime;
    private String gasPrice;
    private String systemChainId;
    private String errorMessage;
    private String sendId;
    private String txHash;

    @Indexed
    private String status;

    private boolean needNewGasPrice;

    private Integer increasedNumber;
    private Long createdAt;
    private Long updatedAt;

    // Add the send list to track each send attempt
    private List<SendRecord> sendList = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SendRecord {
        private Long timestamp;
        private String gasPrice; // 16 0x6fc23ac00
        private String sendId;
        private String status;
        private boolean needNewGasPrice;
        private String txHash;
        // Optional additional fields
        private String error;
    }



    // Add the gas increase list to track gas price increases
    private List<GasIncreaseRecord> incGasList = new ArrayList<>();

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GasIncreaseRecord {
        private Long timestamp;
        private String originalGasPrice;
        private String newGasPrice;
    }
}
