package com.alexjoker.quant.system.service;

import com.alexjoker.quant.marketdata.engine.MarketDataEngineManager;
import com.alexjoker.quant.marketdata.provider.EastMoneyKlineDataProvider;
import com.alexjoker.quant.marketdata.provider.MarketDataSourceHealthRegistry;
import com.alexjoker.quant.marketdata.provider.MarketDataSourceHealthRegistry.ProbeStatus;
import com.alexjoker.quant.marketdata.service.KlineCacheService;
import com.alexjoker.quant.system.dto.CacheRefreshResultDTO;
import com.alexjoker.quant.system.dto.MarketDataSourceOverviewDTO;
import com.alexjoker.quant.system.dto.MarketDataSourceStatusDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class SystemOpsService {
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final EastMoneyKlineDataProvider eastMoneyKlineDataProvider;
    private final MarketDataEngineManager engineManager;
    private final MarketDataSourceHealthRegistry healthRegistry;
    private final KlineCacheService klineCacheService;

    public MarketDataSourceOverviewDTO marketDataSources() {
        Map<String, ProbeStatus> probes = probeSources();
        List<MarketDataSourceStatusDTO> sources = healthRegistry.buildStatuses(probes);
        MarketDataSourceOverviewDTO dto = new MarketDataSourceOverviewDTO();
        dto.setSources(sources);
        dto.setTotalCount(sources.size());
        dto.setUpCount((int) sources.stream().filter(item -> "UP".equals(item.getStatus())).count());
        dto.setDownCount((int) sources.stream().filter(item -> "DOWN".equals(item.getStatus())).count());
        dto.setUnknownCount((int) sources.stream().filter(item -> "UNKNOWN".equals(item.getStatus())).count());
        dto.setRefreshedAt(now());
        return dto;
    }

    public CacheRefreshResultDTO refreshCache() {
        MarketDataSourceOverviewDTO overview = marketDataSources();
        int deleted = klineCacheService.evictAll();
        CacheRefreshResultDTO dto = new CacheRefreshResultDTO();
        dto.setStatus("SUCCESS");
        dto.setRefreshedAt(now());
        dto.setMessage("缓存刷新完成：已刷新数据源状态，并清理 K 线 Redis 图表缓存。");
        dto.setActions(List.of(
                "刷新行情数据源健康探测",
                "清理 K 线图表 Redis 缓存：" + deleted + " 个 key",
                "未清理 MySQL 数据，不影响 K 线、指标、信号和回测记录"
        ));
        if (overview.getDownCount() != null && overview.getDownCount() > 0) {
            List<String> actions = new ArrayList<>(dto.getActions());
            actions.add("检测到不可用数据源：" + overview.getDownCount() + " 个，请查看数据源状态表。");
            dto.setActions(actions);
        }
        return dto;
    }

    public CacheRefreshResultDTO refreshKlineCache(String symbolCode) {
        boolean all = symbolCode == null || symbolCode.isBlank();
        int deleted = all ? klineCacheService.evictAll() : klineCacheService.evictSymbol(symbolCode);
        CacheRefreshResultDTO dto = new CacheRefreshResultDTO();
        dto.setStatus("SUCCESS");
        dto.setRefreshedAt(now());
        dto.setMessage(all ? "已刷新全部 K 线图表缓存。" : "已刷新 " + symbolCode.trim().toUpperCase() + " 的 K 线图表缓存。");
        dto.setActions(List.of(
                "清理 Redis 图表缓存：" + deleted + " 个 key",
                "未删除 MySQL daily_kline 历史数据",
                "下一次进入 K 线详情时会优先从 MySQL 重建缓存；MySQL 数据不足时才请求外部行情源"
        ));
        return dto;
    }

    private Map<String, ProbeStatus> probeSources() {
        Map<String, ProbeStatus> probes = new LinkedHashMap<>();
        probes.put("EASTMONEY", eastMoneyKlineDataProvider.healthCheck()
                ? ProbeStatus.up("HTTP 接口可访问")
                : ProbeStatus.down("HTTP 接口探测失败"));

        ProbeStatus adataStatus = engineManager.isAvailable()
                ? ProbeStatus.up((engineManager.isExternalEngine() ? "外部" : "内置") + " adata 引擎可用：" + engineManager.baseUrl())
                : (engineManager.isEnabled()
                ? ProbeStatus.down("adata 引擎不可用：" + engineManager.baseUrl())
                : ProbeStatus.disabled("adata 引擎已禁用"));
        probes.put("ADATA_ETF_THS", adataStatus);
        probes.put("ADATA_STOCK_EAST", adataStatus);
        probes.put("ADATA_STOCK_BAIDU", adataStatus);
        return probes;
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
