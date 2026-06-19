package com.alexjoker.quant.indicator.service;

import com.alexjoker.quant.indicator.dto.IndicatorSnapshotDTO;

import java.time.LocalDate;
import java.util.List;

public interface IndicatorService {
    int calculateForSymbol(String symbolCode);

    int calculateForAllEnabledSymbols();

    List<IndicatorSnapshotDTO> listIndicators(String symbolCode, LocalDate start, LocalDate end);

    IndicatorSnapshotDTO getLatestIndicator(String symbolCode);
}
