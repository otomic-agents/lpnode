package com.bytetrade.obridge.api;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bytetrade.obridge.bean.EventTransferOutBox;
import com.bytetrade.obridge.bean.EventTransferInBox;
import com.bytetrade.obridge.bean.EventTransferConfirmBox;
import com.bytetrade.obridge.bean.EventTransferRefundBox;
import com.bytetrade.obridge.base.Result;
import com.bytetrade.obridge.component.LPController;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/lpnode/chain_client/")
public class ToChainClient {

    @Autowired
    LPController lpController;

    @PostMapping("on_transfer_out")
    public Result onTransferOut(
            @RequestBody EventTransferOutBox eventBox) {
        log.info("<- [EVENT] on_transfer_out:{}", eventBox.toString());
        lpController.onEventTransferOut(eventBox);
        return new Result(200, "");
    }

    @PostMapping("on_confirm")
    public Result onConfirm(
            @RequestBody EventTransferConfirmBox eventBox) {
        log.info("<- [EVENT]  on_confirm:{}", eventBox.toString());
        lpController.onConfirm(eventBox);
        return new Result(200, "");
    }

    @PostMapping("on_refund")
    public Result onRefund(
            @RequestBody EventTransferRefundBox eventBox) {

        log.info("<- [EVENT] on_refund:{}", eventBox.toString());
        lpController.onEventRefund(eventBox);
        return new Result(200, "");
    }

    @PostMapping("on_transfer_in")
    public Result onTransferIn(
            @RequestBody EventTransferInBox eventBox) {
        log.info("<- [EVENT] on_transfer_in:{}", eventBox.toString());
        lpController.onEventTransferIn(eventBox);
        return new Result(200, "");
    }

}
