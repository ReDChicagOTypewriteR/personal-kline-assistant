package com.alexjoker.quant.system.dto;

import lombok.Data;

import java.util.List;

@Data
public class CacheRefreshResultDTO {
    private String status;
    private String message;
    private String refreshedAt;
    private List<String> actions;
}
