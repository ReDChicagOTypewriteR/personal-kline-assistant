package com.alexjoker.quant.symbol.service;

import com.alexjoker.quant.backtest.entity.BacktestResultSnapshotEntity;
import com.alexjoker.quant.backtest.mapper.BacktestResultSnapshotMapper;
import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.indicator.mapper.IndicatorSnapshotMapper;
import com.alexjoker.quant.journal.entity.TradeJournalEntity;
import com.alexjoker.quant.journal.mapper.TradeJournalMapper;
import com.alexjoker.quant.llm.entity.AiAnalysisSnapshotEntity;
import com.alexjoker.quant.llm.entity.FinalTradeDecisionEntity;
import com.alexjoker.quant.llm.mapper.AiAnalysisSnapshotMapper;
import com.alexjoker.quant.llm.mapper.FinalTradeDecisionMapper;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.marketdata.mapper.DailyKlineMapper;
import com.alexjoker.quant.marketdata.service.KlineCacheService;
import com.alexjoker.quant.signal.entity.TechnicalSignalEntity;
import com.alexjoker.quant.signal.mapper.TechnicalSignalMapper;
import com.alexjoker.quant.watchlist.entity.WatchlistEntity;
import com.alexjoker.quant.watchlist.mapper.WatchlistMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SymbolDataCleanupService {
    private final WatchlistMapper watchlistMapper;
    private final DailyKlineMapper dailyKlineMapper;
    private final IndicatorSnapshotMapper indicatorSnapshotMapper;
    private final TechnicalSignalMapper technicalSignalMapper;
    private final AiAnalysisSnapshotMapper aiAnalysisSnapshotMapper;
    private final FinalTradeDecisionMapper finalTradeDecisionMapper;
    private final TradeJournalMapper tradeJournalMapper;
    private final BacktestResultSnapshotMapper backtestResultSnapshotMapper;
    private final KlineCacheService klineCacheService;

    public void deleteAllBySymbolCode(String symbolCode) {
        klineCacheService.evictSymbol(symbolCode);
        watchlistMapper.delete(new LambdaQueryWrapper<WatchlistEntity>().eq(WatchlistEntity::getSymbolCode, symbolCode));
        dailyKlineMapper.delete(new LambdaQueryWrapper<DailyKlineEntity>().eq(DailyKlineEntity::getSymbolCode, symbolCode));
        indicatorSnapshotMapper.delete(new LambdaQueryWrapper<IndicatorSnapshotEntity>().eq(IndicatorSnapshotEntity::getSymbolCode, symbolCode));
        technicalSignalMapper.delete(new LambdaQueryWrapper<TechnicalSignalEntity>().eq(TechnicalSignalEntity::getSymbolCode, symbolCode));
        aiAnalysisSnapshotMapper.delete(new LambdaQueryWrapper<AiAnalysisSnapshotEntity>().eq(AiAnalysisSnapshotEntity::getSymbolCode, symbolCode));
        finalTradeDecisionMapper.delete(new LambdaQueryWrapper<FinalTradeDecisionEntity>().eq(FinalTradeDecisionEntity::getSymbolCode, symbolCode));
        tradeJournalMapper.delete(new LambdaQueryWrapper<TradeJournalEntity>().eq(TradeJournalEntity::getSymbolCode, symbolCode));
        backtestResultSnapshotMapper.delete(new LambdaQueryWrapper<BacktestResultSnapshotEntity>().eq(BacktestResultSnapshotEntity::getSymbolCode, symbolCode));
    }
}
