package com.alexjoker.quant.backtest.service.impl;

import com.alexjoker.quant.backtest.dto.BacktestBenchmarkDTO;
import com.alexjoker.quant.backtest.dto.BacktestBatchItemDTO;
import com.alexjoker.quant.backtest.dto.BacktestBatchResultDTO;
import com.alexjoker.quant.backtest.dto.BacktestBatchRunRequest;
import com.alexjoker.quant.backtest.dto.BacktestDataQualityDTO;
import com.alexjoker.quant.backtest.dto.BacktestResultDTO;
import com.alexjoker.quant.backtest.dto.BacktestRunRequest;
import com.alexjoker.quant.backtest.dto.BacktestTradeDTO;
import com.alexjoker.quant.backtest.dto.EquityCurvePointDTO;
import com.alexjoker.quant.backtest.engine.BacktestMetricsCalculator;
import com.alexjoker.quant.backtest.engine.BacktestTradeExecutor;
import com.alexjoker.quant.backtest.service.BacktestRecordService;
import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.common.util.NumberUtil;
import com.alexjoker.quant.common.util.TextUtil;
import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.indicator.mapper.IndicatorSnapshotMapper;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.marketdata.mapper.DailyKlineMapper;
import com.alexjoker.quant.signal.rule.TechnicalScoreCalculator;
import com.alexjoker.quant.signal.rule.TechnicalScoreResult;
import com.alexjoker.quant.strategy.rule.EtfTrendStrategyRule;
import com.alexjoker.quant.symbol.dto.SymbolDTO;
import com.alexjoker.quant.symbol.service.SymbolService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.alexjoker.quant.backtest.support.BacktestDefaults.DCA_BASE_AMOUNT;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.DCA_HIGH_MULTIPLIER;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.DCA_LOW_MULTIPLIER;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.DCA_WEEKLY;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.FEE_RATE;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.INITIAL_CAPITAL;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.POSITION_RATIO;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.SLIPPAGE_RATE;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.STRATEGY_ETF_DCA;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.dcaFrequency;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.lotSizeOrDefault;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.positiveOrDefault;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.ratioOrDefault;
import static com.alexjoker.quant.backtest.support.BacktestDefaults.strategyType;

@Service
@RequiredArgsConstructor
public class BacktestServiceImpl implements com.alexjoker.quant.backtest.service.BacktestService {
    private final SymbolService symbolService;
    private final DailyKlineMapper dailyKlineMapper;
    private final IndicatorSnapshotMapper indicatorSnapshotMapper;
    private final TechnicalScoreCalculator technicalScoreCalculator;
    private final EtfTrendStrategyRule etfTrendStrategyRule;
    private final BacktestTradeExecutor tradeExecutor;
    private final BacktestMetricsCalculator metricsCalculator;
    private final BacktestRecordService backtestRecordService;

    @Override
    public BacktestResultDTO run(BacktestRunRequest request) {
        String symbolCode = normalize(request.getSymbolCode());
        SymbolDTO symbol = symbolService.getByCode(symbolCode);
        BigDecimal initialCapital = positiveOrDefault(request.getInitialCapital(), INITIAL_CAPITAL);
        BigDecimal positionRatio = ratioOrDefault(request.getPositionRatio(), POSITION_RATIO);
        BigDecimal feeRate = ratioOrDefault(request.getFeeRate(), FEE_RATE);
        BigDecimal slippageRate = ratioOrDefault(request.getSlippageRate(), SLIPPAGE_RATE);
        int lotSize = lotSizeOrDefault(request.getLotSize());
        String strategyType = strategyType(request.getStrategyType());

        List<DailyKlineEntity> klines = listKlines(symbolCode, request.getStart(), request.getEnd());
        if (klines.isEmpty()) {
            throw new BusinessException("没有可用日 K 数据，无法回测: " + symbolCode);
        }
        BacktestDataQualityDTO dataQuality = inspectDataQuality(klines);
        if (!Boolean.TRUE.equals(dataQuality.getPassed())) {
            throw new BusinessException("K 线数据质量检查未通过: " + String.join("；", dataQuality.getWarnings()));
        }
        Map<LocalDate, IndicatorSnapshotEntity> indicatorMap = indicatorMap(symbolCode, klines.getFirst().getTradeDate(), klines.getLast().getTradeDate());

        BigDecimal cash = initialCapital;
        int shares = 0;
        BigDecimal totalCost = BigDecimal.ZERO;
        BigDecimal peakEquity = initialCapital;
        BigDecimal previousEquity = initialCapital;
        BigDecimal maxDrawdown = BigDecimal.ZERO;
        BigDecimal realizedPnl = BigDecimal.ZERO;
        BigDecimal grossProfit = BigDecimal.ZERO;
        BigDecimal grossLoss = BigDecimal.ZERO;
        int winCount = 0;
        int lossCount = 0;

        List<BacktestTradeDTO> trades = new ArrayList<>();
        List<EquityCurvePointDTO> curve = new ArrayList<>();
        LocalDate lastDcaDate = null;
        PendingOrder pendingOrder = null;

        for (DailyKlineEntity kline : klines) {
            IndicatorSnapshotEntity indicator = indicatorMap.get(kline.getTradeDate());
            if (pendingOrder != null) {
                if ("BUY_BUDGET".equals(pendingOrder.action())) {
                    BacktestTradeExecutor.BuyExecution execution = tradeExecutor.buyByBudget(cash, kline.getOpenPrice(), feeRate, slippageRate,
                            pendingOrder.budget(), lotSize);
                    if (execution.shares() > 0) {
                        cash = cash.subtract(execution.amount()).subtract(execution.fee());
                        int oldShares = shares;
                        shares += execution.shares();
                        totalCost = totalCost.add(execution.amount()).add(execution.fee());
                        trades.add(trade(kline.getTradeDate(), "BUY", execution.price(), execution.shares(),
                                execution.amount(), execution.fee(), null, null,
                                pendingOrder.reason() + "；信号日 " + pendingOrder.signalDate() + "，下一交易日开盘成交，持仓 " + oldShares + " -> " + shares));
                    }
                } else if ("BUY_RATIO".equals(pendingOrder.action()) && shares == 0) {
                    BacktestTradeExecutor.BuyExecution execution = tradeExecutor.buyByPositionRatio(cash, kline.getOpenPrice(), feeRate, slippageRate, positionRatio, lotSize);
                    if (execution.shares() > 0) {
                        cash = cash.subtract(execution.amount()).subtract(execution.fee());
                        shares = execution.shares();
                        totalCost = execution.amount().add(execution.fee());
                        trades.add(trade(kline.getTradeDate(), "BUY", execution.price(), execution.shares(), execution.amount(), execution.fee(), null, null,
                                pendingOrder.reason() + "；信号日 " + pendingOrder.signalDate() + "，下一交易日开盘成交"));
                    }
                } else if ("SELL_ALL".equals(pendingOrder.action()) && shares > 0) {
                    int sellShares = shares;
                    BacktestTradeExecutor.SellExecution execution = tradeExecutor.sellAll(sellShares, totalCost, kline.getOpenPrice(), feeRate, slippageRate);
                    cash = cash.add(execution.amount()).subtract(execution.fee());
                    realizedPnl = realizedPnl.add(execution.pnl());
                    if (execution.pnl().compareTo(BigDecimal.ZERO) >= 0) {
                        winCount++;
                        grossProfit = grossProfit.add(execution.pnl());
                    } else {
                        lossCount++;
                        grossLoss = grossLoss.add(execution.pnl().abs());
                    }
                    trades.add(trade(kline.getTradeDate(), "SELL", execution.price(), sellShares, execution.amount(), execution.fee(), execution.pnl(), execution.pnlRate(),
                            pendingOrder.reason() + "；信号日 " + pendingOrder.signalDate() + "，下一交易日开盘成交"));
                    shares = 0;
                    totalCost = BigDecimal.ZERO;
                }
                pendingOrder = null;
            }

            if (STRATEGY_ETF_DCA.equals(strategyType)) {
                if (!etfTrendStrategyRule.supports(symbol)) {
                    throw new BusinessException("ETF 定投策略仅支持 ETF 标的: " + symbolCode);
                }
                if (shouldDcaBuy(kline.getTradeDate(), lastDcaDate, request.getDcaFrequency())) {
                    BigDecimal multiplier = dcaMultiplier(kline, indicator, request);
                    BigDecimal investmentAmount = positiveOrDefault(request.getDcaBaseAmount(), DCA_BASE_AMOUNT)
                            .multiply(multiplier);
                    pendingOrder = new PendingOrder("BUY_BUDGET", kline.getTradeDate(), dcaReason(kline, indicator, request.getDcaFrequency(), multiplier), investmentAmount);
                    lastDcaDate = kline.getTradeDate();
                }
            } else if (indicator != null) {
                TechnicalScoreResult score = technicalScoreCalculator.calculate(kline, indicator);
                if (pendingOrder == null && shares == 0 && shouldBuy(kline, indicator, score.getScore(), symbol)) {
                    pendingOrder = new PendingOrder("BUY_RATIO", kline.getTradeDate(),
                            "BUY_CANDIDATE: 技术评分 " + score.getScore(), null);
                } else if (shares > 0 && shouldSell(kline, indicator, score.getScore(), symbol)) {
                    pendingOrder = new PendingOrder("SELL_ALL", kline.getTradeDate(), sellReason(score.getScore(), symbol), null);
                }
            }

            BigDecimal marketValue = kline.getClosePrice().multiply(BigDecimal.valueOf(shares));
            BigDecimal totalEquity = cash.add(marketValue);
            if (totalEquity.compareTo(peakEquity) > 0) {
                peakEquity = totalEquity;
            }
            BigDecimal drawdown = metricsCalculator.pct(totalEquity.subtract(peakEquity), peakEquity);
            if (drawdown.compareTo(maxDrawdown) < 0) {
                maxDrawdown = drawdown;
            }
            curve.add(metricsCalculator.curvePoint(kline, cash, marketValue, totalEquity, previousEquity, peakEquity, shares));
            previousEquity = totalEquity;
        }

        DailyKlineEntity lastKline = klines.getLast();
        BigDecimal finalEquity = curve.isEmpty() ? initialCapital : curve.getLast().getTotalEquity();
        BacktestResultDTO result = new BacktestResultDTO();
        result.setSymbolCode(symbolCode);
        result.setStrategyType(strategyType);
        result.setExecutionMode("NEXT_OPEN");
        result.setDataQuality(dataQuality);
        result.setStart(klines.getFirst().getTradeDate());
        result.setEnd(lastKline.getTradeDate());
        result.setTrades(trades);
        result.setEquityCurve(curve);
        result.setBenchmark(buyAndHoldBenchmark(klines, initialCapital, positionRatio, feeRate, slippageRate, lotSize));
        result.setPosition(metricsCalculator.position(symbolCode, shares, totalCost, lastKline.getClosePrice()));
        result.setSummary(metricsCalculator.summary(initialCapital, finalEquity, realizedPnl, maxDrawdown, winCount, lossCount, grossProfit, grossLoss,
                klines.getFirst().getTradeDate(), lastKline.getTradeDate(), trades.size()));
        result.setRecordId(backtestRecordService.save(request, result));
        return result;
    }

    @Override
    public BacktestBatchResultDTO runBatch(BacktestBatchRunRequest request) {
        List<String> symbolCodes = request == null ? List.of() : request.getSymbolCodes();
        if (symbolCodes == null || symbolCodes.isEmpty()) {
            throw new BusinessException("批量回测至少需要选择一个标的");
        }
        List<BacktestBatchItemDTO> items = new ArrayList<>();
        int successCount = 0;
        for (String symbolCode : symbolCodes) {
            String code = normalize(symbolCode);
            BacktestBatchItemDTO item = new BacktestBatchItemDTO();
            item.setSymbolCode(code);
            try {
                BacktestResultDTO result = run(toSingleRequest(request, code));
                item = toBatchItem(result);
                item.setSuccess(true);
                item.setMessage("SUCCESS");
                successCount++;
            } catch (Exception ex) {
                item.setSuccess(false);
                item.setMessage(ex.getMessage());
            }
            items.add(item);
        }
        BacktestBatchResultDTO result = new BacktestBatchResultDTO();
        result.setTotalCount(items.size());
        result.setSuccessCount(successCount);
        result.setFailedCount(items.size() - successCount);
        result.setResults(items);
        return result;
    }

    private BacktestRunRequest toSingleRequest(BacktestBatchRunRequest source, String symbolCode) {
        BacktestRunRequest request = new BacktestRunRequest();
        request.setSymbolCode(symbolCode);
        request.setStart(source.getStart());
        request.setEnd(source.getEnd());
        request.setInitialCapital(source.getInitialCapital());
        request.setPositionRatio(source.getPositionRatio());
        request.setFeeRate(source.getFeeRate());
        request.setSlippageRate(source.getSlippageRate());
        request.setLotSize(source.getLotSize());
        request.setStrategyType(source.getStrategyType());
        request.setDcaFrequency(source.getDcaFrequency());
        request.setDcaBaseAmount(source.getDcaBaseAmount());
        request.setDcaLowMultiplier(source.getDcaLowMultiplier());
        request.setDcaHighMultiplier(source.getDcaHighMultiplier());
        return request;
    }

    private BacktestBatchItemDTO toBatchItem(BacktestResultDTO result) {
        BacktestBatchItemDTO item = new BacktestBatchItemDTO();
        item.setSymbolCode(result.getSymbolCode());
        item.setRecordId(result.getRecordId());
        item.setStrategyType(result.getStrategyType());
        item.setStart(result.getStart());
        item.setEnd(result.getEnd());
        if (result.getSummary() != null) {
            item.setFinalEquity(result.getSummary().getFinalEquity());
            item.setTotalReturnRate(result.getSummary().getTotalReturnRate());
            item.setMaxDrawdownRate(result.getSummary().getMaxDrawdownRate());
            item.setWinRate(result.getSummary().getWinRate());
            item.setTradeCount(result.getSummary().getTradeCount());
        }
        if (result.getBenchmark() != null) {
            item.setBenchmarkReturnRate(result.getBenchmark().getTotalReturnRate());
            item.setBenchmarkMaxDrawdownRate(result.getBenchmark().getMaxDrawdownRate());
        }
        if (item.getTotalReturnRate() != null && item.getBenchmarkReturnRate() != null) {
            item.setExcessReturnRate(item.getTotalReturnRate().subtract(item.getBenchmarkReturnRate()));
        }
        return item;
    }

    private BacktestDataQualityDTO inspectDataQuality(List<DailyKlineEntity> klines) {
        BacktestDataQualityDTO dto = new BacktestDataQualityDTO();
        dto.setTotalRows(klines.size());
        dto.setFirstTradeDate(klines.getFirst().getTradeDate());
        dto.setLastTradeDate(klines.getLast().getTradeDate());
        int invalidPriceRows = 0;
        int missingVolumeRows = 0;
        int missingAmountRows = 0;
        int longCalendarGapCount = 0;
        int suspiciousChangeRows = 0;

        DailyKlineEntity previous = null;
        for (DailyKlineEntity kline : klines) {
            if (!validPriceRow(kline)) {
                invalidPriceRows++;
                dto.getWarnings().add("价格关系异常: " + kline.getTradeDate());
            }
            if (kline.getVolume() == null || kline.getVolume().compareTo(BigDecimal.ZERO) <= 0) {
                missingVolumeRows++;
            }
            if (kline.getAmount() == null || kline.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
                missingAmountRows++;
            }
            if (previous != null) {
                long days = ChronoUnit.DAYS.between(previous.getTradeDate(), kline.getTradeDate());
                if (days > 10) {
                    longCalendarGapCount++;
                    dto.getWarnings().add("交易日期断档超过 10 天: " + previous.getTradeDate() + " -> " + kline.getTradeDate());
                }
                BigDecimal change = metricsCalculator.pct(kline.getClosePrice().subtract(previous.getClosePrice()), previous.getClosePrice()).abs();
                if (change.compareTo(BigDecimal.valueOf(20)) > 0) {
                    suspiciousChangeRows++;
                    dto.getWarnings().add("单日涨跌幅超过 20%，请确认复权或数据源: " + kline.getTradeDate());
                }
            }
            previous = kline;
        }

        dto.setInvalidPriceRows(invalidPriceRows);
        dto.setMissingVolumeRows(missingVolumeRows);
        dto.setMissingAmountRows(missingAmountRows);
        dto.setLongCalendarGapCount(longCalendarGapCount);
        dto.setSuspiciousChangeRows(suspiciousChangeRows);
        if (missingVolumeRows > 0) {
            dto.getWarnings().add("存在 " + missingVolumeRows + " 条缺少成交量或成交量为 0 的 K 线");
        }
        if (missingAmountRows > 0) {
            dto.getWarnings().add("存在 " + missingAmountRows + " 条缺少成交额或成交额为 0 的 K 线");
        }
        dto.setPassed(invalidPriceRows == 0);
        return dto;
    }

    private boolean validPriceRow(DailyKlineEntity kline) {
        if (kline.getOpenPrice() == null || kline.getHighPrice() == null || kline.getLowPrice() == null || kline.getClosePrice() == null) {
            return false;
        }
        if (kline.getOpenPrice().compareTo(BigDecimal.ZERO) <= 0
                || kline.getHighPrice().compareTo(BigDecimal.ZERO) <= 0
                || kline.getLowPrice().compareTo(BigDecimal.ZERO) <= 0
                || kline.getClosePrice().compareTo(BigDecimal.ZERO) <= 0) {
            return false;
        }
        BigDecimal high = kline.getHighPrice();
        BigDecimal low = kline.getLowPrice();
        return high.compareTo(low) >= 0
                && high.compareTo(kline.getOpenPrice()) >= 0
                && high.compareTo(kline.getClosePrice()) >= 0
                && low.compareTo(kline.getOpenPrice()) <= 0
                && low.compareTo(kline.getClosePrice()) <= 0;
    }

    private BacktestBenchmarkDTO buyAndHoldBenchmark(
            List<DailyKlineEntity> klines,
            BigDecimal initialCapital,
            BigDecimal positionRatio,
            BigDecimal feeRate,
            BigDecimal slippageRate,
            int lotSize
    ) {
        DailyKlineEntity first = klines.getFirst();
        DailyKlineEntity last = klines.getLast();
        BacktestTradeExecutor.BuyExecution execution = tradeExecutor.buyByPositionRatio(
                initialCapital,
                first.getOpenPrice(),
                feeRate,
                slippageRate,
                positionRatio,
                lotSize
        );
        BigDecimal cash = initialCapital.subtract(execution.amount()).subtract(execution.fee());
        int shares = execution.shares();
        BigDecimal peakEquity = initialCapital;
        BigDecimal maxDrawdown = BigDecimal.ZERO;

        for (DailyKlineEntity kline : klines) {
            BigDecimal totalEquity = cash.add(kline.getClosePrice().multiply(BigDecimal.valueOf(shares)));
            if (totalEquity.compareTo(peakEquity) > 0) {
                peakEquity = totalEquity;
            }
            BigDecimal drawdown = metricsCalculator.pct(totalEquity.subtract(peakEquity), peakEquity);
            if (drawdown.compareTo(maxDrawdown) < 0) {
                maxDrawdown = drawdown;
            }
        }

        BigDecimal finalEquity = cash.add(last.getClosePrice().multiply(BigDecimal.valueOf(shares)));
        BacktestBenchmarkDTO dto = new BacktestBenchmarkDTO();
        dto.setFinalEquity(scale(finalEquity));
        dto.setTotalReturnRate(scale(metricsCalculator.pct(finalEquity.subtract(initialCapital), initialCapital)));
        dto.setAnnualizedReturnRate(scale(metricsCalculator.annualizedReturn(initialCapital, finalEquity, first.getTradeDate(), last.getTradeDate())));
        dto.setMaxDrawdownRate(scale(maxDrawdown));
        dto.setShares(shares);
        dto.setCash(scale(cash));
        dto.setStartPrice(scale(first.getOpenPrice()));
        dto.setEndPrice(scale(last.getClosePrice()));
        return dto;
    }

    private List<DailyKlineEntity> listKlines(String symbolCode, LocalDate start, LocalDate end) {
        LambdaQueryWrapper<DailyKlineEntity> wrapper = new LambdaQueryWrapper<DailyKlineEntity>()
                .eq(DailyKlineEntity::getSymbolCode, symbolCode);
        if (start != null) {
            wrapper.ge(DailyKlineEntity::getTradeDate, start);
        }
        if (end != null) {
            wrapper.le(DailyKlineEntity::getTradeDate, end);
        }
        wrapper.orderByAsc(DailyKlineEntity::getTradeDate);
        return dailyKlineMapper.selectList(wrapper);
    }

    private Map<LocalDate, IndicatorSnapshotEntity> indicatorMap(String symbolCode, LocalDate start, LocalDate end) {
        List<IndicatorSnapshotEntity> indicators = indicatorSnapshotMapper.selectList(new LambdaQueryWrapper<IndicatorSnapshotEntity>()
                .eq(IndicatorSnapshotEntity::getSymbolCode, symbolCode)
                .ge(IndicatorSnapshotEntity::getTradeDate, start)
                .le(IndicatorSnapshotEntity::getTradeDate, end)
                .orderByAsc(IndicatorSnapshotEntity::getTradeDate));
        Map<LocalDate, IndicatorSnapshotEntity> map = new HashMap<>();
        indicators.forEach(item -> map.put(item.getTradeDate(), item));
        return map;
    }

    private boolean shouldBuy(DailyKlineEntity kline, IndicatorSnapshotEntity indicator, int score, SymbolDTO symbol) {
        if (etfTrendStrategyRule.supports(symbol)) {
            return etfTrendStrategyRule.isBuyCandidate(kline, indicator, score);
        }
        return score >= 75
                && gt(kline.getClosePrice(), indicator.getMa20())
                && gt(indicator.getMa5(), indicator.getMa10())
                && gt(indicator.getMa10(), indicator.getMa20())
                && between(indicator.getRsi14(), 45, 70);
    }

    private boolean shouldSell(DailyKlineEntity kline, IndicatorSnapshotEntity indicator, int score, SymbolDTO symbol) {
        if (etfTrendStrategyRule.supports(symbol)) {
            return etfTrendStrategyRule.isSellWarning(kline, indicator);
        }
        return score < 40
                || lt(kline.getClosePrice(), indicator.getMa10())
                || lt(kline.getClosePrice(), indicator.getMa20())
                || (indicator.getRsi14() != null && indicator.getRsi14().compareTo(BigDecimal.valueOf(40)) < 0);
    }

    private boolean shouldDcaBuy(LocalDate tradeDate, LocalDate lastDcaDate, String frequency) {
        if (lastDcaDate == null) {
            return true;
        }
        String value = dcaFrequency(frequency);
        if (DCA_WEEKLY.equals(value)) {
            return !sameWeek(tradeDate, lastDcaDate);
        }
        return tradeDate.getYear() != lastDcaDate.getYear() || tradeDate.getMonthValue() != lastDcaDate.getMonthValue();
    }

    private boolean sameWeek(LocalDate left, LocalDate right) {
        java.time.temporal.WeekFields weekFields = java.time.temporal.WeekFields.ISO;
        return left.get(weekFields.weekBasedYear()) == right.get(weekFields.weekBasedYear())
                && left.get(weekFields.weekOfWeekBasedYear()) == right.get(weekFields.weekOfWeekBasedYear());
    }

    private BigDecimal dcaMultiplier(DailyKlineEntity kline, IndicatorSnapshotEntity indicator, BacktestRunRequest request) {
        if (indicator == null || indicator.getMa20() == null || kline.getClosePrice() == null) {
            return BigDecimal.ONE;
        }
        if (lt(kline.getClosePrice(), indicator.getMa20())) {
            return positiveOrDefault(request.getDcaLowMultiplier(), DCA_LOW_MULTIPLIER);
        }
        if (gt(kline.getClosePrice(), indicator.getMa20())) {
            return positiveOrDefault(request.getDcaHighMultiplier(), DCA_HIGH_MULTIPLIER);
        }
        return BigDecimal.ONE;
    }

    private String dcaReason(
            DailyKlineEntity kline,
            IndicatorSnapshotEntity indicator,
            String frequency,
            BigDecimal multiplier
    ) {
        String period = DCA_WEEKLY.equals(dcaFrequency(frequency)) ? "每周" : "每月";
        String position = "未取得 MA20，按 1.0 倍定投";
        if (indicator != null && indicator.getMa20() != null) {
            if (lt(kline.getClosePrice(), indicator.getMa20())) {
                position = "低于 MA20，按 " + multiplier.stripTrailingZeros().toPlainString() + " 倍定投";
            } else if (gt(kline.getClosePrice(), indicator.getMa20())) {
                position = "高于 MA20，按 " + multiplier.stripTrailingZeros().toPlainString() + " 倍定投";
            } else {
                position = "接近 MA20，按 1.0 倍定投";
            }
        }
        return "ETF_DCA: " + period + "定投，" + position;
    }

    private String sellReason(int score, SymbolDTO symbol) {
        if (etfTrendStrategyRule.supports(symbol)) {
            return "ETF_SELL_WARNING: 跌破趋势支撑或 RSI 动能转弱，技术评分 " + score;
        }
        return "SELL_WARNING: 技术评分 " + score;
    }

    private BacktestTradeDTO trade(LocalDate date, String action, BigDecimal price, int shares, BigDecimal amount,
                                   BigDecimal fee, BigDecimal pnl, BigDecimal pnlRate, String reason) {
        BacktestTradeDTO dto = new BacktestTradeDTO();
        dto.setTradeDate(date);
        dto.setAction(action);
        dto.setPrice(scale(price));
        dto.setShares(shares);
        dto.setAmount(scale(amount));
        dto.setFee(scale(fee));
        dto.setPnl(scaleNullable(pnl));
        dto.setPnlRate(scaleNullable(pnlRate));
        dto.setReason(reason);
        return dto;
    }

    private BigDecimal scale(BigDecimal value) {
        return NumberUtil.scale(value);
    }

    private BigDecimal scaleNullable(BigDecimal value) {
        return value == null ? null : NumberUtil.scale(value);
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

    private record PendingOrder(String action, LocalDate signalDate, String reason, BigDecimal budget) {
    }

}
