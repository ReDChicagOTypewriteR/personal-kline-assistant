package com.alexjoker.quant.common.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

public final class NumberUtil {
    private NumberUtil() {
    }

    public static BigDecimal scale(BigDecimal value) {
        return value == null ? null : value.setScale(4, RoundingMode.HALF_UP);
    }

    public static BigDecimal bd(double value) {
        return BigDecimal.valueOf(value).setScale(4, RoundingMode.HALF_UP);
    }

    public static String joinNullable(String separator, Iterable<String> values) {
        StringBuilder builder = new StringBuilder();
        for (String value : values) {
            if (value == null || value.isBlank()) {
                continue;
            }
            if (!builder.isEmpty()) {
                builder.append(separator);
            }
            builder.append(value);
        }
        return builder.toString();
    }
}
