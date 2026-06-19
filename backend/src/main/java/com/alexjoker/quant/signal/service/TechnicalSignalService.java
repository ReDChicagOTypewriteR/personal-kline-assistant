package com.alexjoker.quant.signal.service;

import com.alexjoker.quant.signal.dto.TechnicalSignalDTO;

import java.time.LocalDate;
import java.util.List;

public interface TechnicalSignalService {
    List<TechnicalSignalDTO> generateSignalForSymbol(String symbolCode);

    int generateSignalsForAllEnabledSymbols();

    List<TechnicalSignalDTO> listLatestSignals();

    List<TechnicalSignalDTO> listSignalHistory(String symbolCode);

    int deleteSignals(String symbolCode, LocalDate tradeDate);
}
