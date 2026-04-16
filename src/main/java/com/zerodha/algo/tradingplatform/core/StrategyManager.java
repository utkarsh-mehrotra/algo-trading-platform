package com.zerodha.algo.tradingplatform.core;

import com.zerodha.algo.tradingplatform.config.StrategyConfig;
import com.zerodha.algo.tradingplatform.config.TradingConfig;
import com.zerodha.algo.tradingplatform.model.Candlestick;
import com.zerodha.algo.tradingplatform.model.Signal;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StrategyManager {

    private final TradingConfig tradingConfig;
    private final ApplicationContext applicationContext;
    private final ExecutionEngine executionEngine;
    private final MarketDataService marketDataService;
    private final MeterRegistry meterRegistry;

    // Map<InstrumentToken_Timeframe, List<TradingStrategy>>
    private final Map<String, List<TradingStrategy>> routingMap = new ConcurrentHashMap<>();
    
    private final List<TradingStrategy> activeStrategies = new ArrayList<>();

    @PostConstruct
    public void initializeStrategies() {
        for (StrategyConfig config : tradingConfig.getStrategies()) {
            try {
                // Dynamically instantiate the strategy based on the 'clazz' name
                String fqcn = "com.zerodha.algo.tradingplatform.strategy." + config.getClazz();
                Class<?> clazz = Class.forName(fqcn);
                TradingStrategy strategy = (TradingStrategy) applicationContext.getBean(clazz, config);
                
                activeStrategies.add(strategy);
                
                // Route instruments
                for (String instrument : config.getInstruments()) {
                    String subscriptionKey = instrument + "_" + config.getTimeframe();
                    routingMap.computeIfAbsent(subscriptionKey, k -> new ArrayList<>()).add(strategy);
                    // Also trigger market data service subscription if not already subscribed
                    marketDataService.subscribe(instrument);
                }
                
            } catch (Exception e) {
                log.error("Failed to initialize strategy class: {}", config.getClazz(), e);
                throw new IllegalStateException("Startup failed due to strategy load error", e);
            }
        }
        log.info("Initialized {} trading strategies.", activeStrategies.size());
    }

    public void dispatchBarComplete(Candlestick bar) {
        String key = bar.getInstrumentToken() + "_" + bar.getTimeframe();
        List<TradingStrategy> strategies = routingMap.get(key);
        
        if (strategies != null) {
            for (TradingStrategy strategy : strategies) {
                try {
                    strategy.onBarComplete(bar);
                    if (strategy.isWarmupComplete()) {
                        List<Signal> signals = strategy.generateSignals();
                        if (signals != null && !signals.isEmpty()) {
                            processSignals(signals);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error executing strategy {}", strategy.getId(), e);
                }
            }
        }
    }

    private void processSignals(List<Signal> signals) {
        for (Signal signal : signals) {
            log.info("Signal generated: {} on {} Action: {}", signal.getStrategyId(), signal.getInstrumentToken(), signal.getTransactionType());
            
            Timer.Sample sample = Timer.start(meterRegistry);
            try {
                // In a real implementation we resolve standard order quantity using Risk manager
                executionEngine.placeOrder(signal, 1);
            } finally {
                sample.stop(meterRegistry.timer("algo.latency.signal_to_order", 
                        "strategy", signal.getStrategyId()));
            }
        }
    }
}
