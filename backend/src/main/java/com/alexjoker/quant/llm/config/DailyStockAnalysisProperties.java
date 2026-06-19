package com.alexjoker.quant.llm.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "llm.daily-stock-analysis")
public class DailyStockAnalysisProperties {
    private boolean enabled = true;
    private String baseUrl = "http://localhost:8000";
    private String riskAnalysisPath = "/api/external/kline-risk-analysis";
    private String fallbackAnalysisPath = "/api/v1/analysis/analyze";
    private int connectTimeoutSeconds = 5;
    private int readTimeoutSeconds = 90;
    private boolean cacheEnabled = true;
}
