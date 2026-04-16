package com.zerodha.algo.tradingplatform.strategy;

import com.zerodha.algo.tradingplatform.config.StrategyConfig;
import com.zerodha.algo.tradingplatform.model.Candlestick;
import com.zerodha.algo.tradingplatform.model.Signal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Scope("prototype")
public class ShortStraddleStrategy extends AbstractTradingStrategy {

    private boolean entryTakenToday = false;

    public ShortStraddleStrategy(StrategyConfig config) {
        super(config);
    }

    @Override
    public void onBarComplete(Candlestick bar) {
        barsProcessed++;
    }

    @Override
    public List<Signal> generateSignals() {
        List<Signal> signals = new ArrayList<>();
        
        // This strategy requires complex option chain logic to identify the ATM strike, 
        // fetch the PE and CE tokens, and issue SELL signals for both.
        // It relies on the InstrumentManager to cross-reference expiry and strikes.
        
        if (!entryTakenToday) {
            log.info("Checking schedule for Short Straddle ATM options...");
            // Stubbed execution
            signals.add(Signal.builder()
                    .strategyId(config.getId())
                    .transactionType("SELL")
                    .reason("Short Straddle Leg 1 CE")
                    .generatedAt(Instant.now())
                    .build());
            signals.add(Signal.builder()
                    .strategyId(config.getId())
                    .transactionType("SELL")
                    .reason("Short Straddle Leg 2 PE")
                    .generatedAt(Instant.now())
                    .build());
                    
            entryTakenToday = true;
        }

        return signals;
    }

    @Override
    public void onMarketClose() {
        super.onMarketClose();
        entryTakenToday = false;
    }
}
