package com.zerodha.algo.tradingplatform.core;

import com.zerodha.algo.tradingplatform.config.RiskConfig;
import com.zerodha.algo.tradingplatform.model.Instrument;
import com.zerodha.algo.tradingplatform.model.Signal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
@Service
@RequiredArgsConstructor
public class RiskManager {

    private BigDecimal dailyStartingCapital = new BigDecimal("1000000"); // Example starting capital
    private BigDecimal currentMtm = BigDecimal.ZERO;

    public boolean isKillSwitchActive() {
        // Kill switch if MTM loss > 5% of daily starting capital
        BigDecimal maxLoss = dailyStartingCapital.multiply(new BigDecimal("0.05")).negate();
        if (currentMtm.compareTo(maxLoss) <= 0) {
            log.error("KILL SWITCH ACTIVATED: Daily loss limit breached. MTM: {}, Max Loss: {}", currentMtm, maxLoss);
            return true;
        }
        return false;
    }

    public int calculatePositionSize(Signal signal, RiskConfig config, Instrument instrument) {
        if (isKillSwitchActive()) {
            return 0; // Reject signal
        }

        BigDecimal riskPct = config.getMaxRiskPerTradePct().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal capitalToRisk = dailyStartingCapital.multiply(riskPct);

        BigDecimal price = signal.getPrice(); // Signal price or current LTP
        BigDecimal stopLossPct = config.getStopLossPct().divide(BigDecimal.valueOf(100), 4, RoundingMode.HALF_UP);
        BigDecimal stopDistance = price.multiply(stopLossPct);

        if (stopDistance.compareTo(BigDecimal.ZERO) == 0) {
            return 0; // Prevent div by zero
        }

        // size = (capital * riskPct) / stopDistance
        BigDecimal qty = capitalToRisk.divide(stopDistance, 0, RoundingMode.DOWN);

        // Discretize by lot size
        int lotSize = instrument.getLotSize();
        int finalQty = (qty.intValue() / lotSize) * lotSize;

        return Math.max(lotSize, finalQty); // Must be at least 1 lot if passing, or could cap at max lots
    }

    public void updateMTM(BigDecimal realizedPnL, BigDecimal unrealizedPnL) {
        this.currentMtm = realizedPnL.add(unrealizedPnL);
    }
}
