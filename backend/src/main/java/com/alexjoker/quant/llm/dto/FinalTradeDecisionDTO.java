package com.alexjoker.quant.llm.dto;

import lombok.Data;

import java.time.LocalDate;

@Data
public class FinalTradeDecisionDTO {
    private Long id;
    private String symbolCode;
    private LocalDate decisionDate;
    private String technicalSignal;
    private Integer technicalScore;
    private Integer aiSentimentScore;
    private Integer aiRiskScore;
    private String aiRiskLevel;
    private String finalAction;
    private String positionPolicy;
    private String decisionReason;
    private String rejectReason;
}
