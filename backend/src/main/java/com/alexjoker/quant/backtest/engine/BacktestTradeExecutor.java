package com.alexjoker.quant.backtest.engine;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class BacktestTradeExecutor {
    public BuyExecution buyByPositionRatio(
            BigDecimal cash,
            BigDecimal closePrice,
            BigDecimal feeRate,
            BigDecimal slippageRate,
            BigDecimal positionRatio,
            int lotSize
    ) {
        BigDecimal buyPrice = closePrice.multiply(BigDecimal.ONE.add(slippageRate));
        int shares = calculateBuyShares(cash, buyPrice, feeRate, positionRatio, lotSize);
        BigDecimal amount = buyPrice.multiply(BigDecimal.valueOf(shares));
        BigDecimal fee = amount.multiply(feeRate);
        return new BuyExecution(buyPrice, shares, amount, fee);
    }

    public BuyExecution buyByBudget(
            BigDecimal cash,
            BigDecimal closePrice,
            BigDecimal feeRate,
            BigDecimal slippageRate,
            BigDecimal budget,
            int lotSize
    ) {
        BigDecimal availableBudget = budget.min(cash);
        if (availableBudget.compareTo(BigDecimal.ZERO) <= 0) {
            return new BuyExecution(BigDecimal.ZERO, 0, BigDecimal.ZERO, BigDecimal.ZERO);
        }
        BigDecimal buyPrice = closePrice.multiply(BigDecimal.ONE.add(slippageRate));
        int shares = calculateBuyShares(availableBudget, buyPrice, feeRate, BigDecimal.ONE, lotSize);
        BigDecimal amount = buyPrice.multiply(BigDecimal.valueOf(shares));
        BigDecimal fee = amount.multiply(feeRate);
        return new BuyExecution(buyPrice, shares, amount, fee);
    }

    public SellExecution sellAll(
            int shares,
            BigDecimal totalCost,
            BigDecimal closePrice,
            BigDecimal feeRate,
            BigDecimal slippageRate
    ) {
        BigDecimal sellPrice = closePrice.multiply(BigDecimal.ONE.subtract(slippageRate));
        BigDecimal amount = sellPrice.multiply(BigDecimal.valueOf(shares));
        BigDecimal fee = amount.multiply(feeRate);
        BigDecimal pnl = amount.subtract(fee).subtract(totalCost);
        BigDecimal pnlRate = pct(pnl, totalCost);
        return new SellExecution(sellPrice, amount, fee, pnl, pnlRate);
    }

    private int calculateBuyShares(BigDecimal cash, BigDecimal price, BigDecimal feeRate, BigDecimal positionRatio, int lotSize) {
        BigDecimal budget = cash.multiply(positionRatio);
        BigDecimal costPerShare = price.multiply(BigDecimal.ONE.add(feeRate));
        int rawShares = budget.divide(costPerShare, 0, RoundingMode.DOWN).intValue();
        return rawShares / lotSize * lotSize;
    }

    private BigDecimal pct(BigDecimal numerator, BigDecimal denominator) {
        if (denominator == null || denominator.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }
        return numerator.multiply(BigDecimal.valueOf(100)).divide(denominator, 6, RoundingMode.HALF_UP);
    }

    public record BuyExecution(BigDecimal price, int shares, BigDecimal amount, BigDecimal fee) {
    }

    public record SellExecution(BigDecimal price, BigDecimal amount, BigDecimal fee, BigDecimal pnl, BigDecimal pnlRate) {
    }
}
