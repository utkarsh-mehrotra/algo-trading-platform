package com.zerodha.algo.tradingplatform.core;

import com.zerodha.algo.tradingplatform.model.Candlestick;
import java.util.List;

public interface MarketDataService {
    void subscribe(String instrumentToken);
    void unsubscribe(String instrumentToken);
    
    List<Candlestick> getHistoricalBars(String instrumentToken, String timeframe, int limit);
    void saveCompletedBar(Candlestick bar);
}
