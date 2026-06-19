package com.alexjoker.quant.signal.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "signal.rule")
public class TechnicalSignalRuleProperties {
    private int stockBuyScoreThreshold = 75;
    private int etfBuyScoreThreshold = 70;
    private int etfRsiMin = 42;
    private int etfRsiMax = 72;
    private boolean etfAllowPartialMaConfirmation = true;
}
