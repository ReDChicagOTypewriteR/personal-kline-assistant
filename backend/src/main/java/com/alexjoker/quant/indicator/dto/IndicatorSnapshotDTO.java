package com.alexjoker.quant.indicator.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class IndicatorSnapshotDTO {
    private String symbolCode;
    private LocalDate tradeDate;
    private BigDecimal ma5;
    private BigDecimal ma10;
    private BigDecimal ma20;
    private BigDecimal ma60;
    private BigDecimal rsi14;
    private BigDecimal atr14;
    private BigDecimal volumeMa20;
    private BigDecimal volumeRatio;
    private BigDecimal fiveDayReturn;
    private BigDecimal distanceToMa20;
}
