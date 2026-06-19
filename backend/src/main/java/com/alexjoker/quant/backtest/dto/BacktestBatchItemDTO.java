package com.alexjoker.quant.backtest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BacktestBatchItemDTO {
    private String symbolCode;
    private Boolean success;
    private String message;
    private Long recordId;
    private String strategyType;
    private LocalDate start;
    private LocalDate end;
    private BigDecimal finalEquity;
    private BigDecimal totalReturnRate;
    private BigDecimal benchmarkReturnRate;
    private BigDecimal excessReturnRate;
    private BigDecimal maxDrawdownRate;
    private BigDecimal benchmarkMaxDrawdownRate;
    private BigDecimal winRate;
    private Integer tradeCount;
}
