package com.alexjoker.quant.indicator.service.impl;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.common.util.TextUtil;
import com.alexjoker.quant.indicator.calculator.IndicatorCalculator;
import com.alexjoker.quant.indicator.converter.IndicatorSnapshotConverter;
import com.alexjoker.quant.indicator.dto.IndicatorSnapshotDTO;
import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.indicator.mapper.IndicatorSnapshotMapper;
import com.alexjoker.quant.indicator.service.IndicatorService;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.marketdata.mapper.DailyKlineMapper;
import com.alexjoker.quant.symbol.dto.SymbolDTO;
import com.alexjoker.quant.symbol.service.SymbolService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class IndicatorServiceImpl implements IndicatorService {
    private final IndicatorSnapshotMapper indicatorSnapshotMapper;
    private final DailyKlineMapper dailyKlineMapper;
    private final SymbolService symbolService;
    private final IndicatorCalculator indicatorCalculator;

    @Override
    @Transactional
    public int calculateForSymbol(String symbolCode) {
        String code = normalize(symbolCode);
        if (!symbolService.exists(code)) {
            throw new BusinessException("标的不存在，无法计算指标: " + code);
        }
        List<DailyKlineEntity> klines = dailyKlineMapper.selectList(new LambdaQueryWrapper<DailyKlineEntity>()
                .eq(DailyKlineEntity::getSymbolCode, code)
                .orderByAsc(DailyKlineEntity::getTradeDate));
        if (klines.isEmpty()) {
            throw new BusinessException("没有可计算的 K 线数据: " + code);
        }
        List<IndicatorSnapshotEntity> snapshots = indicatorCalculator.calculate(code, klines);
        snapshots.forEach(indicatorSnapshotMapper::upsert);
        return snapshots.size();
    }

    @Override
    @Transactional
    public int calculateForAllEnabledSymbols() {
        int total = 0;
        for (SymbolDTO symbol : symbolService.listEnabled()) {
            List<DailyKlineEntity> klines = dailyKlineMapper.selectList(new LambdaQueryWrapper<DailyKlineEntity>()
                    .eq(DailyKlineEntity::getSymbolCode, symbol.getSymbolCode())
                    .orderByAsc(DailyKlineEntity::getTradeDate));
            if (klines.isEmpty()) {
                continue;
            }
            List<IndicatorSnapshotEntity> snapshots = indicatorCalculator.calculate(symbol.getSymbolCode(), klines);
            snapshots.forEach(indicatorSnapshotMapper::upsert);
            total += snapshots.size();
        }
        return total;
    }

    @Override
    public List<IndicatorSnapshotDTO> listIndicators(String symbolCode, LocalDate start, LocalDate end) {
        String code = normalize(symbolCode);
        LambdaQueryWrapper<IndicatorSnapshotEntity> wrapper = new LambdaQueryWrapper<IndicatorSnapshotEntity>()
                .eq(IndicatorSnapshotEntity::getSymbolCode, code);
        if (start != null) {
            wrapper.ge(IndicatorSnapshotEntity::getTradeDate, start);
        }
        if (end != null) {
            wrapper.le(IndicatorSnapshotEntity::getTradeDate, end);
        }
        wrapper.orderByAsc(IndicatorSnapshotEntity::getTradeDate);
        return indicatorSnapshotMapper.selectList(wrapper).stream().map(IndicatorSnapshotConverter::toDto).toList();
    }

    @Override
    public IndicatorSnapshotDTO getLatestIndicator(String symbolCode) {
        IndicatorSnapshotEntity entity = latestEntity(normalize(symbolCode));
        return IndicatorSnapshotConverter.toDto(entity);
    }

    public IndicatorSnapshotEntity latestEntity(String symbolCode) {
        return indicatorSnapshotMapper.selectOne(new LambdaQueryWrapper<IndicatorSnapshotEntity>()
                .eq(IndicatorSnapshotEntity::getSymbolCode, normalize(symbolCode))
                .orderByDesc(IndicatorSnapshotEntity::getTradeDate)
                .last("LIMIT 1"));
    }

    private String normalize(String value) {
        return TextUtil.normalizeCode(value);
    }
}
