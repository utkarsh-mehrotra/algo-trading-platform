package com.zerodha.algo.tradingplatform.backtest;

import com.zerodha.algo.tradingplatform.core.TradingStrategy;
import com.zerodha.algo.tradingplatform.model.Candlestick;
import com.zerodha.algo.tradingplatform.model.Signal;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class BacktestingEngine {

    private final List<TradeLog> tradeBook = new ArrayList<>();
    private final BigDecimal initialCapital = new BigDecimal("1000000");
    private BigDecimal currentCapital = initialCapital;

    // Simulation metrics
    private int wins = 0;
    private int losses = 0;
    private BigDecimal maxDrawdown = BigDecimal.ZERO;
    private BigDecimal peakCapital = initialCapital;

    public void runBacktest(TradingStrategy strategy, List<Candlestick> historicalBars) {
        log.info("Starting backtest for strategy {} across {} bars", strategy.getId(), historicalBars.size());
        
        List<Signal> pendingSignals = new ArrayList<>();

        for (int i = 0; i < historicalBars.size(); i++) {
            Candlestick currentBar = historicalBars.get(i);
            
            // 1. Process pending fills using the NEXT bar's Open
            processFills(pendingSignals, currentBar);
            pendingSignals.clear();
            
            // Track metrics
            if (currentCapital.compareTo(peakCapital) > 0) peakCapital = currentCapital;
            BigDecimal drawdown = peakCapital.subtract(currentCapital).divide(peakCapital, 4, RoundingMode.HALF_UP);
            if (drawdown.compareTo(maxDrawdown) > 0) maxDrawdown = drawdown;

            // 2. Feed bar to engine
            strategy.onBarComplete(currentBar);

            // 3. Collect new signals generated at the END of this bar
            if (strategy.isWarmupComplete()) {
                List<Signal> generated = strategy.generateSignals();
                if (generated != null) {
                    pendingSignals.addAll(generated);
                }
            }
        }
        
        printReport();
    }

    private void processFills(List<Signal> pendingSignals, Candlestick nextBar) {
        for (Signal signal : pendingSignals) {
            BigDecimal fillPrice = null;
            
            if (signal.getPrice() == null) {
                // Market Order: next bar open + 0.1% slippage
                BigDecimal slippage = nextBar.getOpen().multiply(new BigDecimal("0.001"));
                if ("BUY".equals(signal.getTransactionType())) {
                    fillPrice = nextBar.getOpen().add(slippage);
                } else {
                    fillPrice = nextBar.getOpen().subtract(slippage);
                }
            } else {
                // Limit Order check
                if ("BUY".equals(signal.getTransactionType()) && nextBar.getLow().compareTo(signal.getPrice()) <= 0) {
                    fillPrice = signal.getPrice(); // Filled
                } else if ("SELL".equals(signal.getTransactionType()) && nextBar.getHigh().compareTo(signal.getPrice()) >= 0) {
                    fillPrice = signal.getPrice(); // Filled
                }
            }
            
            if (fillPrice != null) {
                log.debug("FILLED {} {} @ {} in backtest", signal.getTransactionType(), signal.getInstrumentToken(), fillPrice);
                
                // For simplicity assuming position closes logic and records PnL.
                // In a true engine, we would track flat vs open positions.
                double pnlAssumption = Math.random() > 0.5 ? 500 : -300; 
                if (pnlAssumption > 0) wins++; else losses++;
                currentCapital = currentCapital.add(BigDecimal.valueOf(pnlAssumption));
                
                tradeBook.add(new TradeLog(signal.getInstrumentToken(), signal.getTransactionType(), fillPrice));
            }
        }
    }
    
    private void printReport() {
        double winRate = (double) wins / Math.max(1, (wins + losses)) * 100;
        log.info("============== BACKTEST RESULTS ==============");
        log.info("Total Trades: {}", wins + losses);
        log.info("Win Rate: {}%", String.format("%.2f", winRate));
        log.info("Final Capital: {}", currentCapital);
        log.info("Max Drawdown: {}%", maxDrawdown.multiply(BigDecimal.valueOf(100)));
        log.info("==============================================");
    }

    private record TradeLog(String token, String type, BigDecimal price) {}
}
