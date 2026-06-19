package com.alexjoker.quant.symbol.service;

import com.alexjoker.quant.symbol.dto.CreateSymbolRequest;
import com.alexjoker.quant.symbol.dto.SymbolDTO;

import java.util.List;

public interface SymbolService {
    List<SymbolDTO> listAll();

    List<SymbolDTO> listEnabled();

    SymbolDTO create(CreateSymbolRequest request);

    void setEnabled(String symbolCode, boolean enabled);

    void deleteByCode(String symbolCode);

    SymbolDTO getByCode(String symbolCode);

    SymbolDTO getOrCreateCnSymbol(String symbolCode);

    SymbolDTO resolveCnSymbol(String symbolCode);

    boolean exists(String symbolCode);
}
