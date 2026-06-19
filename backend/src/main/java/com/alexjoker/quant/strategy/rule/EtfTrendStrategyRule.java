package com.alexjoker.quant.strategy.rule;

import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.signal.config.TechnicalSignalRuleProperties;
import com.alexjoker.quant.symbol.dto.SymbolDTO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@RequiredArgsConstructor
public class EtfTrendStrategyRule {
    private final TechnicalSignalRuleProperties properties;

    public boolean supports(SymbolDTO symbol) {
        return symbol != null && "ETF".equalsIgnoreCase(symbol.getAssetType());
    }

    public boolean isBuyCandidate(DailyKlineEntity kline, IndicatorSnapshotEntity indicator, int score) {
        boolean maConfirmed = properties.isEtfAllowPartialMaConfirmation()
                ? gt(indicator.getMa5(), indicator.getMa10()) || gt(indicator.getMa10(), indicator.getMa20())
                : gt(indicator.getMa5(), indicator.getMa10()) && gt(indicator.getMa10(), indicator.getMa20());
        return score >= properties.getEtfBuyScoreThreshold()
                && gt(kline.getClosePrice(), indicator.getMa20())
                && maConfirmed
                && between(indicator.getRsi14(), properties.getEtfRsiMin(), properties.getEtfRsiMax());
    }

    public boolean isAvoid(DailyKlineEntity kline, IndicatorSnapshotEntity indicator, int score) {
        return score < 40
                || lt(kline.getClosePrice(), indicator.getMa20())
                || (indicator.getRsi14() != null && indicator.getRsi14().compareTo(BigDecimal.valueOf(75)) > 0)
                || (indicator.getDistanceToMa20() != null && indicator.getDistanceToMa20().compareTo(BigDecimal.valueOf(12)) > 0);
    }

    public boolean isSellWarning(DailyKlineEntity kline, IndicatorSnapshotEntity indicator) {
        return lt(kline.getClosePrice(), indicator.getMa10())
                || lt(kline.getClosePrice(), indicator.getMa20())
                || (indicator.getRsi14() != null && indicator.getRsi14().compareTo(BigDecimal.valueOf(40)) < 0);
    }

    public String trendState(DailyKlineEntity kline, IndicatorSnapshotEntity indicator) {
        if (gt(kline.getClosePrice(), indicator.getMa20()) && gt(indicator.getMa10(), indicator.getMa20())) {
            return "UP";
        }
        if (lt(kline.getClosePrice(), indicator.getMa20())) {
            return "DOWN";
        }
        return "SIDEWAYS";
    }

    public String avoidTrendState(DailyKlineEntity kline, IndicatorSnapshotEntity indicator) {
        if (lt(kline.getClosePrice(), indicator.getMa20())) {
            return "DOWN";
        }
        if ((indicator.getRsi14() != null && indicator.getRsi14().compareTo(BigDecimal.valueOf(75)) > 0)
                || (indicator.getDistanceToMa20() != null && indicator.getDistanceToMa20().compareTo(BigDecimal.valueOf(12)) > 0)) {
            return "OVERHEATED";
        }
        return "UNKNOWN";
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
}
