package com.alexjoker.quant.signal.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class TechnicalSignalDTO {
    private String symbolCode;
    private LocalDate tradeDate;
    private BigDecimal closePrice;
    private BigDecimal ma5;
    private BigDecimal ma10;
    private BigDecimal ma20;
    private BigDecimal ma60;
    private BigDecimal rsi14;
    private BigDecimal atr14;
    private BigDecimal volumeRatio;
    private String trendState;
    private String signalType;
    private String signalLevel;
    private Integer technicalScore;
    private BigDecimal entryReference;
    private BigDecimal stopLossReference;
    private BigDecimal takeProfitReference;
    private List<String> reasons = new ArrayList<>();
    private List<String> riskNotes = new ArrayList<>();
    private Integer aiSentimentScore;
    private Integer aiRiskScore;
    private String aiRiskLevel;
    private String finalAction;
    private String positionPolicy;
}
