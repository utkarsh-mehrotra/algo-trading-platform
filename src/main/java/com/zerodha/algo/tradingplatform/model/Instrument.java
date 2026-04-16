package com.zerodha.algo.tradingplatform.model;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class Instrument {
    private String instrumentToken;
    private String exchangeToken;
    private String tradingsymbol;
    private String name;
    private BigDecimal lastPrice;
    private LocalDate expiry;
    private String strike;
    private BigDecimal tickSize;
    private int lotSize;
    private String instrumentType; // EQ, CE, PE, FUT
    private String segment;
    private String exchange;
}
