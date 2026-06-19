package com.alexjoker.quant.backtest.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Data
public class BacktestBatchRunRequest {
    private List<String> symbolCodes;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate start;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate end;
    private BigDecimal initialCapital;
    private BigDecimal positionRatio;
    private BigDecimal feeRate;
    private BigDecimal slippageRate;
    private Integer lotSize;
    private String strategyType;
    private String dcaFrequency;
    private BigDecimal dcaBaseAmount;
    private BigDecimal dcaLowMultiplier;
    private BigDecimal dcaHighMultiplier;
}
