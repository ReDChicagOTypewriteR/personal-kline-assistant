package com.alexjoker.quant.signal.converter;

import com.alexjoker.quant.common.util.TextUtil;
import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.signal.dto.TechnicalSignalDTO;
import com.alexjoker.quant.signal.entity.TechnicalSignalEntity;

public final class TechnicalSignalConverter {
    private TechnicalSignalConverter() {
    }

    public static TechnicalSignalDTO toDto(TechnicalSignalEntity signal, DailyKlineEntity kline, IndicatorSnapshotEntity indicator) {
        if (signal == null) {
            return null;
        }
        TechnicalSignalDTO dto = new TechnicalSignalDTO();
        dto.setSymbolCode(signal.getSymbolCode());
        dto.setTradeDate(signal.getTradeDate());
        if (kline != null) {
            dto.setClosePrice(kline.getClosePrice());
        }
        if (indicator != null) {
            dto.setMa5(indicator.getMa5());
            dto.setMa10(indicator.getMa10());
            dto.setMa20(indicator.getMa20());
            dto.setMa60(indicator.getMa60());
            dto.setRsi14(indicator.getRsi14());
            dto.setAtr14(indicator.getAtr14());
            dto.setVolumeRatio(indicator.getVolumeRatio());
        }
        dto.setTrendState(signal.getTrendState());
        dto.setSignalType(signal.getSignalType());
        dto.setSignalLevel(signal.getSignalLevel());
        dto.setTechnicalScore(signal.getTechnicalScore());
        dto.setEntryReference(signal.getEntryReference());
        dto.setStopLossReference(signal.getStopLossReference());
        dto.setTakeProfitReference(signal.getTakeProfitReference());
        dto.setReasons(TextUtil.splitLines(signal.getReason()));
        dto.setRiskNotes(TextUtil.splitLines(signal.getRiskNote()));
        return dto;
    }
}
