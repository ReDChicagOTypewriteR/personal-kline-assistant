package com.alexjoker.quant.marketdata.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@TableName("daily_kline")
public class DailyKlineEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String symbolCode;
    private LocalDate tradeDate;
    @TableField("open_price")
    private BigDecimal openPrice;
    @TableField("high_price")
    private BigDecimal highPrice;
    @TableField("low_price")
    private BigDecimal lowPrice;
    @TableField("close_price")
    private BigDecimal closePrice;
    private BigDecimal volume;
    private BigDecimal amount;
    private String createdAt;
    private String updatedAt;
}
