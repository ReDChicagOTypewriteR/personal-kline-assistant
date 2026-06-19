package com.alexjoker.quant.marketdata.controller;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.common.response.ApiResponse;
import com.alexjoker.quant.marketdata.dto.MarketKlineDTO;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.marketdata.mapper.DailyKlineMapper;
import com.alexjoker.quant.marketdata.provider.MultiSourceKlineDataProvider;
import com.alexjoker.quant.marketdata.service.KlineCacheService;
import com.alexjoker.quant.symbol.dto.SymbolDTO;
import com.alexjoker.quant.symbol.service.SymbolService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@RestController
@RequestMapping("/api/klines")
@RequiredArgsConstructor
public class MarketKlineController {
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private final MultiSourceKlineDataProvider multiSourceKlineDataProvider;
    private final SymbolService symbolService;
    private final DailyKlineMapper dailyKlineMapper;
    private final KlineCacheService klineCacheService;

    @GetMapping("/chart")
    public ApiResponse<List<MarketKlineDTO>> chartKlines(@RequestParam String symbolCode,
                                                         @RequestParam(required = false) String stockCode,
                                                         @RequestParam(required = false, defaultValue = "D") String period,
                                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                                                         @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
                                                         @RequestParam(required = false, defaultValue = "1") Integer adjustType) {
        SymbolDTO symbol = symbolService.getOrCreateCnSymbol(symbolCode);
        String code = normalizeStockCode(symbolCode);
        String sourceCode = normalizeStockCode(stockCode == null || stockCode.isBlank() ? code : stockCode);
        String normalizedPeriod = normalizePeriod(period);
        int normalizedAdjustType = adjustType == null ? 1 : adjustType;

        List<MarketKlineDTO> cachedRows = klineCacheService.getChart(code, normalizedPeriod, start, end, normalizedAdjustType);
        if (cachedRows != null && !cachedRows.isEmpty()) {
            return ApiResponse.ok(cachedRows);
        }

        if (isDailyPeriod(normalizedPeriod)) {
            List<MarketKlineDTO> localRows = localDailyRows(code, start, end);
            if (isLocalRangeUsable(localRows, start, end)) {
                klineCacheService.putChart(code, normalizedPeriod, start, end, normalizedAdjustType, localRows);
                return ApiResponse.ok(localRows);
            }
        }

        try {
            List<MarketKlineDTO> rows = isDailyPeriod(normalizedPeriod)
                    ? fetchDailyRowsAndPersist(code, sourceCode, symbol.getAssetType(), start, end, normalizedAdjustType)
                    : multiSourceKlineDataProvider.fetchChartKlines(
                    sourceCode, symbol.getAssetType(), normalizedPeriod, start, end, normalizedAdjustType
            );
            klineCacheService.putChart(code, normalizedPeriod, start, end, normalizedAdjustType, rows);
            return ApiResponse.ok(rows);
        } catch (BusinessException ex) {
            if (isDailyPeriod(normalizedPeriod)) {
                List<MarketKlineDTO> localRows = localDailyRows(code, start, end);
                if (!localRows.isEmpty()) {
                    klineCacheService.putChart(code, normalizedPeriod, start, end, normalizedAdjustType, localRows);
                    return ApiResponse.ok(localRows);
                }
            }
            throw ex;
        }
    }

    private List<MarketKlineDTO> fetchDailyRowsAndPersist(String symbolCode, String sourceCode, String assetType,
                                                          LocalDate start, LocalDate end, int adjustType) {
        List<DailyKlineEntity> rows = multiSourceKlineDataProvider.fetchDaily(symbolCode, sourceCode, assetType, start, end, adjustType);
        rows.forEach(dailyKlineMapper::upsert);
        return rows.stream().map(this::toMarketKline).toList();
    }

    private List<MarketKlineDTO> localDailyRows(String symbolCode, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<DailyKlineEntity> wrapper = new LambdaQueryWrapper<DailyKlineEntity>()
                .eq(DailyKlineEntity::getSymbolCode, symbolCode.trim().toUpperCase());
        if (start != null) {
            wrapper.ge(DailyKlineEntity::getTradeDate, start);
        }
        if (end != null) {
            wrapper.le(DailyKlineEntity::getTradeDate, end);
        }
        wrapper.orderByAsc(DailyKlineEntity::getTradeDate);
        return dailyKlineMapper.selectList(wrapper).stream().map(this::toMarketKline).toList();
    }

    private MarketKlineDTO toMarketKline(DailyKlineEntity entity) {
        MarketKlineDTO dto = new MarketKlineDTO();
        dto.setTimestamp(entity.getTradeDate().atStartOfDay(CHINA_ZONE).toInstant().toEpochMilli());
        dto.setOpen(entity.getOpenPrice());
        dto.setClose(entity.getClosePrice());
        dto.setHigh(entity.getHighPrice());
        dto.setLow(entity.getLowPrice());
        dto.setVolume(entity.getVolume());
        dto.setTurnover(entity.getAmount());
        return dto;
    }

    private boolean isDailyPeriod(String period) {
        return period == null || period.isBlank() || "D".equalsIgnoreCase(period.trim());
    }

    private boolean isLocalRangeUsable(List<MarketKlineDTO> rows, LocalDate start, LocalDate end) {
        if (rows == null || rows.isEmpty()) {
            return false;
        }
        if (start == null && end == null) {
            return true;
        }
        LocalDate first = toLocalDate(rows.getFirst());
        LocalDate last = toLocalDate(rows.getLast());
        LocalDate effectiveEnd = end == null || end.isAfter(LocalDate.now()) ? LocalDate.now() : end;
        if (start != null && first.isAfter(start.plusDays(7))) {
            return false;
        }
        if (last.isBefore(effectiveEnd.minusDays(10))) {
            return false;
        }
        if (start == null) {
            return true;
        }
        long calendarDays = Math.max(1, java.time.temporal.ChronoUnit.DAYS.between(start, effectiveEnd) + 1);
        long expectedLowerBound = Math.max(1, Math.round(calendarDays * 0.45));
        return rows.size() >= expectedLowerBound;
    }

    private LocalDate toLocalDate(MarketKlineDTO row) {
        return java.time.Instant.ofEpochMilli(row.getTimestamp()).atZone(CHINA_ZONE).toLocalDate();
    }

    private String normalizePeriod(String value) {
        return value == null || value.isBlank() ? "D" : value.trim().toUpperCase();
    }

    private String normalizeStockCode(String value) {
        String code = value.trim().toUpperCase();
        int dotIndex = code.indexOf('.');
        if (dotIndex > 0) {
            code = code.substring(0, dotIndex);
        }
        if (!code.matches("\\d{6}")) {
            throw new BusinessException("东方财富 A 股代码必须是 6 位数字，例如 000001");
        }
        return code;
    }
}
