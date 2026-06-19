package com.alexjoker.quant.indicator.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("indicator_snapshot")
public class IndicatorSnapshotEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String symbolCode;
    private LocalDate tradeDate;
    private BigDecimal ma5;
    private BigDecimal ma10;
    private BigDecimal ma20;
    private BigDecimal ma60;
    private BigDecimal rsi14;
    private BigDecimal atr14;
    private BigDecimal volumeMa20;
    private BigDecimal volumeRatio;
    private BigDecimal fiveDayReturn;
    private BigDecimal distanceToMa20;
    private String createdAt;
    private String updatedAt;
}
