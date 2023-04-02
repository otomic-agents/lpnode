package com.bytetrade.obridge.component;

import java.util.ArrayList;
import java.util.List;

import javax.annotation.Resource;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

import com.bytetrade.obridge.bean.LPBridge;
import com.bytetrade.obridge.component.LPController;
import com.bytetrade.obridge.bean.CmdEvent;
import com.bytetrade.obridge.db.redis.RedisConfig;

@Slf4j
@Service
public class LPCommandWatcher {

    List<RedisConnection> connections = new ArrayList<RedisConnection>();

    @Autowired
    ObjectMapper objectMapper;

    @Resource
    RedisConfig redisConfig;

    public void exitWatch() {
        for (RedisConnection rc : connections) {
            rc.getSubscription().unsubscribe();
        }
        connections.clear();
    }

    @Async
    public void watchCmd(LPBridge lpBridge, LPController lpController) {
        RedisConnectionFactory rcf = redisConfig.getRedisTemplate().getConnectionFactory();
        RedisConnection rc = rcf.getConnection();
        connections.add(rc);

        // log.info("rcf={}, conn={}", rcf, rc);

        rc.subscribe(new MessageListener() {

            @Override
            public void onMessage(Message message, byte[] pattern) {
                // log.info("message={}, {}", new String(message.getBody()), new String(message.getChannel()));
                String msg = new String(message.getBody());
                CmdEvent cmdEvent;
                try {
                    log.info("Message:" + msg);
                    cmdEvent = objectMapper.readValue(msg, CmdEvent.class);
                    switch (cmdEvent.getCmd()) {
                        case CmdEvent.CMD_UPDATE_QUOTE:
                            lpController.updateQuote(cmdEvent.getQuoteData(), lpBridge);
                            break;
                        case CmdEvent.EVENT_ASK_REPLY:
                            lpController.askReply(cmdEvent.getCid(), cmdEvent.getQuoteData(), lpBridge);
                            break;
                        case CmdEvent.CALLBACK_LOCK_QUOTE:
                            lpController.newCallback(
                                    cmdEvent.getPreBusiness().getHash() + "_" + CmdEvent.CALLBACK_LOCK_QUOTE, cmdEvent);
                            break;
                        case CmdEvent.CMD_TRANSFER_IN:
                            lpController.transferIn(cmdEvent.getBusinessFullData(), lpBridge);
                            break;
                        case CmdEvent.CMD_TRANSFER_IN_CONFIRM:
                            lpController.transferInConfirm(cmdEvent.getBusinessFullData(), lpBridge);
                            break;
                        case CmdEvent.CMD_TRANSFER_IN_REFUND:
                            lpController.transferInRefund(cmdEvent.getBusinessFullData(), lpBridge);
                            break;
                        default:
                            break;
                    }
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }

            }
        }, lpBridge.getMsmqName().getBytes());
    }

    @Async
    public void watchCmds(byte[][] channels, LPController lpController) {
        RedisConnectionFactory rcf = redisConfig.getRedisTemplate().getConnectionFactory();
        RedisConnection rc = rcf.getConnection();
        connections.add(rc);

        rc.subscribe(new MessageListener() {

            @Override
            public void onMessage(Message message, byte[] pattern) {
                LPCommandWatcher.this.notify(message, lpController );
            }

        }, channels);
    }

    @Async
    private void notify(Message message, LPController lpController) {
        String msg = new String(message.getBody());
        String channel = new String(message.getChannel());
        CmdEvent cmdEvent;
        LPBridge lpBridge = lpController.getBridgeFromChannel(channel);

        try {
            log.info("Message:" + msg);
            log.info("channel:" + channel);
            log.info("lpBridge:" + lpBridge);
            cmdEvent = objectMapper.readValue(msg, CmdEvent.class);
            switch (cmdEvent.getCmd()) {
                case CmdEvent.CMD_UPDATE_QUOTE:
                    lpController.updateQuote(cmdEvent.getQuoteData(), lpBridge);
                    break;
                case CmdEvent.EVENT_ASK_REPLY:
                    lpController.askReply(cmdEvent.getCid(), cmdEvent.getQuoteData(), lpBridge);
                    break;
                case CmdEvent.CALLBACK_LOCK_QUOTE:
                    lpController.newCallback(cmdEvent.getPreBusiness().getHash() + "_" + CmdEvent.CALLBACK_LOCK_QUOTE,
                            cmdEvent);
                    break;
                case CmdEvent.CMD_TRANSFER_IN:
                    lpController.transferIn(cmdEvent.getBusinessFullData(), lpBridge);
                    break;
                case CmdEvent.CMD_TRANSFER_IN_CONFIRM:
                    lpController.transferInConfirm(cmdEvent.getBusinessFullData(), lpBridge);
                    break;
                case CmdEvent.CMD_TRANSFER_IN_REFUND:
                    lpController.transferInRefund(cmdEvent.getBusinessFullData(), lpBridge);
                    break;
                default:
                    break;
            }
        } catch (Exception e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
    }
}