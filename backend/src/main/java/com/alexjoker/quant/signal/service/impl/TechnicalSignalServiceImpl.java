package com.alexjoker.quant.signal.service.impl;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.common.util.NumberUtil;
import com.alexjoker.quant.common.util.TextUtil;
import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.indicator.mapper.IndicatorSnapshotMapper;
import com.alexjoker.quant.indicator.service.impl.IndicatorServiceImpl;
import com.alexjoker.quant.journal.service.TradeJournalService;
import com.alexjoker.quant.llm.entity.FinalTradeDecisionEntity;
import com.alexjoker.quant.llm.mapper.FinalTradeDecisionMapper;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.marketdata.mapper.DailyKlineMapper;
import com.alexjoker.quant.marketdata.service.impl.DailyKlineServiceImpl;
import com.alexjoker.quant.signal.converter.TechnicalSignalConverter;
import com.alexjoker.quant.signal.dto.TechnicalSignalDTO;
import com.alexjoker.quant.signal.entity.TechnicalSignalEntity;
import com.alexjoker.quant.signal.mapper.TechnicalSignalMapper;
import com.alexjoker.quant.signal.rule.TechnicalScoreCalculator;
import com.alexjoker.quant.signal.rule.TechnicalScoreResult;
import com.alexjoker.quant.signal.service.TechnicalSignalService;
import com.alexjoker.quant.strategy.rule.EtfTrendStrategyRule;
import com.alexjoker.quant.symbol.dto.SymbolDTO;
import com.alexjoker.quant.symbol.service.SymbolService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class TechnicalSignalServiceImpl implements TechnicalSignalService {
    private final TechnicalSignalMapper technicalSignalMapper;
    private final SymbolService symbolService;
    private final DailyKlineServiceImpl dailyKlineService;
    private final IndicatorServiceImpl indicatorService;
    private final DailyKlineMapper dailyKlineMapper;
    private final IndicatorSnapshotMapper indicatorSnapshotMapper;
    private final TechnicalScoreCalculator technicalScoreCalculator;
    private final FinalTradeDecisionMapper finalTradeDecisionMapper;
    private final EtfTrendStrategyRule etfTrendStrategyRule;
    private final TradeJournalService tradeJournalService;

    @Override
    @Transactional
    public List<TechnicalSignalDTO> generateSignalForSymbol(String symbolCode) {
        String code = normalize(symbolCode);
        if (!symbolService.exists(code)) {
            throw new BusinessException("标的不存在，无法生成信号: " + code);
        }
        DailyKlineEntity latestKline = dailyKlineService.latestEntity(code);
        IndicatorSnapshotEntity latestIndicator = indicatorService.latestEntity(code);
        if (latestKline == null) {
            throw new BusinessException("没有可用 K 线数据: " + code);
        }
        if (latestIndicator == null || !latestKline.getTradeDate().equals(latestIndicator.getTradeDate())) {
            throw new BusinessException("请先计算最新指标: " + code);
        }

        TechnicalScoreResult score = technicalScoreCalculator.calculate(latestKline, latestIndicator);
        SymbolDTO symbol = symbolService.getByCode(code);
        List<TechnicalSignalEntity> generated = new ArrayList<>();
        generated.add(buildMainSignal(latestKline, latestIndicator, score, symbol));
        if (isSellWarning(latestKline, latestIndicator, symbol)) {
            generated.add(buildSignal(latestKline, latestIndicator, score, "SELL_WARNING", "WEAK",
                    trendState(latestKline, latestIndicator, symbol), false));
        }
        for (TechnicalSignalEntity signal : generated) {
            technicalSignalMapper.upsert(signal);
            tradeJournalService.recordSignal(signal, latestKline, latestIndicator);
        }
        return generated.stream().map(entity -> TechnicalSignalConverter.toDto(entity, latestKline, latestIndicator)).toList();
    }

    @Override
    @Transactional
    public int generateSignalsForAllEnabledSymbols() {
        int count = 0;
        for (SymbolDTO symbol : symbolService.listEnabled()) {
            try {
                count += generateSignalForSymbol(symbol.getSymbolCode()).size();
            } catch (BusinessException ignored) {
                // 缺少 K 线或指标的标的不阻断批量生成。
            }
        }
        return count;
    }

    @Override
    public List<TechnicalSignalDTO> listLatestSignals() {
        List<TechnicalSignalEntity> all = technicalSignalMapper.selectList(new LambdaQueryWrapper<TechnicalSignalEntity>()
                .orderByDesc(TechnicalSignalEntity::getTradeDate));
        Map<String, List<TechnicalSignalEntity>> bySymbol = new LinkedHashMap<>();
        for (TechnicalSignalEntity signal : all) {
            bySymbol.computeIfAbsent(signal.getSymbolCode(), key -> new ArrayList<>()).add(signal);
        }
        List<TechnicalSignalDTO> latest = new ArrayList<>();
        for (List<TechnicalSignalEntity> signals : bySymbol.values()) {
            var latestDate = signals.stream().map(TechnicalSignalEntity::getTradeDate).max(Comparator.naturalOrder()).orElse(null);
            TechnicalSignalEntity main = signals.stream()
                    .filter(signal -> signal.getTradeDate().equals(latestDate))
                    .min(Comparator.comparingInt(signal -> priority(signal.getSignalType())))
                    .orElse(signals.getFirst());
            latest.add(toDto(main));
        }
        latest.sort(Comparator.comparing(TechnicalSignalDTO::getTradeDate, Comparator.nullsLast(Comparator.reverseOrder())));
        return latest;
    }

    @Override
    public List<TechnicalSignalDTO> listSignalHistory(String symbolCode) {
        return technicalSignalMapper.selectList(new LambdaQueryWrapper<TechnicalSignalEntity>()
                        .eq(TechnicalSignalEntity::getSymbolCode, normalize(symbolCode))
                        .orderByDesc(TechnicalSignalEntity::getTradeDate)
                        .orderByAsc(TechnicalSignalEntity::getSignalType))
                .stream().map(this::toDto).toList();
    }

    @Override
    @Transactional
    public int deleteSignals(String symbolCode, java.time.LocalDate tradeDate) {
        String code = normalize(symbolCode);
        int deleted = technicalSignalMapper.delete(new LambdaQueryWrapper<TechnicalSignalEntity>()
                .eq(TechnicalSignalEntity::getSymbolCode, code)
                .eq(TechnicalSignalEntity::getTradeDate, tradeDate));
        finalTradeDecisionMapper.delete(new LambdaQueryWrapper<FinalTradeDecisionEntity>()
                .eq(FinalTradeDecisionEntity::getSymbolCode, code)
                .eq(FinalTradeDecisionEntity::getDecisionDate, tradeDate));
        tradeJournalService.deleteBySignal(code, tradeDate);
        return deleted;
    }

    private TechnicalSignalEntity buildMainSignal(DailyKlineEntity kline, IndicatorSnapshotEntity indicator,
                                                  TechnicalScoreResult score, SymbolDTO symbol) {
        if (isAvoid(kline, indicator, score.getScore(), symbol)) {
            return buildSignal(kline, indicator, score, "AVOID", "WEAK", avoidTrendState(kline, indicator, symbol), false);
        }
        if (isBuyCandidate(kline, indicator, score.getScore(), symbol)) {
            String level = score.getScore() >= 85 ? "STRONG" : "MEDIUM";
            return buildSignal(kline, indicator, score, "BUY_CANDIDATE", level, "UP", true);
        }
        if (score.getScore() >= 60) {
            return buildSignal(kline, indicator, score, "WATCH", "MEDIUM", trendState(kline, indicator, symbol), true);
        }
        return buildSignal(kline, indicator, score, "NEUTRAL", "WEAK", "SIDEWAYS", false);
    }

    private TechnicalSignalEntity buildSignal(DailyKlineEntity kline, IndicatorSnapshotEntity indicator, TechnicalScoreResult score,
                                              String type, String level, String trendState, boolean withReference) {
        TechnicalSignalEntity entity = new TechnicalSignalEntity();
        entity.setSymbolCode(kline.getSymbolCode());
        entity.setTradeDate(kline.getTradeDate());
        entity.setSignalType(type);
        entity.setSignalLevel(level);
        entity.setTrendState(trendState);
        entity.setTechnicalScore(score.getScore());
        if (withReference) {
            BigDecimal close = kline.getClosePrice();
            BigDecimal atr = indicator.getAtr14();
            entity.setEntryReference(NumberUtil.scale(close));
            if (atr != null) {
                entity.setStopLossReference(NumberUtil.scale(close.subtract(atr.multiply(BigDecimal.valueOf(1.5)))));
                entity.setTakeProfitReference(NumberUtil.scale(close.add(atr.multiply(BigDecimal.valueOf(2.0)))));
            } else {
                entity.setStopLossReference(NumberUtil.scale(close.multiply(BigDecimal.valueOf(0.98))));
                entity.setTakeProfitReference(NumberUtil.scale(close.multiply(BigDecimal.valueOf(1.04))));
            }
        }
        entity.setReason(NumberUtil.joinNullable("\n", score.getReasons()));
        entity.setRiskNote(NumberUtil.joinNullable("\n", score.getRiskNotes()));
        return entity;
    }

    private boolean isBuyCandidate(DailyKlineEntity kline, IndicatorSnapshotEntity indicator, int score, SymbolDTO symbol) {
        if (etfTrendStrategyRule.supports(symbol)) {
            return etfTrendStrategyRule.isBuyCandidate(kline, indicator, score);
        }
        return score >= 75
                && gt(kline.getClosePrice(), indicator.getMa20())
                && gt(indicator.getMa5(), indicator.getMa10())
                && gt(indicator.getMa10(), indicator.getMa20())
                && between(indicator.getRsi14(), 45, 70);
    }

    private boolean isAvoid(DailyKlineEntity kline, IndicatorSnapshotEntity indicator, int score, SymbolDTO symbol) {
        if (etfTrendStrategyRule.supports(symbol)) {
            return etfTrendStrategyRule.isAvoid(kline, indicator, score);
        }
        return score < 40
                || lt(kline.getClosePrice(), indicator.getMa20())
                || (indicator.getRsi14() != null && indicator.getRsi14().compareTo(BigDecimal.valueOf(75)) > 0)
                || (indicator.getDistanceToMa20() != null && indicator.getDistanceToMa20().compareTo(BigDecimal.valueOf(12)) > 0);
    }

    private boolean isSellWarning(DailyKlineEntity kline, IndicatorSnapshotEntity indicator, SymbolDTO symbol) {
        if (etfTrendStrategyRule.supports(symbol)) {
            return etfTrendStrategyRule.isSellWarning(kline, indicator);
        }
        return lt(kline.getClosePrice(), indicator.getMa10())
                || lt(kline.getClosePrice(), indicator.getMa20())
                || (indicator.getRsi14() != null && indicator.getRsi14().compareTo(BigDecimal.valueOf(40)) < 0);
    }

    private String trendState(DailyKlineEntity kline, IndicatorSnapshotEntity indicator, SymbolDTO symbol) {
        if (etfTrendStrategyRule.supports(symbol)) {
            return etfTrendStrategyRule.trendState(kline, indicator);
        }
        if (gt(kline.getClosePrice(), indicator.getMa20()) && gt(indicator.getMa10(), indicator.getMa20())) {
            return "UP";
        }
        if (lt(kline.getClosePrice(), indicator.getMa20())) {
            return "DOWN";
        }
        return "SIDEWAYS";
    }

    private String avoidTrendState(DailyKlineEntity kline, IndicatorSnapshotEntity indicator, SymbolDTO symbol) {
        if (etfTrendStrategyRule.supports(symbol)) {
            return etfTrendStrategyRule.avoidTrendState(kline, indicator);
        }
        if (lt(kline.getClosePrice(), indicator.getMa20())) {
            return "DOWN";
        }
        if ((indicator.getRsi14() != null && indicator.getRsi14().compareTo(BigDecimal.valueOf(75)) > 0)
                || (indicator.getDistanceToMa20() != null && indicator.getDistanceToMa20().compareTo(BigDecimal.valueOf(12)) > 0)) {
            return "OVERHEATED";
        }
        return "UNKNOWN";
    }

    private TechnicalSignalDTO toDto(TechnicalSignalEntity signal) {
        DailyKlineEntity kline = dailyKlineMapper.selectOne(new LambdaQueryWrapper<DailyKlineEntity>()
                .eq(DailyKlineEntity::getSymbolCode, signal.getSymbolCode())
                .eq(DailyKlineEntity::getTradeDate, signal.getTradeDate())
                .last("LIMIT 1"));
        IndicatorSnapshotEntity indicator = indicatorSnapshotMapper.selectOne(new LambdaQueryWrapper<IndicatorSnapshotEntity>()
                .eq(IndicatorSnapshotEntity::getSymbolCode, signal.getSymbolCode())
                .eq(IndicatorSnapshotEntity::getTradeDate, signal.getTradeDate())
                .last("LIMIT 1"));
        return TechnicalSignalConverter.toDto(signal, kline, indicator);
    }

    private int priority(String signalType) {
        return switch (signalType) {
            case "AVOID" -> 1;
            case "BUY_CANDIDATE" -> 2;
            case "WATCH" -> 3;
            case "SELL_WARNING" -> 4;
            case "NEUTRAL" -> 5;
            default -> 99;
        };
    }

    private boolean gt(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) > 0;
    }

    private boolean lt(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) < 0;
    }

    private boolean between(BigDecimal value, int min, int max) {
        return value != null
                && value.compareTo(BigDecimal.valueOf(min)) >= 0
                && value.compareTo(BigDecimal.valueOf(max)) <= 0;
    }

    private String normalize(String value) {
        return TextUtil.normalizeCode(value);
    }
}
