package com.alexjoker.quant.journal.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class TradeJournalDTO {
    private Long id;
    private String symbolCode;
    private LocalDate tradeDate;
    private String signalType;
    private String signalLevel;
    private String trendState;
    private BigDecimal closePrice;
    private BigDecimal ma5;
    private BigDecimal ma10;
    private BigDecimal ma20;
    private BigDecimal ma60;
    private BigDecimal rsi14;
    private BigDecimal atr14;
    private BigDecimal volumeRatio;
    private Integer technicalScore;
    private String signalReason;
    private String riskNote;
    private Integer aiRiskScore;
    private String aiRiskLevel;
    private String finalAction;
    private String positionPolicy;
    private String decisionReason;
    private String rejectReason;
    private Boolean executed;
    private LocalDate executionDate;
    private BigDecimal executionPrice;
    private String executionNote;
    private String followUpStatus;
    private LocalDate followUpDate;
    private BigDecimal followUpPrice;
    private BigDecimal followUpReturnRate;
    private String followUpNote;
}
