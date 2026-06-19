package com.alexjoker.quant.backtest.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class BacktestTradeDTO {
    private LocalDate tradeDate;
    private String action;
    private BigDecimal price;
    private Integer shares;
    private BigDecimal amount;
    private BigDecimal fee;
    private BigDecimal pnl;
    private BigDecimal pnlRate;
    private String reason;
}
