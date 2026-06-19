package com.alexjoker.quant.backtest.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BacktestRunRequest {
    @NotBlank(message = "symbolCode 不能为空")
    private String symbolCode;
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
