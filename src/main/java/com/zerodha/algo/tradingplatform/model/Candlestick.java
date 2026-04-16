package com.zerodha.algo.tradingplatform.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.Instant;

@Data
public class Candlestick {
    private String instrumentToken;
    private String timeframe; // e.g., "1m", "5m", "15m"
    private Instant startTime;
    private Instant endTime;
    
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private long volume;
    private BigDecimal vwap;

    public void applyTick(Tick tick) {
        if (open == null) open = tick.getLastPrice();
        if (high == null || tick.getLastPrice().compareTo(high) > 0) high = tick.getLastPrice();
        if (low == null || tick.getLastPrice().compareTo(low) < 0) low = tick.getLastPrice();
        close = tick.getLastPrice();
        volume += tick.getVolume();
    }
}
