package com.alexjoker.quant.llm.service;

import com.alexjoker.quant.llm.dto.AiAnalysisSnapshotDTO;
import com.alexjoker.quant.llm.dto.AiAnalyzeResultDTO;
import com.alexjoker.quant.llm.dto.AiBatchAnalyzeResultDTO;
import com.alexjoker.quant.llm.entity.AiAnalysisSnapshotEntity;

import java.time.LocalDate;

public interface AiAnalysisService {
    AiAnalyzeResultDTO analyze(String symbolCode, LocalDate tradeDate);

    AiBatchAnalyzeResultDTO analyzeBuyCandidates(LocalDate tradeDate);

    AiAnalysisSnapshotDTO getSnapshot(String symbolCode, LocalDate analysisDate);

    AiAnalysisSnapshotEntity getSnapshotEntity(String symbolCode, LocalDate analysisDate);

    AiAnalysisSnapshotDTO toDto(AiAnalysisSnapshotEntity entity);
}
