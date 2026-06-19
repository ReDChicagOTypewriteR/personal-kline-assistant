package com.alexjoker.quant.journal.service;

import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.journal.dto.TradeJournalDTO;
import com.alexjoker.quant.journal.dto.UpdateTradeJournalRequest;
import com.alexjoker.quant.llm.entity.FinalTradeDecisionEntity;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.signal.entity.TechnicalSignalEntity;

import java.time.LocalDate;
import java.util.List;

public interface TradeJournalService {
    void recordSignal(TechnicalSignalEntity signal, DailyKlineEntity kline, IndicatorSnapshotEntity indicator);

    void recordDecision(FinalTradeDecisionEntity decision);

    List<TradeJournalDTO> listLatest(int limit);

    List<TradeJournalDTO> listHistory(String symbolCode);

    TradeJournalDTO update(Long id, UpdateTradeJournalRequest request);

    void deleteById(Long id);

    int deleteByIds(List<Long> ids);

    int deleteBySignal(String symbolCode, LocalDate tradeDate);
}
