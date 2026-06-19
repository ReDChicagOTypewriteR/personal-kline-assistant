package com.alexjoker.quant.marketdata.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class MarketKlineDTO {
    private Long timestamp;
    private BigDecimal open;
    private BigDecimal close;
    private BigDecimal high;
    private BigDecimal low;
    private BigDecimal volume;
    private BigDecimal turnover;
}
