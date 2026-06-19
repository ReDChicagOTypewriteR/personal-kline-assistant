package com.alexjoker.quant.marketdata.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImportKlineResultDTO {
    private String symbolCode;
    private int totalRows;
    private int successRows;
    private int failedRows;
    private String message;
}
