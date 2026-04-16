package com.zerodha.algo.tradingplatform.data;

import static org.junit.jupiter.api.Assertions.*;

import com.zerodha.algo.tradingplatform.core.MarketDataService;
import com.zerodha.algo.tradingplatform.model.Candlestick;
import com.zerodha.algo.tradingplatform.model.Tick;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

class BarAggregatorTest {

    private BarAggregator barAggregator;
    private List<Candlestick> savedBars;

    @BeforeEach
    void setUp() {
        savedBars = new ArrayList<>();
        // Simple anonymous implementation of MarketDataService to avoid Mockito issues on Java 23
        MarketDataService mockService = new MarketDataService() {
            @Override public void subscribe(String token) {}
            @Override public void unsubscribe(String token) {}
            @Override public List<Candlestick> getHistoricalBars(String t, String tf, int l) { return null; }
            @Override public void saveCompletedBar(Candlestick bar) {
                savedBars.add(bar);
            }
        };
        barAggregator = new BarAggregator(mockService);
    }

    @Test
    void testProcessTick_AggregatesCorrectly() {
        String token = "TOKEN1";
        Instant now = Instant.parse("2026-04-17T09:15:00Z"); 
        
        Tick tick1 = new Tick();
        tick1.setInstrumentToken(token);
        tick1.setExchangeTimestamp(now);
        tick1.setLastPrice(new BigDecimal("100.0"));
        tick1.setVolume(10);

        Tick tick2 = new Tick();
        tick2.setInstrumentToken(token);
        tick2.setExchangeTimestamp(now.plusSeconds(30));
        tick2.setLastPrice(new BigDecimal("105.0"));
        tick2.setVolume(20);

        Tick tick3 = new Tick();
        tick3.setInstrumentToken(token);
        tick3.setExchangeTimestamp(now.plusSeconds(60)); 
        tick3.setLastPrice(new BigDecimal("102.0"));
        tick3.setVolume(5);

        barAggregator.processTick(tick1);
        barAggregator.processTick(tick2);
        assertTrue(savedBars.isEmpty(), "No bars should be saved yet");

        barAggregator.processTick(tick3);
        assertFalse(savedBars.isEmpty(), "Bars should be saved now");

        // Verify the 1m bar
        Candlestick bar1m = savedBars.stream()
                .filter(b -> "1m".equals(b.getTimeframe()))
                .findFirst().orElseThrow();

        assertEquals(new BigDecimal("100.0"), bar1m.getOpen());
        assertEquals(new BigDecimal("105.0"), bar1m.getHigh());
        assertEquals(new BigDecimal("100.0"), bar1m.getLow());
        assertEquals(new BigDecimal("105.0"), bar1m.getClose());
        assertEquals(30, bar1m.getVolume());
    }
}
