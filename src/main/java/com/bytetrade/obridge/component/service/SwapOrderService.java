package com.bytetrade.obridge.component.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.bytetrade.obridge.db.SwapOrder;
import com.bytetrade.obridge.db.SwapOrderRepository;

@Service
public class SwapOrderService {
    @Autowired
    private SwapOrderRepository swapOrderRepository;

    public SwapOrder createSwapOrder(SwapOrder swapOrder) {
        return swapOrderRepository.save(swapOrder);
    }
}
