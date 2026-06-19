package com.alexjoker.quant.backtest.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class BacktestDataQualityDTO {
    private Integer totalRows;
    private Integer invalidPriceRows;
    private Integer missingVolumeRows;
    private Integer missingAmountRows;
    private Integer longCalendarGapCount;
    private Integer suspiciousChangeRows;
    private LocalDate firstTradeDate;
    private LocalDate lastTradeDate;
    private Boolean passed;
    private List<String> warnings = new ArrayList<>();
}
