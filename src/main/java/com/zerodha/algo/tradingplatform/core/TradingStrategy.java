package com.zerodha.algo.tradingplatform.core;

import com.zerodha.algo.tradingplatform.model.Candlestick;
import com.zerodha.algo.tradingplatform.model.Signal;
import java.util.List;

public interface TradingStrategy {
    String getId();
    
    void onBarComplete(Candlestick bar);
    void onMarketOpen();
    void onMarketClose();
    
    boolean isWarmupComplete();
    
    List<Signal> generateSignals();
}
