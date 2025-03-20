package com.bytetrade.obridge.component.chain_client_message_processer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.stereotype.Component;

import com.bytetrade.obridge.bean.chain_transaction.ChainTransaction;

import lombok.extern.slf4j.Slf4j;

@Component
@Slf4j
public class ChainClientTransactionProcess {
    @Autowired
    private MongoTemplate mongoTemplate;

    public void process(ChainTransaction chainTx) {
        // log.info("process tx:{}", chainTx);
        // mongoTemplate.insert(chainTx, "chain_clients_sended_transactions");

        // log.info("process tx:{}", chainTx);
        
        
        Query query = new Query(Criteria.where("eventName").is(chainTx.getEventName())
                                .and("businessId").is(chainTx.getBusinessId()));
        
        
        Update update = new Update();
        
        
        update.set("systemChainId", chainTx.getSystemChainId());
        update.set("chainId", chainTx.getChainId());
        update.set("status", chainTx.getStatus());
        update.set("gasPrice", chainTx.getGasPrice());
        update.set("lastSend", chainTx.getLastSend());
        update.set("updatedAt", System.currentTimeMillis());
        
        
        if (chainTx.getTransferOut() != null) {
            update.set("transferOut", chainTx.getTransferOut());
        }
        
        if (chainTx.getTransferInfo() != null) {
            update.set("transferInfo", chainTx.getTransferInfo());
        }
        
        
        if (chainTx.getSendList() != null && !chainTx.getSendList().isEmpty()) {
            chainTx.getSendList().forEach(sendRecord -> {
                update.push("sendList", sendRecord);
            });
        }
        
        
        if (chainTx.getGasList() != null && !chainTx.getGasList().isEmpty()) {
            chainTx.getGasList().forEach(gasRecord -> {
                update.push("gasList", gasRecord);
            });
        }
        
        
        update.setOnInsert("createdAt", System.currentTimeMillis());
        
        
        FindAndModifyOptions options = FindAndModifyOptions.options().returnNew(true).upsert(true);
        
    
        ChainTransaction result = mongoTemplate.findAndModify(
            query, 
            update, 
            options,
            ChainTransaction.class, 
            "chain_clients_sended_transactions"
        );
        
        log.info("updated: {}", result);
    }
}
