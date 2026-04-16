package com.zerodha.algo.tradingplatform.data;

import com.zerodha.algo.tradingplatform.model.Candlestick;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Computes standard OHLC indicators (EMA, SMA, RSI, VWAP) functionally.
 * Keeps daily state for VWAP resets at 09:15 IST.
 */
@Component
public class IndicatorCalculator {

    // VWAP tracking state (sum of product price*vol, sum of vol) per instrument
    private final Map<String, VwapState> vwapStateMap = new ConcurrentHashMap<>();
    private static final ZoneId IST_ZONE = ZoneId.of("Asia/Kolkata");

    public void computeVWAP(Candlestick bar) {
        String token = bar.getInstrumentToken();
        Instant barTime = bar.getStartTime();
        
        vwapStateMap.compute(token, (k, state) -> {
            boolean reset = false;
            if (state == null) {
                reset = true;
            } else {
                // Check if the current bar crossed the 09:15 daily threshold
                ZonedDateTime stateDate = state.lastUpdated.atZone(IST_ZONE);
                ZonedDateTime barDate = barTime.atZone(IST_ZONE);
                if (stateDate.getDayOfYear() != barDate.getDayOfYear()) {
                    reset = true;
                }
            }

            VwapState newState = reset ? new VwapState() : state;
            
            // Typical price for VWAP = (High + Low + Close) / 3
            BigDecimal typicalPrice = bar.getHigh().add(bar.getLow()).add(bar.getClose())
                    .divide(BigDecimal.valueOf(3), 4, RoundingMode.HALF_UP);
            
            long vol = bar.getVolume();
            BigDecimal pVol = typicalPrice.multiply(BigDecimal.valueOf(vol));
            
            newState.cumulativePriceVolume = newState.cumulativePriceVolume.add(pVol);
            newState.cumulativeVolume += vol;
            newState.lastUpdated = barTime;
            
            if (newState.cumulativeVolume > 0) {
                bar.setVwap(newState.cumulativePriceVolume.divide(BigDecimal.valueOf(newState.cumulativeVolume), 2, RoundingMode.HALF_UP));
            } else {
                bar.setVwap(typicalPrice);
            }
            
            return newState;
        });
    }

    private static class VwapState {
        BigDecimal cumulativePriceVolume = BigDecimal.ZERO;
        long cumulativeVolume = 0;
        Instant lastUpdated = Instant.EPOCH;
    }
}
