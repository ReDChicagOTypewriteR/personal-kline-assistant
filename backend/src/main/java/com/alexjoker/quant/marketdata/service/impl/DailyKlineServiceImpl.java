package com.alexjoker.quant.marketdata.service.impl;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.marketdata.dto.DailyKlineDTO;
import com.alexjoker.quant.marketdata.dto.ImportKlineResultDTO;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.marketdata.mapper.DailyKlineMapper;
import com.alexjoker.quant.marketdata.provider.MultiSourceKlineDataProvider;
import com.alexjoker.quant.marketdata.service.DailyKlineService;
import com.alexjoker.quant.marketdata.service.KlineCacheService;
import com.alexjoker.quant.marketdata.service.MarketDataAutoRefreshService;
import com.alexjoker.quant.symbol.dto.SymbolDTO;
import com.alexjoker.quant.symbol.service.SymbolService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DailyKlineServiceImpl implements DailyKlineService {
    private final DailyKlineMapper dailyKlineMapper;
    private final SymbolService symbolService;
    private final MultiSourceKlineDataProvider multiSourceKlineDataProvider;
    private final MarketDataAutoRefreshService marketDataAutoRefreshService;
    private final KlineCacheService klineCacheService;

    @Override
    @Transactional
    public ImportKlineResultDTO importDaily(String symbolCode, MultipartFile file) {
        String code = normalize(symbolCode);
        if (!symbolService.exists(code)) {
            throw new BusinessException("标的不存在，无法导入 K 线: " + code);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException("CSV 文件不能为空");
        }

        int total = 0;
        int success = 0;
        int failed = 0;
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            boolean headerSkipped = false;
            while ((line = reader.readLine()) != null) {
                if (!headerSkipped) {
                    headerSkipped = true;
                    continue;
                }
                if (line.isBlank()) {
                    continue;
                }
                total++;
                try {
                    DailyKlineEntity entity = parseLine(code, line);
                    dailyKlineMapper.upsert(entity);
                    success++;
                } catch (Exception ex) {
                    failed++;
                }
            }
        } catch (Exception ex) {
            throw new BusinessException("读取 CSV 失败: " + ex.getMessage());
        }
        klineCacheService.evictSymbol(code);
        return new ImportKlineResultDTO(code, total, success, failed, "导入完成");
    }

    @Override
    @Transactional
    public ImportKlineResultDTO importFromEastMoney(String symbolCode, String stockCode, LocalDate start,
                                                   LocalDate end, Integer adjustType) {
        String code = normalize(symbolCode);
        if (!symbolService.exists(code)) {
            throw new BusinessException("标的不存在，无法导入 K 线: " + code);
        }
        SymbolDTO symbol = symbolService.getByCode(code);
        String sourceCode = normalizeStockCode(stockCode == null || stockCode.isBlank() ? code : stockCode);
        List<DailyKlineEntity> klines = multiSourceKlineDataProvider.fetchDaily(
                code, sourceCode, symbol.getAssetType(), start, end, adjustType == null ? 1 : adjustType
        );
        klines.forEach(this::validatePriceRow);
        klines.forEach(dailyKlineMapper::upsert);
        klineCacheService.evictSymbol(code);
        return new ImportKlineResultDTO(code, klines.size(), klines.size(), 0,
                "多数据源日 K 导入完成，source=" + sourceCode);
    }

    @Override
    public List<DailyKlineDTO> listDaily(String symbolCode, LocalDate start, LocalDate end) {
        String code = normalize(symbolCode);
        marketDataAutoRefreshService.ensureDailyKlines(code, start, end);
        LambdaQueryWrapper<DailyKlineEntity> wrapper = new LambdaQueryWrapper<DailyKlineEntity>()
                .eq(DailyKlineEntity::getSymbolCode, code);
        if (start != null) {
            wrapper.ge(DailyKlineEntity::getTradeDate, start);
        }
        if (end != null) {
            wrapper.le(DailyKlineEntity::getTradeDate, end);
        }
        wrapper.orderByAsc(DailyKlineEntity::getTradeDate);
        return dailyKlineMapper.selectList(wrapper).stream().map(this::toDto).toList();
    }

    @Override
    public DailyKlineDTO getLatest(String symbolCode) {
        DailyKlineEntity entity = latestEntity(normalize(symbolCode));
        return entity == null ? null : toDto(entity);
    }

    public DailyKlineEntity latestEntity(String symbolCode) {
        return dailyKlineMapper.selectOne(new LambdaQueryWrapper<DailyKlineEntity>()
                .eq(DailyKlineEntity::getSymbolCode, normalize(symbolCode))
                .orderByDesc(DailyKlineEntity::getTradeDate)
                .last("LIMIT 1"));
    }

    private DailyKlineEntity parseLine(String code, String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 5) {
            throw new IllegalArgumentException("列数不足");
        }
        DailyKlineEntity entity = new DailyKlineEntity();
        entity.setSymbolCode(code);
        entity.setTradeDate(LocalDate.parse(parts[0].trim()));
        entity.setOpenPrice(requiredDecimal(parts[1]));
        entity.setHighPrice(requiredDecimal(parts[2]));
        entity.setLowPrice(requiredDecimal(parts[3]));
        entity.setClosePrice(requiredDecimal(parts[4]));
        entity.setVolume(optionalDecimal(parts.length > 5 ? parts[5] : null));
        entity.setAmount(optionalDecimal(parts.length > 6 ? parts[6] : null));
        validatePriceRow(entity);
        return entity;
    }

    private void validatePriceRow(DailyKlineEntity entity) {
        BigDecimal open = entity.getOpenPrice();
        BigDecimal high = entity.getHighPrice();
        BigDecimal low = entity.getLowPrice();
        BigDecimal close = entity.getClosePrice();
        if (open.compareTo(BigDecimal.ZERO) <= 0
                || high.compareTo(BigDecimal.ZERO) <= 0
                || low.compareTo(BigDecimal.ZERO) <= 0
                || close.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("OHLC 价格必须大于 0");
        }
        if (high.compareTo(low) < 0
                || high.compareTo(open) < 0
                || high.compareTo(close) < 0
                || low.compareTo(open) > 0
                || low.compareTo(close) > 0) {
            throw new IllegalArgumentException("OHLC 价格关系异常");
        }
    }

    private BigDecimal requiredDecimal(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("必填数值为空");
        }
        return new BigDecimal(value.trim());
    }

    private BigDecimal optionalDecimal(String value) {
        return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
    }

    private DailyKlineDTO toDto(DailyKlineEntity entity) {
        DailyKlineDTO dto = new DailyKlineDTO();
        dto.setSymbolCode(entity.getSymbolCode());
        dto.setTradeDate(entity.getTradeDate());
        dto.setOpen(entity.getOpenPrice());
        dto.setHigh(entity.getHighPrice());
        dto.setLow(entity.getLowPrice());
        dto.setClose(entity.getClosePrice());
        dto.setVolume(entity.getVolume());
        dto.setAmount(entity.getAmount());
        return dto;
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
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
