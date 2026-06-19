package com.alexjoker.quant.marketdata.service;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.marketdata.mapper.DailyKlineMapper;
import com.alexjoker.quant.marketdata.provider.MultiSourceKlineDataProvider;
import com.alexjoker.quant.symbol.dto.SymbolDTO;
import com.alexjoker.quant.symbol.service.SymbolService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class MarketDataAutoRefreshService {
    private static final int DEFAULT_LOOKBACK_YEARS = 2;
    private static final int RECENT_THRESHOLD_DAYS = 5;

    private final DailyKlineMapper dailyKlineMapper;
    private final SymbolService symbolService;
    private final MultiSourceKlineDataProvider multiSourceKlineDataProvider;

    @Transactional
    public void ensureDailyKlines(String symbolCode, LocalDate requestedStart, LocalDate requestedEnd) {
        SymbolDTO symbol = symbolService.getByCode(symbolCode);
        Optional<String> sourceCode = resolveEastMoneyStockCode(symbol);
        if (sourceCode.isEmpty()) {
            return;
        }

        DailyKlineEntity latest = latestEntity(symbol.getSymbolCode());
        LocalDate today = LocalDate.now();
        LocalDate end = requestedEnd == null || requestedEnd.isAfter(today) ? today : requestedEnd;
        LocalDate start;

        if (latest == null) {
            start = requestedStart == null ? today.minusYears(DEFAULT_LOOKBACK_YEARS) : requestedStart;
        } else if (latest.getTradeDate().isBefore(today.minusDays(RECENT_THRESHOLD_DAYS))) {
            start = latest.getTradeDate().minusDays(7);
        } else {
            return;
        }

        if (start.isAfter(end)) {
            return;
        }
        try {
            List<DailyKlineEntity> klines = multiSourceKlineDataProvider.fetchDaily(
                    symbol.getSymbolCode(), sourceCode.get(), symbol.getAssetType(), start, end, 1
            );
            klines.forEach(dailyKlineMapper::upsert);
        } catch (BusinessException ex) {
            if (latest != null) {
                return;
            }
            throw ex;
        }
    }

    private Optional<String> resolveEastMoneyStockCode(SymbolDTO symbol) {
        if (symbol.getMarket() == null || !"CN".equalsIgnoreCase(symbol.getMarket())) {
            return Optional.empty();
        }
        String code = symbol.getSymbolCode().trim().toUpperCase();
        int dotIndex = code.indexOf('.');
        if (dotIndex > 0) {
            code = code.substring(0, dotIndex);
        }
        return code.matches("\\d{6}") ? Optional.of(code) : Optional.empty();
    }

    private DailyKlineEntity latestEntity(String symbolCode) {
        return dailyKlineMapper.selectOne(new LambdaQueryWrapper<DailyKlineEntity>()
                .eq(DailyKlineEntity::getSymbolCode, symbolCode)
                .orderByDesc(DailyKlineEntity::getTradeDate)
                .last("LIMIT 1"));
    }
}
