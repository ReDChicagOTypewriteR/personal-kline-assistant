package com.alexjoker.quant.marketdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "market-data.kline-cache")
public class KlineCacheProperties {
    private boolean enabled = true;
    private String keyPrefix = "personal-kline:kline:chart";
    private long historicalTtlHours = 168;
    private long liveTtlMinutes = 15;
}
