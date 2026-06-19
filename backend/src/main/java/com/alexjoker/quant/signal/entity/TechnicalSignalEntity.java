package com.alexjoker.quant.signal.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("technical_signal")
public class TechnicalSignalEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String symbolCode;
    private LocalDate tradeDate;
    private String signalType;
    private String signalLevel;
    private String trendState;
    private Integer technicalScore;
    private BigDecimal entryReference;
    private BigDecimal stopLossReference;
    private BigDecimal takeProfitReference;
    private String reason;
    private String riskNote;
    private String createdAt;
    private String updatedAt;
}
