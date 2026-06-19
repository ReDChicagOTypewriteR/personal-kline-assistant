package com.alexjoker.quant.journal.converter;

import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.journal.dto.TradeJournalDTO;
import com.alexjoker.quant.journal.entity.TradeJournalEntity;
import com.alexjoker.quant.llm.entity.FinalTradeDecisionEntity;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.signal.entity.TechnicalSignalEntity;

public final class TradeJournalConverter {
    private TradeJournalConverter() {
    }

    public static TradeJournalEntity fromSignal(TechnicalSignalEntity signal, DailyKlineEntity kline, IndicatorSnapshotEntity indicator) {
        if (signal == null) {
            return null;
        }
        TradeJournalEntity entity = new TradeJournalEntity();
        entity.setSymbolCode(signal.getSymbolCode());
        entity.setTradeDate(signal.getTradeDate());
        entity.setSignalType(signal.getSignalType());
        entity.setSignalLevel(signal.getSignalLevel());
        entity.setTrendState(signal.getTrendState());
        entity.setTechnicalScore(signal.getTechnicalScore());
        entity.setSignalReason(signal.getReason());
        entity.setRiskNote(signal.getRiskNote());
        if (kline != null) {
            entity.setClosePrice(kline.getClosePrice());
        }
        if (indicator != null) {
            entity.setMa5(indicator.getMa5());
            entity.setMa10(indicator.getMa10());
            entity.setMa20(indicator.getMa20());
            entity.setMa60(indicator.getMa60());
            entity.setRsi14(indicator.getRsi14());
            entity.setAtr14(indicator.getAtr14());
            entity.setVolumeRatio(indicator.getVolumeRatio());
        }
        return entity;
    }

    public static TradeJournalEntity fromDecision(FinalTradeDecisionEntity decision, String signalFallback) {
        if (decision == null) {
            return null;
        }
        TradeJournalEntity entity = new TradeJournalEntity();
        entity.setSymbolCode(decision.getSymbolCode());
        entity.setTradeDate(decision.getDecisionDate());
        entity.setSignalType(signalFallback);
        entity.setTechnicalScore(decision.getTechnicalScore());
        entity.setAiRiskScore(decision.getAiRiskScore());
        entity.setAiRiskLevel(decision.getAiRiskLevel());
        entity.setFinalAction(decision.getFinalAction());
        entity.setPositionPolicy(decision.getPositionPolicy());
        entity.setDecisionReason(decision.getDecisionReason());
        entity.setRejectReason(decision.getRejectReason());
        return entity;
    }

    public static TradeJournalDTO toDto(TradeJournalEntity entity) {
        if (entity == null) {
            return null;
        }
        TradeJournalDTO dto = new TradeJournalDTO();
        dto.setId(entity.getId());
        dto.setSymbolCode(entity.getSymbolCode());
        dto.setTradeDate(entity.getTradeDate());
        dto.setSignalType(entity.getSignalType());
        dto.setSignalLevel(entity.getSignalLevel());
        dto.setTrendState(entity.getTrendState());
        dto.setClosePrice(entity.getClosePrice());
        dto.setMa5(entity.getMa5());
        dto.setMa10(entity.getMa10());
        dto.setMa20(entity.getMa20());
        dto.setMa60(entity.getMa60());
        dto.setRsi14(entity.getRsi14());
        dto.setAtr14(entity.getAtr14());
        dto.setVolumeRatio(entity.getVolumeRatio());
        dto.setTechnicalScore(entity.getTechnicalScore());
        dto.setSignalReason(entity.getSignalReason());
        dto.setRiskNote(entity.getRiskNote());
        dto.setAiRiskScore(entity.getAiRiskScore());
        dto.setAiRiskLevel(entity.getAiRiskLevel());
        dto.setFinalAction(entity.getFinalAction());
        dto.setPositionPolicy(entity.getPositionPolicy());
        dto.setDecisionReason(entity.getDecisionReason());
        dto.setRejectReason(entity.getRejectReason());
        dto.setExecuted(entity.getExecuted() != null && entity.getExecuted() == 1);
        dto.setExecutionDate(entity.getExecutionDate());
        dto.setExecutionPrice(entity.getExecutionPrice());
        dto.setExecutionNote(entity.getExecutionNote());
        dto.setFollowUpStatus(entity.getFollowUpStatus());
        dto.setFollowUpDate(entity.getFollowUpDate());
        dto.setFollowUpPrice(entity.getFollowUpPrice());
        dto.setFollowUpReturnRate(entity.getFollowUpReturnRate());
        dto.setFollowUpNote(entity.getFollowUpNote());
        return dto;
    }
}
