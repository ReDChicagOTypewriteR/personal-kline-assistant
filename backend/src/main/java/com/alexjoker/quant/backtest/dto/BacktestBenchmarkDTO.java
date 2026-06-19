package com.alexjoker.quant.backtest.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BacktestBenchmarkDTO {
    private BigDecimal finalEquity;
    private BigDecimal totalReturnRate;
    private BigDecimal annualizedReturnRate;
    private BigDecimal maxDrawdownRate;
    private Integer shares;
    private BigDecimal cash;
    private BigDecimal startPrice;
    private BigDecimal endPrice;
}
