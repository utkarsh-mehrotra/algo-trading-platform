package com.zerodha.algo.tradingplatform.execution;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "orders")
@Data
public class OrderEntity {
    
    @Id
    private String internalOrderId;
    private String brokerOrderId;
    private String instrumentToken;
    private String tradingsymbol;
    private String transactionType; 
    private String orderType; 
    private String product; 
    private String validity; 
    private BigDecimal price; 
    private BigDecimal triggerPrice;
    
    private int totalQty;
    private int filledQty;
    private int pendingQty;
    private int cancelledQty;
    
    private BigDecimal averageFilledPrice;
    
    private String status;
    private Instant createdAt;
    private Instant updatedAt;
    
    private String strategyId;
}
