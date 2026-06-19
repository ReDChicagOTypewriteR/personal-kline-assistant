package com.alexjoker.quant.indicator.converter;

import com.alexjoker.quant.indicator.dto.IndicatorSnapshotDTO;
import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;

public final class IndicatorSnapshotConverter {
    private IndicatorSnapshotConverter() {
    }

    public static IndicatorSnapshotDTO toDto(IndicatorSnapshotEntity entity) {
        if (entity == null) {
            return null;
        }
        IndicatorSnapshotDTO dto = new IndicatorSnapshotDTO();
        dto.setSymbolCode(entity.getSymbolCode());
        dto.setTradeDate(entity.getTradeDate());
        dto.setMa5(entity.getMa5());
        dto.setMa10(entity.getMa10());
        dto.setMa20(entity.getMa20());
        dto.setMa60(entity.getMa60());
        dto.setRsi14(entity.getRsi14());
        dto.setAtr14(entity.getAtr14());
        dto.setVolumeMa20(entity.getVolumeMa20());
        dto.setVolumeRatio(entity.getVolumeRatio());
        dto.setFiveDayReturn(entity.getFiveDayReturn());
        dto.setDistanceToMa20(entity.getDistanceToMa20());
        return dto;
    }
}
