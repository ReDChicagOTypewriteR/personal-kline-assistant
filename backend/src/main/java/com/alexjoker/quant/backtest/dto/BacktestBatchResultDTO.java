package com.alexjoker.quant.backtest.dto;

import lombok.Data;

import java.util.List;

@Data
public class BacktestBatchResultDTO {
    private Integer totalCount;
    private Integer successCount;
    private Integer failedCount;
    private List<BacktestBatchItemDTO> results;
}
