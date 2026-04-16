package com.zerodha.algo.tradingplatform.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class Tick {
    private String instrumentToken;
    private Instant exchangeTimestamp;
    private BigDecimal lastPrice;
    private long volume;
    private BigDecimal bestBidPrice;
    private BigDecimal bestAskPrice;
    private long bestBidQty;
    private long bestAskQty;
    private BigDecimal openInterest;
}
