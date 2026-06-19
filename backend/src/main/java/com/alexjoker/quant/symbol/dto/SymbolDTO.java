package com.alexjoker.quant.symbol.dto;

import lombok.Data;

@Data
public class SymbolDTO {
    private Long id;
    private String symbolCode;
    private String symbolName;
    private String market;
    private String assetType;
    private String currency;
    private Integer enabled;
}
