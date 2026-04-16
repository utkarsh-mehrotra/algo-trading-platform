package com.zerodha.algo.tradingplatform.strategy;

import com.zerodha.algo.tradingplatform.config.StrategyConfig;
import com.zerodha.algo.tradingplatform.model.Candlestick;
import com.zerodha.algo.tradingplatform.model.Signal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@Scope("prototype") // Allow multiple distinct instances per YAML definition
public class EMACrossoverStrategy extends AbstractTradingStrategy {

    private BigDecimal currentShortEma;
    private BigDecimal currentLongEma;

    private static final int SHORT_PERIOD = 12;
    private static final int LONG_PERIOD = 26;

    public EMACrossoverStrategy(StrategyConfig config) {
        super(config);
    }

    @Override
    public void onBarComplete(Candlestick bar) {
        BigDecimal close = bar.getClose();
        
        // Calculate EMAs (simplified incremental algorithm)
        if (barsProcessed == 0) {
            currentShortEma = close;
            currentLongEma = close;
        } else {
            BigDecimal shortK = BigDecimal.valueOf(2.0 / (SHORT_PERIOD + 1));
            BigDecimal longK = BigDecimal.valueOf(2.0 / (LONG_PERIOD + 1));
            
            currentShortEma = close.subtract(currentShortEma).multiply(shortK).add(currentShortEma);
            currentLongEma = close.subtract(currentLongEma).multiply(longK).add(currentLongEma);
        }

        history.add(bar);
        if (history.size() > Math.max(SHORT_PERIOD, LONG_PERIOD)) {
            history.remove(0);
        }
        
        barsProcessed++;
    }

    @Override
    public List<Signal> generateSignals() {
        List<Signal> signals = new ArrayList<>();
        if (!isWarmupComplete() || history.size() < 2) {
            return signals;
        }

        // Logic check: Just crossed over?
        // Fast line goes above slow line -> BUY. Fast goes below -> SELL
        // (For a true crossover, we need previous states too, but stubbed simply here for architecture)
        
        if (currentShortEma.compareTo(currentLongEma) > 0) {
            signals.add(Signal.builder()
                    .strategyId(config.getId())
                    .instrumentToken(config.getInstruments().getFirst())
                    .transactionType("BUY")
                    .generatedAt(Instant.now())
                    .reason("EMA 12 crossed above EMA 26")
                    .build());
        }

        return signals;
    }
}
