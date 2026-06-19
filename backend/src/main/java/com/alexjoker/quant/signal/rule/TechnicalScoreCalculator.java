package com.alexjoker.quant.signal.rule;

import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
public class TechnicalScoreCalculator {
    public TechnicalScoreResult calculate(DailyKlineEntity kline, IndicatorSnapshotEntity indicator) {
        TechnicalScoreResult result = new TechnicalScoreResult();
        int score = 0;
        BigDecimal close = kline.getClosePrice();

        if (gt(close, indicator.getMa20())) {
            score += 20;
            result.getReasons().add("收盘价位于 MA20 上方，趋势偏强");
        } else if (indicator.getMa20() != null) {
            result.getRiskNotes().add("收盘价低于 MA20，趋势偏弱");
        }

        if (gt(indicator.getMa5(), indicator.getMa10())) {
            score += 15;
            result.getReasons().add("MA5 高于 MA10，短期趋势向上");
        }
        if (gt(indicator.getMa10(), indicator.getMa20())) {
            score += 15;
            result.getReasons().add("MA10 高于 MA20，中期趋势向上");
        }
        if (gt(indicator.getMa20(), indicator.getMa60())) {
            score += 10;
            result.getReasons().add("MA20 高于 MA60，长期趋势保持向上");
        }

        BigDecimal rsi = indicator.getRsi14();
        if (between(rsi, 45, 70)) {
            score += 15;
            result.getReasons().add("RSI 位于 45-70 区间，未明显过热");
        } else if (rsi != null && rsi.compareTo(BigDecimal.valueOf(75)) > 0) {
            result.getRiskNotes().add("RSI 高于 75，短期可能过热");
        } else if (rsi != null && rsi.compareTo(BigDecimal.valueOf(40)) < 0) {
            result.getRiskNotes().add("RSI 低于 40，短期动能偏弱");
        }

        BigDecimal volumeRatio = indicator.getVolumeRatio();
        if (volumeRatio != null && volumeRatio.compareTo(BigDecimal.ONE) >= 0) {
            score += 10;
            result.getReasons().add("成交量不低于 20 日均量");
        } else if (volumeRatio != null) {
            result.getRiskNotes().add("成交量低于 20 日均量，动能不足");
        }

        BigDecimal fiveDayReturn = indicator.getFiveDayReturn();
        if (fiveDayReturn != null && fiveDayReturn.compareTo(BigDecimal.ZERO) > 0) {
            score += 10;
            result.getReasons().add("近 5 日收益为正，短期表现偏强");
        }

        BigDecimal distance = indicator.getDistanceToMa20();
        if (distance != null && distance.abs().compareTo(BigDecimal.valueOf(8)) <= 0) {
            score += 5;
            result.getReasons().add("价格距离 MA20 不超过 8%，位置相对可控");
        } else if (distance != null && distance.compareTo(BigDecimal.valueOf(8)) > 0) {
            result.getRiskNotes().add("价格距离 MA20 超过 8%，存在追高风险");
        }

        result.setScore(Math.min(score, 100));
        if (result.getReasons().isEmpty()) {
            result.getReasons().add("当前技术条件尚未形成明确优势");
        }
        return result;
    }

    private boolean gt(BigDecimal left, BigDecimal right) {
        return left != null && right != null && left.compareTo(right) > 0;
    }

    private boolean between(BigDecimal value, int min, int max) {
        return value != null
                && value.compareTo(BigDecimal.valueOf(min)) >= 0
                && value.compareTo(BigDecimal.valueOf(max)) <= 0;
    }
}
