package com.alexjoker.quant.llm.service;

import com.alexjoker.quant.llm.dto.FinalTradeDecisionDTO;
import com.alexjoker.quant.llm.entity.AiAnalysisSnapshotEntity;
import com.alexjoker.quant.llm.entity.FinalTradeDecisionEntity;
import com.alexjoker.quant.signal.entity.TechnicalSignalEntity;

import java.time.LocalDate;
import java.util.List;

public interface FinalDecisionService {
    FinalTradeDecisionDTO generate(TechnicalSignalEntity signal, AiAnalysisSnapshotEntity aiAnalysis);

    FinalTradeDecisionDTO generate(String symbolCode, LocalDate decisionDate);

    FinalTradeDecisionDTO getLatestForSymbol(String symbolCode);

    List<FinalTradeDecisionDTO> listLatest();

    List<FinalTradeDecisionDTO> listHistory(String symbolCode);

    FinalTradeDecisionDTO toDto(FinalTradeDecisionEntity entity);
}
