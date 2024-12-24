package com.bytetrade.obridge.api.SingleSwap;

import org.springframework.web.bind.annotation.RestController;

import com.bytetrade.obridge.base.Result;
import com.bytetrade.obridge.bean.SingleSwap.EventConfirmSwapBox;
import com.bytetrade.obridge.bean.SingleSwap.EventInitSwapBox;
import com.bytetrade.obridge.component.SingleSwapLpController;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

@Slf4j
@RestController
@RequestMapping("/lpnode/chain_client/single_swap")
public class SingleSwapChainClient {

    @Autowired
    SingleSwapLpController singleSwapController;

    @PostMapping("on_init_swap")
    public Result onInitSwap(
            @RequestBody EventInitSwapBox eventBox) {
        log.info("<- [EVENT] on_init_swap:{}", eventBox.toString());
        singleSwapController.onEventInitSwap(eventBox);
        return new Result(200, "");
    }

    @PostMapping("on_confirm_swap")
    public Result onConfirmSwap(@RequestBody EventConfirmSwapBox eventBox) {
        log.info("<- [EVENT] on_confirm_swap:{}", eventBox.toString());
        singleSwapController.onEventConfirmSwap(eventBox);
        return new Result(200, "");
    }
}
