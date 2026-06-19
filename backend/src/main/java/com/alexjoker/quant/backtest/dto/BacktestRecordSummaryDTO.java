package com.alexjoker.quant.backtest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BacktestRecordSummaryDTO {
    private Long id;
    private String symbolCode;
    private String strategyType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal initialCapital;
    private BigDecimal positionRatio;
    private BigDecimal feeRate;
    private BigDecimal slippageRate;
    private Integer lotSize;
    private BigDecimal finalEquity;
    private BigDecimal totalReturnRate;
    private BigDecimal annualizedReturnRate;
    private BigDecimal maxDrawdownRate;
    private BigDecimal benchmarkReturnRate;
    private BigDecimal benchmarkMaxDrawdownRate;
    private BigDecimal excessReturnRate;
    private BigDecimal winRate;
    private Integer tradeCount;
    private String createdAt;
}
