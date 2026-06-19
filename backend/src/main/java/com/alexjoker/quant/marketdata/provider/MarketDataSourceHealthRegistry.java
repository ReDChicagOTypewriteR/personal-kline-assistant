package com.alexjoker.quant.marketdata.provider;

import com.alexjoker.quant.system.dto.MarketDataSourceStatusDTO;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class MarketDataSourceHealthRegistry {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final Map<String, RuntimeStatus> runtimeStatuses = new ConcurrentHashMap<>();

    public void recordSuccess(String sourceCode) {
        RuntimeStatus status = runtimeStatuses.computeIfAbsent(sourceCode, key -> new RuntimeStatus());
        status.successCount++;
        status.lastCheckedAt = now();
        status.lastSuccessAt = status.lastCheckedAt;
        status.lastError = null;
    }

    public void recordFailure(String sourceCode, String message) {
        RuntimeStatus status = runtimeStatuses.computeIfAbsent(sourceCode, key -> new RuntimeStatus());
        status.failureCount++;
        status.lastCheckedAt = now();
        status.lastErrorAt = status.lastCheckedAt;
        status.lastError = message;
    }

    public List<MarketDataSourceStatusDTO> buildStatuses(Map<String, ProbeStatus> probes) {
        List<MarketDataSourceStatusDTO> results = new ArrayList<>();
        for (SourceDefinition definition : definitions().values()) {
            RuntimeStatus runtime = runtimeStatuses.get(definition.sourceCode());
            ProbeStatus probe = probes.getOrDefault(definition.sourceCode(), ProbeStatus.unknown("未探测"));
            MarketDataSourceStatusDTO dto = new MarketDataSourceStatusDTO();
            dto.setSourceCode(definition.sourceCode());
            dto.setDisplayName(definition.displayName());
            dto.setProviderType(definition.providerType());
            dto.setRole(definition.role());
            dto.setPriority(definition.priority());
            dto.setEnabled(probe.enabled());
            dto.setStatus(probe.status());
            dto.setHealthMessage(probe.message());
            dto.setSuccessCount(runtime == null ? 0 : runtime.successCount);
            dto.setFailureCount(runtime == null ? 0 : runtime.failureCount);
            dto.setLastCheckedAt(runtime == null ? null : runtime.lastCheckedAt);
            dto.setLastSuccessAt(runtime == null ? null : runtime.lastSuccessAt);
            dto.setLastErrorAt(runtime == null ? null : runtime.lastErrorAt);
            dto.setLastError(runtime == null ? null : runtime.lastError);
            results.add(dto);
        }
        return results;
    }

    public static Map<String, SourceDefinition> definitions() {
        Map<String, SourceDefinition> map = new LinkedHashMap<>();
        map.put("EASTMONEY", new SourceDefinition("EASTMONEY", "东方财富", "HTTP", "主行情源 / 图表行情", 1));
        map.put("ADATA_ETF_THS", new SourceDefinition("ADATA_ETF_THS", "adata ETF 同花顺", "ADATA_ENGINE", "ETF 日 K 备用源", 2));
        map.put("ADATA_STOCK_EAST", new SourceDefinition("ADATA_STOCK_EAST", "adata 股票东方财富", "ADATA_ENGINE", "股票 / ETF 日 K 备用源", 3));
        map.put("ADATA_STOCK_BAIDU", new SourceDefinition("ADATA_STOCK_BAIDU", "adata 百度股市通", "ADATA_ENGINE", "股票 / ETF 日 K 备用源", 4));
        return map;
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }

    public record SourceDefinition(String sourceCode, String displayName, String providerType, String role, int priority) {
    }

    public record ProbeStatus(boolean enabled, String status, String message) {
        public static ProbeStatus up(String message) {
            return new ProbeStatus(true, "UP", message);
        }

        public static ProbeStatus down(String message) {
            return new ProbeStatus(true, "DOWN", message);
        }

        public static ProbeStatus disabled(String message) {
            return new ProbeStatus(false, "DOWN", message);
        }

        public static ProbeStatus unknown(String message) {
            return new ProbeStatus(true, "UNKNOWN", message);
        }
    }

    private static class RuntimeStatus {
        private int successCount;
        private int failureCount;
        private String lastCheckedAt;
        private String lastSuccessAt;
        private String lastErrorAt;
        private String lastError;
    }
}
