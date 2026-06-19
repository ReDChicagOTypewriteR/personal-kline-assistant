package com.alexjoker.quant.system.dto;

import lombok.Data;

@Data
public class MarketDataSourceStatusDTO {
    private String sourceCode;
    private String displayName;
    private String providerType;
    private String role;
    private Integer priority;
    private Boolean enabled;
    private String status;
    private String healthMessage;
    private String lastCheckedAt;
    private String lastSuccessAt;
    private String lastErrorAt;
    private String lastError;
    private Integer successCount;
    private Integer failureCount;
}
