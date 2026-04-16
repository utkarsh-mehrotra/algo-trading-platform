package com.zerodha.algo.tradingplatform.core;

import com.zerodha.algo.tradingplatform.model.Order;
import com.zerodha.algo.tradingplatform.model.Signal;

public interface ExecutionEngine {
    Order placeOrder(Signal signal, int quantity);
    Order modifyOrder(Order order);
    void cancelOrder(String brokerOrderId);
    
    void reconcilePendingOrders();
}
