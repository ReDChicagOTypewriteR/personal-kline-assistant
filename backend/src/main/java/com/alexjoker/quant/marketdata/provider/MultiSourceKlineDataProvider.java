package com.alexjoker.quant.marketdata.provider;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.marketdata.dto.MarketKlineDTO;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MultiSourceKlineDataProvider {
    private final EastMoneyKlineDataProvider eastMoneyKlineDataProvider;
    private final AdataKlineDataProvider adataKlineDataProvider;
    private final MarketDataSourceHealthRegistry healthRegistry;

    public List<DailyKlineEntity> fetchDaily(String symbolCode, String stockCode, String assetType,
                                             LocalDate start, LocalDate end, int adjustType) {
        List<String> sources = dailySources(assetType);
        List<String> errors = new ArrayList<>();
        for (String source : sources) {
            try {
                List<DailyKlineEntity> rows = fetchDailyBySource(source, symbolCode, stockCode, start, end, adjustType);
                if (!rows.isEmpty()) {
                    healthRegistry.recordSuccess(source);
                    return rows;
                }
                String message = "行情数据源未返回日 K 数据";
                healthRegistry.recordFailure(source, message);
                errors.add(source + ": " + message);
            } catch (BusinessException ex) {
                healthRegistry.recordFailure(source, ex.getMessage());
                errors.add(source + ": " + ex.getMessage());
            }
        }
        throw new BusinessException("所有行情数据源均失败，请检查代码是否正确、资产类型是否匹配，或稍后重试。详情: " + String.join(" | ", errors));
    }

    public List<MarketKlineDTO> fetchChartKlines(String stockCode, String assetType, String period,
                                                 LocalDate start, LocalDate end, int adjustType) {
        List<String> sources = chartSources(assetType, period);
        List<String> errors = new ArrayList<>();
        for (String source : sources) {
            try {
                List<MarketKlineDTO> rows = fetchChartBySource(source, stockCode, period, start, end, adjustType);
                if (!rows.isEmpty()) {
                    healthRegistry.recordSuccess(source);
                    return rows;
                }
                String message = "行情数据源未返回图表 K 线数据";
                healthRegistry.recordFailure(source, message);
                errors.add(source + ": " + message);
            } catch (BusinessException ex) {
                healthRegistry.recordFailure(source, ex.getMessage());
                errors.add(source + ": " + ex.getMessage());
            }
        }
        throw new BusinessException("所有图表行情数据源均失败，请检查代码是否正确、资产类型是否匹配，或稍后重试。详情: " + String.join(" | ", errors));
    }

    private List<DailyKlineEntity> fetchDailyBySource(String source, String symbolCode, String stockCode,
                                                      LocalDate start, LocalDate end, int adjustType) {
        return switch (source) {
            case "EASTMONEY" -> eastMoneyKlineDataProvider.fetchDaily(symbolCode, stockCode, start, end, adjustType);
            case "ADATA_ETF_THS" -> adataKlineDataProvider.fetchDaily("ETF_THS", symbolCode, stockCode, start, end, adjustType);
            case "ADATA_STOCK_EAST" -> adataKlineDataProvider.fetchDaily("STOCK_EAST", symbolCode, stockCode, start, end, adjustType);
            case "ADATA_STOCK_BAIDU" -> adataKlineDataProvider.fetchDaily("STOCK_BAIDU", symbolCode, stockCode, start, end, adjustType);
            default -> throw new BusinessException("未知行情数据源: " + source);
        };
    }

    private List<MarketKlineDTO> fetchChartBySource(String source, String stockCode, String period,
                                                    LocalDate start, LocalDate end, int adjustType) {
        return switch (source) {
            case "EASTMONEY" -> eastMoneyKlineDataProvider.fetchChartKlines(stockCode, period, start, end, adjustType);
            case "ADATA_ETF_THS" -> adataKlineDataProvider.fetchChartKlines("ETF_THS", stockCode, period, start, end, adjustType);
            case "ADATA_STOCK_EAST" -> adataKlineDataProvider.fetchChartKlines("STOCK_EAST", stockCode, period, start, end, adjustType);
            case "ADATA_STOCK_BAIDU" -> adataKlineDataProvider.fetchChartKlines("STOCK_BAIDU", stockCode, period, start, end, adjustType);
            default -> throw new BusinessException("未知图表行情数据源: " + source);
        };
    }

    private List<String> dailySources(String assetType) {
        if (isEtf(assetType)) {
            return List.of("EASTMONEY", "ADATA_ETF_THS", "ADATA_STOCK_EAST", "ADATA_STOCK_BAIDU");
        }
        return List.of("EASTMONEY", "ADATA_STOCK_EAST", "ADATA_STOCK_BAIDU");
    }

    private List<String> chartSources(String assetType, String period) {
        if (!isDailyLike(period)) {
            return List.of("EASTMONEY");
        }
        return dailySources(assetType);
    }

    private boolean isEtf(String assetType) {
        return assetType != null && assetType.equalsIgnoreCase("ETF");
    }

    private boolean isDailyLike(String period) {
        String value = period == null || period.isBlank() ? "D" : period.trim().toUpperCase();
        return List.of("D", "W", "M").contains(value);
    }
}
