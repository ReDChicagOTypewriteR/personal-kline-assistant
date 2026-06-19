package com.alexjoker.quant.llm.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class AiAnalysisSnapshotDTO {
    private Long id;
    private String symbolCode;
    private LocalDate analysisDate;
    private String assetName;
    private String market;
    private String assetType;
    private Integer sentimentScore;
    private Integer riskScore;
    private String riskLevel;
    private String marketState;
    private String actionConstraint;
    private String summary;
    private String positiveFactors;
    private String negativeFactors;
    private String riskFactors;
    private String contrarianView;
    private String rawReport;
    private String sourceSystem;
    private String callStatus;
    private String errorMessage;
}
