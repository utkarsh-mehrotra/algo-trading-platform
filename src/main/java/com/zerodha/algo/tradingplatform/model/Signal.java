package com.zerodha.algo.tradingplatform.model;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class Signal {
    private String strategyId;
    private String instrumentToken;
    private String transactionType; // BUY, SELL
    private BigDecimal price; // suggested limit price
    private Instant generatedAt;
    private String reason; // e.g. "EMA Crossover"
}
