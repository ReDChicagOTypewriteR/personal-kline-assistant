package com.alexjoker.quant.watchlist.converter;

import com.alexjoker.quant.watchlist.dto.WatchlistDTO;
import com.alexjoker.quant.watchlist.entity.WatchlistEntity;

public final class WatchlistConverter {
    private WatchlistConverter() {
    }

    public static WatchlistDTO toDto(WatchlistEntity entity) {
        if (entity == null) {
            return null;
        }
        WatchlistDTO dto = new WatchlistDTO();
        dto.setId(entity.getId());
        dto.setSymbolCode(entity.getSymbolCode());
        dto.setGroupName(entity.getGroupName());
        dto.setPriority(entity.getPriority());
        dto.setNote(entity.getNote());
        dto.setEnabled(entity.getEnabled());
        return dto;
    }
}
