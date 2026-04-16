package com.zerodha.algo.tradingplatform.data;

import com.zerodha.algo.tradingplatform.model.Tick;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class TickDeduplicator {

    // Store up to 10,000 recent tick signatures to deduplicate
    private static final int MAX_ENTRIES = 10000;

    private final Map<String, Boolean> recentTicks = Collections.synchronizedMap(
            new LinkedHashMap<String, Boolean>(MAX_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<String, Boolean> eldest) {
                    return size() > MAX_ENTRIES;
                }
            });

    /**
     * Checks if the tick is already processed based on its unique signature.
     * @param tick incoming tick
     * @return true if new and added, false if duplicate
     */
    public boolean isNewTick(Tick tick) {
        if (tick == null || tick.getInstrumentToken() == null || tick.getExchangeTimestamp() == null) {
            return false;
        }
        
        // The signature is (token_exchangeTimestamp_lastPrice)
        // Since Kite might emit ticks with same timestamp but different prices (in edge cases), price is added.
        // But prompt explicitly specified (token, exchange_timestamp) tuple deduplication.
        String signature = tick.getInstrumentToken() + "_" + tick.getExchangeTimestamp().toEpochMilli();
        
        return recentTicks.putIfAbsent(signature, Boolean.TRUE) == null;
    }
}
