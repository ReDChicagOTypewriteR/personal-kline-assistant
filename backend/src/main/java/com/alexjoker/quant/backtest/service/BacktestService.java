package com.alexjoker.quant.backtest.service;

import com.alexjoker.quant.backtest.dto.BacktestBatchResultDTO;
import com.alexjoker.quant.backtest.dto.BacktestBatchRunRequest;
import com.alexjoker.quant.backtest.dto.BacktestResultDTO;
import com.alexjoker.quant.backtest.dto.BacktestRunRequest;

public interface BacktestService {
    BacktestResultDTO run(BacktestRunRequest request);

    BacktestBatchResultDTO runBatch(BacktestBatchRunRequest request);
}
