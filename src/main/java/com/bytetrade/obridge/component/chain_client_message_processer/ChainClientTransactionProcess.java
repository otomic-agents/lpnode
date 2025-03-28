package com.bytetrade.obridge.component.chain_client_message_processer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.stereotype.Component;

import com.bytetrade.obridge.bean.OtmoicSystemEventBusMessageReply;
import com.bytetrade.obridge.bean.chain_transaction.ChainTransaction;
import com.bytetrade.obridge.bean.chain_transaction.ChainTransaction.GasIncreaseRecord;
import com.bytetrade.obridge.bean.chain_transaction.ChainTransaction.SendRecord;
import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import java.math.BigInteger;

@Component
@Slf4j
public class ChainClientTransactionProcess {
    @Autowired
    private MongoTemplate mongoTemplate;

    /**
     * Process chain transaction information
     * 
     * @param chainTx Chain transaction information
     * @return If status is timeouted, returns reply object, otherwise returns null
     */
    public OtmoicSystemEventBusMessageReply process(String messageId, ChainTransaction chainTx) {
        log.info("Processing transaction: {}", chainTx);

        Query query = new Query(Criteria.where("eventName").is(chainTx.getEventName())
                .and("businessId").is(chainTx.getBusinessId()));

        Update update = new Update();
        update.set("status", chainTx.getStatus());
        update.set("gasPrice", chainTx.getGasPrice());
        update.set("sendId", chainTx.getSendId());
        update.set("txHash", chainTx.getTxHash());
        update.set("errorMessage", chainTx.getErrorMessage());
        update.set("updatedAt", System.currentTimeMillis());
        update.set("sendTime", chainTx.getSendTime());
        update.set("systemChainId", chainTx.getSystemChainId());
        update.setOnInsert("createdAt", System.currentTimeMillis());

        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);

        ChainTransaction result = mongoTemplate.findAndModify(
                query,
                update,
                options,
                ChainTransaction.class,
                "chain_clients_sended_transactions");

        log.info("Updated transaction: {}", result);

        recordSend(messageId, chainTx);

        if (chainTx.isNeedNewGasPrice()) {
            // First, query the current increasedNumber
            Query increasedNumber = new Query(Criteria.where("eventName").is(chainTx.getEventName())
                    .and("businessId").is(chainTx.getBusinessId()));
            ChainTransaction currentTx = mongoTemplate.findOne(increasedNumber, ChainTransaction.class,
                    "chain_clients_sended_transactions");

            // Check if the increasedNumber is less than 3
            if (currentTx != null && (currentTx.getIncreasedNumber() == null || currentTx.getIncreasedNumber() < 3)) {
                return onNewGasPrice(messageId, chainTx);
            } else {
                JSONObject responseData = new JSONObject();
                responseData.put("status", "gas_increased");
                responseData.put("oldGasPrice", chainTx.getGasPrice());
                responseData.put("newGasPrice", "0x0");
                responseData.put("messageId", messageId);
                responseData.put("stopTx", true);
                return new OtmoicSystemEventBusMessageReply(messageId, "CHAIN_TRANSACTION_RESPONSE", responseData);
            }
        }
        return null;
        // JSONObject responseData = new JSONObject();
        // responseData.put("status", "success");
        // responseData.put("messageId", messageId);
        // return new OtmoicSystemEventBusMessageReply(messageId,
        // "CHAIN_TRANSACTION_RESPONSE", responseData);
    }

    public void recordSend(String messageId, ChainTransaction chainTx) {
        try {
            Query query = new Query(Criteria.where("eventName").is(chainTx.getEventName())
                    .and("businessId").is(chainTx.getBusinessId()));
            log.info("record tx ,query is:{}", query);
            Update gasUpdate = new Update();
            SendRecord sendRecord = new SendRecord();
            sendRecord.setGasPrice(chainTx.getGasPrice());
            sendRecord.setTxHash(chainTx.getTxHash());
            sendRecord.setNeedNewGasPrice(chainTx.isNeedNewGasPrice());
            sendRecord.setSendId(chainTx.getSendId());
            sendRecord.setStatus(chainTx.getStatus());
            sendRecord.setError(chainTx.getErrorMessage());
            sendRecord.setTimestamp(chainTx.getSendTime());
            gasUpdate.push("sendList", sendRecord);
            mongoTemplate.updateFirst(
                    query,
                    gasUpdate,
                    ChainTransaction.class,
                    "chain_clients_sended_transactions");
        } catch (Exception e) {
            log.error("Error recordSend: {}", e.getMessage(), e);
        }
    }

    public OtmoicSystemEventBusMessageReply onNewGasPrice(String messageId, ChainTransaction chainTx) {
        try {
            log.info("IncreaseRecord");
            Query query = new Query(Criteria.where("eventName").is(chainTx.getEventName())
                    .and("businessId").is(chainTx.getBusinessId()));

            String currentGasPrice = chainTx.getGasPrice();
            String newGasPrice = increaseGasPriceByPercent(currentGasPrice, 15);

            Update gasUpdate = new Update();
            gasUpdate.inc("increasedNumber");
            gasUpdate.push("incGasList", new GasIncreaseRecord(
                    System.currentTimeMillis(),
                    currentGasPrice,
                    newGasPrice));

            mongoTemplate.updateFirst(
                    query,
                    gasUpdate,
                    ChainTransaction.class,
                    "chain_clients_sended_transactions");
            JSONObject responseData = new JSONObject();
            responseData.put("status", "gas_increased");
            responseData.put("oldGasPrice", currentGasPrice);
            responseData.put("newGasPrice", newGasPrice);
            responseData.put("messageId", messageId);
            return new OtmoicSystemEventBusMessageReply(messageId, "CHAIN_TRANSACTION_RESPONSE", responseData);
        } catch (Exception e) {
            log.error("Error creating reply for timeouted transaction: {}", e.getMessage(), e);
        }
        return null;
    }

    /**
     * Increase gasPrice by specified percentage
     * 
     * @param hexGasPrice Gas price in hexadecimal format, e.g. "0x6fc23ac00"
     * @param percent     Percentage to increase
     * @return Increased gas price in hexadecimal format
     */
    private String increaseGasPriceByPercent(String hexGasPrice, int percent) {
        try {
            // Remove "0x" prefix (if present)
            String cleanHex = hexGasPrice.startsWith("0x") ? hexGasPrice.substring(2) : hexGasPrice;

            // Convert to BigInteger
            BigInteger gasPrice = new BigInteger(cleanHex, 16);

            // Calculate value after percentage increase
            BigInteger increase = gasPrice.multiply(BigInteger.valueOf(percent)).divide(BigInteger.valueOf(100));
            BigInteger newGasPrice = gasPrice.add(increase);

            // Convert back to hex format with "0x" prefix
            return "0x" + newGasPrice.toString(16);
        } catch (Exception e) {
            log.error("Failed to increase gas price: {}", e.getMessage(), e);
            return hexGasPrice; // Return original value on error
        }
    }
}
