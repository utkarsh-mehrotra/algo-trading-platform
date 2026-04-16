package com.zerodha.algo.tradingplatform.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class Order {
    private String internalOrderId;
    private String brokerOrderId;
    private String instrumentToken;
    private String tradingsymbol;
    private String transactionType; // BUY, SELL
    private String orderType; // MARKET, LIMIT, SL, SL-M
    private String product; // MIS, CNC, NRML
    private String validity; // DAY, IOC
    private BigDecimal price; // Limit price
    private BigDecimal triggerPrice;
    
    private int totalQty;
    private int filledQty;
    private int pendingQty;
    private int cancelledQty;
    
    private BigDecimal averageFilledPrice;
    
    private OrderStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    
    private String strategyId;
}
