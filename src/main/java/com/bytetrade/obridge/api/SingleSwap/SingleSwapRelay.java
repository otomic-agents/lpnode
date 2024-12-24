package com.bytetrade.obridge.api.SingleSwap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.bytetrade.obridge.bean.SingleSwap.SingleSwapBusinessFullData;
import com.bytetrade.obridge.component.SingleSwapLpController;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestController
@RequestMapping("/lpnode/relay/single_swap/")
public class SingleSwapRelay {

    @Autowired
    SingleSwapLpController singleSwapController;

    @PostMapping(value = "{lpnode_api_key}/update_business_init_swap/{comm_id}", produces = "application/json;charset=UTF-8")
    public void updateBusinessInitSwap(
            @PathVariable String lpnode_api_key,
            @PathVariable String comm_id,
            @RequestBody SingleSwapBusinessFullData bfd) {
                singleSwapController.onRelayInitSwap(bfd);
    }
}
