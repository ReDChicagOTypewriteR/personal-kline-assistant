package com.alexjoker.quant.backtest.support;

import com.alexjoker.quant.common.util.TextUtil;

import java.math.BigDecimal;

public final class BacktestDefaults {
    public static final String STRATEGY_ETF_TREND = "ETF_TREND";
    public static final String STRATEGY_ETF_DCA = "ETF_DCA";
    public static final String DCA_MONTHLY = "MONTHLY";
    public static final String DCA_WEEKLY = "WEEKLY";
    public static final BigDecimal INITIAL_CAPITAL = BigDecimal.valueOf(100000);
    public static final BigDecimal POSITION_RATIO = BigDecimal.valueOf(0.95);
    public static final BigDecimal FEE_RATE = BigDecimal.valueOf(0.0003);
    public static final BigDecimal SLIPPAGE_RATE = BigDecimal.valueOf(0.0005);
    public static final int LOT_SIZE = 100;
    public static final BigDecimal DCA_BASE_AMOUNT = BigDecimal.valueOf(1000);
    public static final BigDecimal DCA_LOW_MULTIPLIER = BigDecimal.valueOf(2.0);
    public static final BigDecimal DCA_HIGH_MULTIPLIER = BigDecimal.valueOf(0.5);

    private BacktestDefaults() {
    }

    public static BigDecimal positiveOrDefault(BigDecimal value, BigDecimal defaultValue) {
        return value == null || value.compareTo(BigDecimal.ZERO) <= 0 ? defaultValue : value;
    }

    public static BigDecimal ratioOrDefault(BigDecimal value, BigDecimal defaultValue) {
        return value == null || value.compareTo(BigDecimal.ZERO) < 0 ? defaultValue : value;
    }

    public static int lotSizeOrDefault(Integer value) {
        return value == null || value <= 0 ? LOT_SIZE : value;
    }

    public static String strategyType(String value) {
        String normalized = TextUtil.normalizeCode(value);
        return STRATEGY_ETF_DCA.equals(normalized) ? STRATEGY_ETF_DCA : STRATEGY_ETF_TREND;
    }

    public static String dcaFrequency(String value) {
        String normalized = TextUtil.normalizeCode(value);
        return DCA_WEEKLY.equals(normalized) ? DCA_WEEKLY : DCA_MONTHLY;
    }
}
