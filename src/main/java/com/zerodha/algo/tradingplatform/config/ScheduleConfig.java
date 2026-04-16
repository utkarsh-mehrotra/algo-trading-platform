package com.zerodha.algo.tradingplatform.config;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class ScheduleConfig {
    
    @NotBlank
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]–([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Must be in HH:mm–HH:mm format (Note: Use en-dash or hyphen interchangeably, but standardize checking)")
    private String entryWindow;
    
    @NotBlank
    @Pattern(regexp = "^([01]?[0-9]|2[0-3]):[0-5][0-9]$", message = "Must be in HH:mm format")
    private String exitBy;
}
