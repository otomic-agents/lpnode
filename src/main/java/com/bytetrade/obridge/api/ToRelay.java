package com.bytetrade.obridge.api;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bytetrade.obridge.api.relay.ResultLockQuote;
import com.bytetrade.obridge.api.relay.ResultUnlockQuote;
import com.bytetrade.obridge.base.Result;
import com.bytetrade.obridge.bean.AskCmd;
import com.bytetrade.obridge.bean.Business;
import com.bytetrade.obridge.bean.PreBusiness;
import com.bytetrade.obridge.bean.QuoteRemoveInfo;
import com.bytetrade.obridge.bean.BusinessFullData;
import com.bytetrade.obridge.component.LPController;
import lombok.extern.slf4j.Slf4j;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

@Slf4j
@RestController
@RequestMapping("/lpnode/relay/")
public class ToRelay {

    @Autowired
    LPController lpController;

    @PostMapping(value = "{lpnode_api_key}/quote_removed", produces = "application/json;charset=UTF-8")
    public Result quoteRemoved(
            @RequestBody List<QuoteRemoveInfo> data) {
        lpController.onQuoteRemoved(data);
        return new Result(200, "");
    }

    @PostMapping(value = "{lpnode_api_key}/lock_quote", produces = "application/json;charset=UTF-8")
    public ResultLockQuote lockQuote(
            @RequestBody PreBusiness business) {
        PreBusiness resultBusiness = lpController.onLockQuote(business);
        ResultLockQuote result = new ResultLockQuote(200, "", resultBusiness);
        if (resultBusiness.getLocked() == false) {
            result = new ResultLockQuote(32011, resultBusiness.getLockMessage(), resultBusiness);
        }
        log.info("result:" + result.toString());
        return result;
    }

    @PostMapping(value = "{lpnode_api_key}/unlock_quote", produces = "application/json;charset=UTF-8")
    public ResultUnlockQuote unlockQuote(
            @RequestBody Business business) {
        return new ResultUnlockQuote(32001, "function not ready");
    }

    @PostMapping(value = "{lpnode_api_key}/update_business_transfer_out", produces = "application/json;charset=UTF-8")
    public Result updateBusinessTransferOut(
            @PathVariable String lpnode_api_key,
            @RequestBody BusinessFullData bfd) {
        log.info("<- relay call updateBusinessTransferOut bfd:" + bfd.toString());
        log.info("lpnode_api_key:" + lpnode_api_key);
        Boolean arrived = lpController.onTransferOut(bfd);
        if (arrived) {
            return new Result(200, "");
        } else {
            return new Result(32006, "lp offline");
        }
    }

    @PostMapping(value = "{lpnode_api_key}/update_business_transfer_out_confirm", produces = "application/json;charset=UTF-8")
    public Result updateBusinessTransferOutConfirm(
            @PathVariable String lpnode_api_key,
            @RequestBody BusinessFullData bfd) {
        log.info("on updateBusinessTransferOutConfirm:" + bfd.toString());
        lpController.onTransferOutConfirm(bfd);
        return new Result(200, "");
    }

    @PostMapping(value = "{lpnode_api_key}/update_business_transfer_out_refund", produces = "application/json;charset=UTF-8")
    public Result updateBusinessTransferOutRefund(
            @PathVariable String lpnode_api_key,
            @RequestBody BusinessFullData bfd) {
        log.info("on updateBusinessTransferOutRefund:" + bfd.toString());
        lpController.onTransferOutRefund(bfd);
        return new Result(200, "");
    }

    @PostMapping(value = "{lpnode_api_key}/ask_quote", produces = "application/json;charset=UTF-8")
    public Result askQuote(
            @PathVariable String lpnode_api_key,
            @RequestBody AskCmd askCmd) {
        lpController.askQuote(askCmd);
        return new Result(200, "");
    }

}
