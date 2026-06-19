package com.alexjoker.quant.dashboard.dto;

import com.alexjoker.quant.indicator.dto.IndicatorSnapshotDTO;
import com.alexjoker.quant.llm.dto.AiAnalysisSnapshotDTO;
import com.alexjoker.quant.llm.dto.FinalTradeDecisionDTO;
import com.alexjoker.quant.marketdata.dto.DailyKlineDTO;
import com.alexjoker.quant.signal.dto.TechnicalSignalDTO;
import com.alexjoker.quant.symbol.dto.SymbolDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class SymbolTechnicalDetailDTO {
    private SymbolDTO symbol;
    private List<DailyKlineDTO> klines = new ArrayList<>();
    private List<IndicatorSnapshotDTO> indicators = new ArrayList<>();
    private TechnicalSignalDTO latestSignal;
    private AiAnalysisSnapshotDTO aiAnalysis;
    private FinalTradeDecisionDTO finalDecision;
}
