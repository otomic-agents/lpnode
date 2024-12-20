package com.bytetrade.obridge.api.Atomic;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bytetrade.obridge.base.Result;
import com.bytetrade.obridge.bean.AskCmd;
import com.bytetrade.obridge.bean.AtomicBusinessFullData;
import com.bytetrade.obridge.component.AtomicLPController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/lpnode/relay/")
public class AtomicToRelay {

    @Autowired
    AtomicLPController atomicLpController;

    @PostMapping(value = "{lpnode_api_key}/update_business_transfer_out/{comm_id}", produces = "application/json;charset=UTF-8")
    public Result updateBusinessTransferOut(
            @PathVariable String lpnode_api_key,
            @PathVariable String comm_id,
            @RequestBody AtomicBusinessFullData bfd) {
        log.info("<- comm_id:{}", comm_id);
        log.info("<- relay call updateBusinessTransferOut bfd:" + bfd.toString());
        log.info("<- lpnode_api_key:{}", lpnode_api_key);
        log.info("<- businessId:{}", bfd.getBusiness().getBusinessHash());
        Result result = new Result(32006, "Lp offline");
        try {
            Boolean arrived = atomicLpController.onRelayTransferOut(bfd);
            if (arrived) {
                log.info("updateBusinessTransferOut info arrived");
                result = new Result(200, "");
                result.setCommid(comm_id);
            } else {
                result = new Result(32006, "Lp offline");
            }
        } catch (Exception e) {
            result = new Result(32006, "An error occurred during LP processing");
        } finally {

        }
        return result;
    }

    @PostMapping(value = "{lpnode_api_key}/update_business_transfer_out_confirm", produces = "application/json;charset=UTF-8")
    public Result updateBusinessTransferOutConfirm(
            @PathVariable String lpnode_api_key,
            @RequestBody AtomicBusinessFullData bfd) {
        log.info("on updateBusinessTransferOutConfirm:" + bfd.toString());
        atomicLpController.onRelayTransferOutConfirm(bfd);
        return new Result(200, "");
    }

    @PostMapping(value = "{lpnode_api_key}/update_business_transfer_out_refund", produces = "application/json;charset=UTF-8")
    public Result updateBusinessTransferOutRefund(
            @PathVariable String lpnode_api_key,
            @RequestBody AtomicBusinessFullData bfd) {
        log.info("on updateBusinessTransferOutRefund:" + bfd.toString());
        atomicLpController.onRelayTransferOutRefund(bfd);
        return new Result(200, "");
    }

    @PostMapping(value = "{lpnode_api_key}/ask_quote", produces = "application/json;charset=UTF-8")
    public Result askQuote(
            @PathVariable String lpnode_api_key,
            @RequestBody AskCmd askCmd) {
        atomicLpController.relayAskQuote(askCmd);
        return new Result(200, "");
    }

}
