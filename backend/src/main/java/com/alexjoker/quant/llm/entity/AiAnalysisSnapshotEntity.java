package com.alexjoker.quant.llm.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("ai_analysis_snapshot")
public class AiAnalysisSnapshotEntity {
    @TableId(type = IdType.AUTO)
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
    private String createdAt;
    private String updatedAt;
}
