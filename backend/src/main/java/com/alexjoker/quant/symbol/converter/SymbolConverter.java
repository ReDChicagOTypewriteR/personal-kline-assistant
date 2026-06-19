package com.alexjoker.quant.symbol.converter;

import com.alexjoker.quant.symbol.dto.SymbolDTO;
import com.alexjoker.quant.symbol.entity.SymbolEntity;

public final class SymbolConverter {
    private SymbolConverter() {
    }

    public static SymbolDTO toDto(SymbolEntity entity) {
        if (entity == null) {
            return null;
        }
        SymbolDTO dto = new SymbolDTO();
        dto.setId(entity.getId());
        dto.setSymbolCode(entity.getSymbolCode());
        dto.setSymbolName(entity.getSymbolName());
        dto.setMarket(entity.getMarket());
        dto.setAssetType(entity.getAssetType());
        dto.setCurrency(entity.getCurrency());
        dto.setEnabled(entity.getEnabled());
        return dto;
    }
}
