package com.alexjoker.quant.marketdata.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class DailyKlineDTO {
    private String symbolCode;
    private LocalDate tradeDate;
    private BigDecimal open;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal close;
    private BigDecimal volume;
    private BigDecimal amount;
}
