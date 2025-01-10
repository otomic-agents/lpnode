package com.bytetrade.obridge.api.SingleSwap;

import org.springframework.web.bind.annotation.RestController;

import com.bytetrade.obridge.base.Result;
import com.bytetrade.obridge.bean.SingleSwap.EventConfirmSwapBox;
import com.bytetrade.obridge.bean.SingleSwap.EventInitSwapBox;
import com.bytetrade.obridge.bean.SingleSwap.EventRefundSwapBox;
import com.bytetrade.obridge.component.SingleSwapLpController;
import com.bytetrade.obridge.utils.JsonUtils;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * SingleSwapChainClient is a REST controller that handles single swap events
 * from the chain client. It provides endpoints for initializing, confirming,
 * and refunding swaps.
 */
@Slf4j // Lombok annotation to automatically generate a logger instance
@RestController // Marks this class as a REST controller
@RequestMapping("/lpnode/chain_client/single_swap") // Base URL for all endpoints in this controller
public class SingleSwapChainClient {

    @Autowired // Automatically injects an instance of SingleSwapLpController
    SingleSwapLpController singleSwapController;

    /**
     * Handles the initialization of a swap event.
     *
     * @param eventBox The event data containing swap initialization details.
     * @return A Result object indicating the success of the operation.
     */
    @PostMapping("on_init_swap")
    public Result onInitSwap(
            @RequestBody EventInitSwapBox eventBox) {
        // Log the incoming event for debugging and monitoring
        // log.info("<- [EVENT] on_init_swap:{}", eventBox.toString());
        // Delegate the event handling to the SingleSwapLpController
        // singleSwapController.onEventInitSwap(eventBox);
        // Return a success response
        return new Result(200, "");
    }

    /**
     * Handles the confirmation of a swap event.
     *
     * @param eventBox The event data containing swap confirmation details.
     * @return A Result object indicating the success of the operation.
     */
    @PostMapping("on_confirm_swap")
    public Result onConfirmSwap(@RequestBody EventConfirmSwapBox eventBox) {
        // Log the incoming event for debugging and monitoring
        // log.info("<- [EVENT] on_confirm_swap:{}", eventBox.toString());
        // Delegate the event handling to the SingleSwapLpController
        // singleSwapController.onEventConfirmSwap(eventBox);
        // Return a success response
        return new Result(200, "");
    }

    /**
     * Handles the refund of a swap event.
     *
     * @param eventBox The event data containing swap refund details.
     * @return A Result object indicating the success of the operation.
     */
    @PostMapping("on_refund_swap")
    public Result onRefundSwap(@RequestBody EventRefundSwapBox eventBox) {
        // Log the incoming event for debugging and monitoring
        // log.info("<- [EVENT] on_confirm_swap:{}",
        // JsonUtils.toCompactJsonString(eventBox));
        // Delegate the event handling to the SingleSwapLpController
        // singleSwapController.onEventRefundSwap(eventBox);
        // Return a success response
        return new Result(200, "");
    }
}
