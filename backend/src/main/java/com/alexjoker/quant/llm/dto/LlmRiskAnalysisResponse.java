package com.alexjoker.quant.llm.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class LlmRiskAnalysisResponse {
    private String symbolCode;
    private LocalDate analysisDate;
    private Integer sentimentScore;
    private Integer riskScore;
    private String riskLevel;
    private String marketState;
    private String actionConstraint;
    private String summary;
    private List<String> positiveFactors = new ArrayList<>();
    private List<String> negativeFactors = new ArrayList<>();
    private List<String> riskFactors = new ArrayList<>();
    private String contrarianView;
    private String rawReport;
}
