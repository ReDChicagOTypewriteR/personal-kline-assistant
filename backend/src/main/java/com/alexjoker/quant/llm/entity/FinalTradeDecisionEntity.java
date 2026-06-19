package com.alexjoker.quant.llm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("final_trade_decision")
public class FinalTradeDecisionEntity {
    @TableId(type = IdType.AUTO)
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
    private String createdAt;
    private String updatedAt;
}
