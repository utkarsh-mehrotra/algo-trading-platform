package com.zerodha.algo.tradingplatform.config;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;
import java.util.List;

@Data
public class StrategyConfig {
    
    @NotBlank
    private String id;
    
    @NotBlank
    @com.fasterxml.jackson.annotation.JsonProperty("class")
    private String clazz; // mapped from 'class' in yaml
    
    @NotEmpty
    private List<String> instruments;
    
    @NotBlank
    private String timeframe;
    
    @Min(1)
    private int warmupBars;
    
    @Valid
    @NotNull
    private RiskConfig risk;
    
    @Valid
    @NotNull
    private ScheduleConfig schedule;
}
