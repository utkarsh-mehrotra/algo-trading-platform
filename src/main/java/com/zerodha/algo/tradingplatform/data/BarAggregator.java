package com.zerodha.algo.tradingplatform.data;

import com.zerodha.algo.tradingplatform.core.MarketDataService;
import com.zerodha.algo.tradingplatform.model.Candlestick;
import com.zerodha.algo.tradingplatform.model.Tick;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class BarAggregator {

    private final MarketDataService marketDataService;
    // Map<InstrumentToken_Timeframe, Candlestick>
    private final Map<String, Candlestick> currentBars = new ConcurrentHashMap<>();

    // Support standard timeframes: 1m (60s), 5m (300s), 15m (900s)
    private static final int[] TIMEFRAMES_SEC = {60, 300, 900};
    private static final String[] TIMEFRAME_LABELS = {"1m", "5m", "15m"};

    public void processTick(Tick tick) {
        Instant ts = tick.getExchangeTimestamp();
        if (ts == null) return;
        
        long epochSec = ts.getEpochSecond();

        for (int i = 0; i < TIMEFRAMES_SEC.length; i++) {
            int tfSec = TIMEFRAMES_SEC[i];
            String tfLabel = TIMEFRAME_LABELS[i];
            long barStartSec = (epochSec / tfSec) * tfSec;
            Instant barStart = Instant.ofEpochSecond(barStartSec);
            Instant barEnd = barStart.plusSeconds(tfSec);

            String key = tick.getInstrumentToken() + "_" + tfLabel;

            currentBars.compute(key, (k, existingBar) -> {
                if (existingBar == null || existingBar.getStartTime().isBefore(barStart)) {
                    // Previous bar completed
                    if (existingBar != null) {
                        onBarComplete(existingBar);
                    }
                    // Create new bar
                    Candlestick newBar = new Candlestick();
                    newBar.setInstrumentToken(tick.getInstrumentToken());
                    newBar.setTimeframe(tfLabel);
                    newBar.setStartTime(barStart);
                    newBar.setEndTime(barEnd);
                    newBar.applyTick(tick);
                    return newBar;
                } else if (existingBar.getStartTime().equals(barStart)) {
                    // Update existing bar
                    existingBar.applyTick(tick);
                    return existingBar;
                } else {
                    // Late tick for an older bar, drop or handle separately
                    log.warn("Late tick received for {}: {} < {}", k, barStart, existingBar.getStartTime());
                    return existingBar;
                }
            });
        }
    }

    private void onBarComplete(Candlestick bar) {
        log.debug("Bar Completed: {} {} {}", bar.getInstrumentToken(), bar.getTimeframe(), bar.getStartTime());
        // For VWAP and indicators, we would compute them here or inside the data service
        marketDataService.saveCompletedBar(bar);
        
        // StrategyManager.onBarComplete(bar) would also be invoked here via an EventBus or direct bean reference
    }
}
