package com.alexjoker.quant.backtest.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BacktestSummaryDTO {
    private BigDecimal initialCapital;
    private BigDecimal finalEquity;
    private BigDecimal totalReturnRate;
    private BigDecimal annualizedReturnRate;
    private BigDecimal maxDrawdownRate;
    private BigDecimal realizedPnl;
    private Integer tradeCount;
    private Integer winCount;
    private Integer lossCount;
    private BigDecimal winRate;
    private BigDecimal profitLossRatio;
}
