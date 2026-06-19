package com.alexjoker.quant.backtest.dto;

import lombok.Data;

@Data
public class BacktestRecordDetailDTO {
    private BacktestRecordSummaryDTO summary;
    private BacktestRunRequest request;
    private BacktestResultDTO result;
}
