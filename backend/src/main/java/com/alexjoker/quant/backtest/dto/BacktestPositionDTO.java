package com.alexjoker.quant.backtest.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class BacktestPositionDTO {
    private String symbolCode;
    private Integer shares;
    private BigDecimal avgCost;
    private BigDecimal marketPrice;
    private BigDecimal marketValue;
    private BigDecimal unrealizedPnl;
    private BigDecimal unrealizedPnlRate;
    private String status;
}
