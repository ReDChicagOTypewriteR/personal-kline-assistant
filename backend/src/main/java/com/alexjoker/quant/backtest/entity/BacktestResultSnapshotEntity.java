package com.alexjoker.quant.backtest.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("backtest_result_snapshot")
public class BacktestResultSnapshotEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String symbolCode;
    private String strategyType;
    private LocalDate startDate;
    private LocalDate endDate;
    private BigDecimal initialCapital;
    private BigDecimal positionRatio;
    private BigDecimal feeRate;
    private BigDecimal slippageRate;
    private Integer lotSize;
    private BigDecimal finalEquity;
    private BigDecimal totalReturnRate;
    private BigDecimal annualizedReturnRate;
    private BigDecimal maxDrawdownRate;
    private BigDecimal winRate;
    private Integer tradeCount;
    private String requestJson;
    private String resultJson;
    private String createdAt;
    private String updatedAt;
}
