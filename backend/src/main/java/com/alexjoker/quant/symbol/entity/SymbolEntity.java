package com.alexjoker.quant.symbol.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("symbol")
public class SymbolEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String symbolCode;
    private String symbolName;
    private String market;
    private String assetType;
    private String currency;
    private Integer enabled;
    private String createdAt;
    private String updatedAt;
}
