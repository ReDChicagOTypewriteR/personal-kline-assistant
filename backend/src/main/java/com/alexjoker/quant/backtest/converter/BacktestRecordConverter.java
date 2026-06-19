package com.alexjoker.quant.backtest.converter;

import com.alexjoker.quant.backtest.dto.BacktestRecordSummaryDTO;
import com.alexjoker.quant.backtest.dto.BacktestResultDTO;
import com.alexjoker.quant.backtest.entity.BacktestResultSnapshotEntity;

public final class BacktestRecordConverter {
    private BacktestRecordConverter() {
    }

    public static BacktestRecordSummaryDTO toSummary(BacktestResultSnapshotEntity entity) {
        if (entity == null) {
            return null;
        }
        BacktestRecordSummaryDTO dto = new BacktestRecordSummaryDTO();
        dto.setId(entity.getId());
        dto.setSymbolCode(entity.getSymbolCode());
        dto.setStrategyType(entity.getStrategyType());
        dto.setStartDate(entity.getStartDate());
        dto.setEndDate(entity.getEndDate());
        dto.setInitialCapital(entity.getInitialCapital());
        dto.setPositionRatio(entity.getPositionRatio());
        dto.setFeeRate(entity.getFeeRate());
        dto.setSlippageRate(entity.getSlippageRate());
        dto.setLotSize(entity.getLotSize());
        dto.setFinalEquity(entity.getFinalEquity());
        dto.setTotalReturnRate(entity.getTotalReturnRate());
        dto.setAnnualizedReturnRate(entity.getAnnualizedReturnRate());
        dto.setMaxDrawdownRate(entity.getMaxDrawdownRate());
        dto.setWinRate(entity.getWinRate());
        dto.setTradeCount(entity.getTradeCount());
        dto.setCreatedAt(entity.getCreatedAt());
        return dto;
    }

    public static void applyBenchmark(BacktestRecordSummaryDTO dto, BacktestResultDTO result) {
        if (dto == null || result == null || result.getBenchmark() == null || result.getSummary() == null) {
            return;
        }
        dto.setBenchmarkReturnRate(result.getBenchmark().getTotalReturnRate());
        dto.setBenchmarkMaxDrawdownRate(result.getBenchmark().getMaxDrawdownRate());
        if (result.getSummary().getTotalReturnRate() != null && result.getBenchmark().getTotalReturnRate() != null) {
            dto.setExcessReturnRate(result.getSummary().getTotalReturnRate().subtract(result.getBenchmark().getTotalReturnRate()));
        }
    }
}
