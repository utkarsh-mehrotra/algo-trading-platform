package com.zerodha.algo.tradingplatform.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.List;

@Data
public class TradingConfig {
    
    @Valid
    @NotEmpty
    private List<StrategyConfig> strategies;
}
