package com.bytetrade.obridge.api.common;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bytetrade.obridge.base.Result;
import com.bytetrade.obridge.bean.Business;
import com.bytetrade.obridge.bean.PreBusiness;
import com.bytetrade.obridge.bean.QuoteRemoveInfo;
import com.bytetrade.obridge.component.CommLpController;

@Slf4j
@RestController
@RequestMapping("/lpnode/relay/")
public class Quote {
    @Autowired
    CommLpController commLpController;

    @PostMapping(value = "{lpnode_api_key}/quote_removed", produces = "application/json;charset=UTF-8")
    public Result quoteRemoved(
            @RequestBody List<QuoteRemoveInfo> data) {
        commLpController.onQuoteRemoved(data);
        return new Result(200, "");
    }

    @PostMapping(value = "{lpnode_api_key}/lock_quote", produces = "application/json;charset=UTF-8")
    public ResultLockQuote lockQuote(
            @RequestBody PreBusiness business) {

        long startTime = System.nanoTime();
        PreBusiness resultBusiness = commLpController.onRelayLockQuote(business);
        ResultLockQuote result = new ResultLockQuote(200, "", resultBusiness);
        long endTime = System.nanoTime();
        long duration = endTime - startTime;

        double durationInMilliseconds = duration / 1_000_000.0;
        log.info("LockQuote execution time: " + durationInMilliseconds + " milliseconds");
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
}
