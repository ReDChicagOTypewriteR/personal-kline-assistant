package com.alexjoker.quant.llm.dto;

import lombok.Data;

@Data
public class AiAnalyzeResultDTO {
    private AiAnalysisSnapshotDTO aiAnalysis;
    private FinalTradeDecisionDTO finalDecision;
    private boolean cached;
    private String message;
}
