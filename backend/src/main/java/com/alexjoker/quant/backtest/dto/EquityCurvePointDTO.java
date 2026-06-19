package com.alexjoker.quant.backtest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class EquityCurvePointDTO {
    private LocalDate tradeDate;
    private BigDecimal cash;
    private BigDecimal marketValue;
    private BigDecimal totalEquity;
    private BigDecimal dailyReturn;
    private BigDecimal drawdown;
    private Integer positionShares;
    private BigDecimal closePrice;
}
