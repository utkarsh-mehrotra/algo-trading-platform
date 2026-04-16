package com.zerodha.algo.tradingplatform.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.math.BigDecimal;

@Data
@com.fasterxml.jackson.databind.annotation.JsonNaming(com.fasterxml.jackson.databind.PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RiskConfig {
    
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal maxRiskPerTradePct;
    
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal stopLossPct;
    
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal targetPct;

    // Greek limitations for Options
    @DecimalMin("-1.0")
    @DecimalMax("1.0")
    private BigDecimal maxNetDelta;
    
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal maxVegaPctCapital;
    
    @DecimalMin("0.0")
    @DecimalMax("100.0")
    private BigDecimal maxThetaBurnDailyPct;
    
    @DecimalMin("0.0")
    private BigDecimal maxGammaPerRupee;
}
