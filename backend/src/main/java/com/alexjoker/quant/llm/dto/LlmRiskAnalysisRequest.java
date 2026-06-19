package com.alexjoker.quant.llm.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class LlmRiskAnalysisRequest {
    private String symbolCode;
    private String assetName;
    private String market;
    private String assetType;
    private String trackingTarget;
    private LocalDate tradeDate;
    private String technicalSignal;
    private Integer technicalScore;
    private BigDecimal closePrice;
    private BigDecimal ma20;
    private BigDecimal rsi14;
    private BigDecimal atr14;
    private List<String> analysisScope = new ArrayList<>();
}
