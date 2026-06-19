package com.alexjoker.quant.symbol.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class CreateSymbolRequest {
    @NotBlank
    private String symbolCode;
    private String symbolName;
    @NotBlank
    private String market;
    private String assetType;
    private String currency;
}
