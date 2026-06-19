package com.alexjoker.quant.marketdata.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "market-data.engine")
public class MarketDataEngineProperties {
    private boolean enabled = true;
    private String host = "127.0.0.1";
    private int port = 19090;
    private String pythonCommand = "python3";
    private String adataHome = "./adata-main";
    private int startupTimeoutSeconds = 20;
    private int requestTimeoutSeconds = 60;
}
