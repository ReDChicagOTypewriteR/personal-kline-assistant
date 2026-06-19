package com.alexjoker.quant.backtest.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.List;

@Data
public class BacktestResultDTO {
    private Long recordId;
    private String symbolCode;
    private String strategyType;
    private String executionMode;
    private LocalDate start;
    private LocalDate end;
    private BacktestDataQualityDTO dataQuality;
    private BacktestSummaryDTO summary;
    private BacktestBenchmarkDTO benchmark;
    private BacktestPositionDTO position;
    private List<BacktestTradeDTO> trades;
    private List<EquityCurvePointDTO> equityCurve;
}
