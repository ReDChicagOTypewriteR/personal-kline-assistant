package com.alexjoker.quant.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class MarketDataSourceOverviewDTO {
    private Integer totalCount;
    private Integer upCount;
    private Integer downCount;
    private Integer unknownCount;
    private String refreshedAt;
    private List<MarketDataSourceStatusDTO> sources;
}
