package com.alexjoker.quant.backtest.engine;

import com.alexjoker.quant.backtest.dto.BacktestPositionDTO;
import com.alexjoker.quant.backtest.dto.BacktestSummaryDTO;
import com.alexjoker.quant.backtest.dto.EquityCurvePointDTO;
import com.alexjoker.quant.common.util.NumberUtil;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

@Component
public class BacktestMetricsCalculator {
    public EquityCurvePointDTO curvePoint(
            DailyKlineEntity kline,
            BigDecimal cash,
            BigDecimal marketValue,
            BigDecimal totalEquity,
            BigDecimal previousEquity,
            BigDecimal peakEquity,
            int shares
    ) {
        BigDecimal drawdown = pct(totalEquity.subtract(peakEquity), peakEquity);
        EquityCurvePointDTO dto = new EquityCurvePointDTO();
        dto.setTradeDate(kline.getTradeDate());
        dto.setCash(scale(cash));
        dto.setMarketValue(scale(marketValue));
        dto.setTotalEquity(scale(totalEquity));
        dto.setDailyReturn(scale(pct(totalEquity.subtract(previousEquity), previousEquity)));
        dto.setDrawdown(scale(drawdown));
        dto.setPositionShares(shares);
        dto.setClosePrice(scale(kline.getClosePrice()));
        return dto;
    }

    public BacktestPositionDTO position(String symbolCode, int shares, BigDecimal totalCost, BigDecimal marketPrice) {
        BacktestPositionDTO dto = new BacktestPositionDTO();
        dto.setSymbolCode(symbolCode);
        dto.setShares(shares);
        dto.setMarketPrice(scale(marketPrice));
        dto.setMarketValue(scale(marketPrice.multiply(BigDecimal.valueOf(shares))));
        dto.setStatus(shares > 0 ? "HOLDING" : "EMPTY");
        if (shares > 0) {
            BigDecimal avgCost = totalCost.divide(BigDecimal.valueOf(shares), 6, RoundingMode.HALF_UP);
            BigDecimal unrealizedPnl = dto.getMarketValue().subtract(totalCost);
            dto.setAvgCost(scale(avgCost));
            dto.setUnrealizedPnl(scale(unrealizedPnl));
            dto.setUnrealizedPnlRate(scale(pct(unrealizedPnl, totalCost)));
        }
        return dto;
    }

    public BacktestSummaryDTO summary(
            BigDecimal initialCapital,
            BigDecimal finalEquity,
            BigDecimal realizedPnl,
            BigDecimal maxDrawdown,
            int winCount,
            int lossCount,
            BigDecimal grossProfit,
            BigDecimal grossLoss,
            LocalDate start,
            LocalDate end,
            int tradeCount
    ) {
        BacktestSummaryDTO dto = new BacktestSummaryDTO();
        dto.setInitialCapital(scale(initialCapital));
        dto.setFinalEquity(scale(finalEquity));
        dto.setTotalReturnRate(scale(pct(finalEquity.subtract(initialCapital), initialCapital)));
        dto.setAnnualizedReturnRate(scale(annualizedReturn(initialCapital, finalEquity, start, end)));
        dto.setMaxDrawdownRate(scale(maxDrawdown));
        dto.setRealizedPnl(scale(realizedPnl));
        dto.setTradeCount(tradeCount);
        dto.setWinCount(winCount);
        dto.setLossCount(lossCount);
        int closedTrades = winCount + lossCount;
        dto.setWinRate(closedTrades == 0 ? BigDecimal.ZERO : scale(BigDecimal.valueOf(winCount).multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(closedTrades), 4, RoundingMode.HALF_UP)));
        dto.setProfitLossRatio(grossLoss.compareTo(BigDecimal.ZERO) == 0 ? null : scale(grossProfit.divide(grossLoss, 4, RoundingMode.HALF_UP)));
        return dto;
    }

    public BigDecimal pct(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 6, RoundingMode.HALF_UP);
    }

    public BigDecimal annualizedReturn(BigDecimal initial, BigDecimal finalEquity, LocalDate start, LocalDate end) {
        long days = Math.max(1, ChronoUnit.DAYS.between(start, end));
        double total = finalEquity.divide(initial, 8, RoundingMode.HALF_UP).doubleValue();
        double annualized = Math.pow(total, 365.0 / days) - 1;
        return BigDecimal.valueOf(annualized * 100);
    }

    private BigDecimal scale(BigDecimal value) {
        return NumberUtil.scale(value);
    }
}
