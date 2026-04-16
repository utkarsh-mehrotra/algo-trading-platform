package com.zerodha.algo.tradingplatform.strategy;

import com.zerodha.algo.tradingplatform.config.StrategyConfig;
import com.zerodha.algo.tradingplatform.core.TradingStrategy;
import com.zerodha.algo.tradingplatform.model.Candlestick;

import java.util.ArrayList;
import java.util.List;

public abstract class AbstractTradingStrategy implements TradingStrategy {

    protected final StrategyConfig config;
    protected int barsProcessed = 0;
    
    // Maintain a rolling window of historical bars if subclasses need them
    protected final List<Candlestick> history = new ArrayList<>();

    public AbstractTradingStrategy(StrategyConfig config) {
        this.config = config;
    }

    @Override
    public String getId() {
        return config.getId();
    }

    @Override
    public boolean isWarmupComplete() {
        return barsProcessed >= config.getWarmupBars();
    }

    @Override
    public void onMarketOpen() {
        // Default no-op
    }

    @Override
    public void onMarketClose() {
        history.clear();
        barsProcessed = 0;
    }
}
