package com.alexjoker.quant.backtest.service;

import com.alexjoker.quant.backtest.dto.BacktestRecordDetailDTO;
import com.alexjoker.quant.backtest.dto.BacktestRecordSummaryDTO;
import com.alexjoker.quant.backtest.dto.BacktestResultDTO;
import com.alexjoker.quant.backtest.dto.BacktestRunRequest;

import java.util.List;

public interface BacktestRecordService {
    Long save(BacktestRunRequest request, BacktestResultDTO result);

    List<BacktestRecordSummaryDTO> listLatest(String symbolCode, int limit);

    BacktestRecordDetailDTO detail(Long id);

    void deleteById(Long id);
}
