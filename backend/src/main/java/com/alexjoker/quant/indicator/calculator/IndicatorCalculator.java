package com.alexjoker.quant.indicator.calculator;

import com.alexjoker.quant.common.util.NumberUtil;
import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

@Component
public class IndicatorCalculator {
    public List<IndicatorSnapshotEntity> calculate(String symbolCode, List<DailyKlineEntity> klines) {
        List<IndicatorSnapshotEntity> results = new ArrayList<>();
        for (int i = 0; i < klines.size(); i++) {
            DailyKlineEntity kline = klines.get(i);
            IndicatorSnapshotEntity snapshot = new IndicatorSnapshotEntity();
            snapshot.setSymbolCode(symbolCode);
            snapshot.setTradeDate(kline.getTradeDate());
            snapshot.setMa5(ma(klines, i, 5));
            snapshot.setMa10(ma(klines, i, 10));
            snapshot.setMa20(ma(klines, i, 20));
            snapshot.setMa60(ma(klines, i, 60));
            snapshot.setRsi14(rsi(klines, i, 14));
            snapshot.setAtr14(atr(klines, i, 14));
            snapshot.setVolumeMa20(volumeMa(klines, i, 20));
            snapshot.setVolumeRatio(volumeRatio(kline.getVolume(), snapshot.getVolumeMa20()));
            snapshot.setFiveDayReturn(fiveDayReturn(klines, i));
            snapshot.setDistanceToMa20(distanceToMa(kline.getClosePrice(), snapshot.getMa20()));
            results.add(snapshot);
        }
        return results;
    }

    private BigDecimal ma(List<DailyKlineEntity> klines, int index, int period) {
        if (index + 1 < period) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = index - period + 1; i <= index; i++) {
            sum = sum.add(klines.get(i).getClosePrice());
        }
        return NumberUtil.scale(sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP));
    }

    private BigDecimal rsi(List<DailyKlineEntity> klines, int index, int period) {
        if (index < period) {
            return null;
        }
        BigDecimal gain = BigDecimal.ZERO;
        BigDecimal loss = BigDecimal.ZERO;
        for (int i = index - period + 1; i <= index; i++) {
            BigDecimal change = klines.get(i).getClosePrice().subtract(klines.get(i - 1).getClosePrice());
            if (change.signum() >= 0) {
                gain = gain.add(change);
            } else {
                loss = loss.add(change.abs());
            }
        }
        BigDecimal avgGain = gain.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        BigDecimal avgLoss = loss.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP);
        if (avgLoss.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.valueOf(100).setScale(4, RoundingMode.HALF_UP);
        }
        BigDecimal rs = avgGain.divide(avgLoss, 8, RoundingMode.HALF_UP);
        BigDecimal rsi = BigDecimal.valueOf(100).subtract(
                BigDecimal.valueOf(100).divide(BigDecimal.ONE.add(rs), 8, RoundingMode.HALF_UP)
        );
        return NumberUtil.scale(rsi);
    }

    private BigDecimal atr(List<DailyKlineEntity> klines, int index, int period) {
        if (index < period) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = index - period + 1; i <= index; i++) {
            DailyKlineEntity today = klines.get(i);
            BigDecimal previousClose = klines.get(i - 1).getClosePrice();
            BigDecimal tr1 = today.getHighPrice().subtract(today.getLowPrice());
            BigDecimal tr2 = today.getHighPrice().subtract(previousClose).abs();
            BigDecimal tr3 = today.getLowPrice().subtract(previousClose).abs();
            sum = sum.add(tr1.max(tr2).max(tr3));
        }
        return NumberUtil.scale(sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP));
    }

    private BigDecimal volumeMa(List<DailyKlineEntity> klines, int index, int period) {
        if (index + 1 < period) {
            return null;
        }
        BigDecimal sum = BigDecimal.ZERO;
        for (int i = index - period + 1; i <= index; i++) {
            BigDecimal volume = klines.get(i).getVolume();
            if (volume == null) {
                return null;
            }
            sum = sum.add(volume);
        }
        return NumberUtil.scale(sum.divide(BigDecimal.valueOf(period), 8, RoundingMode.HALF_UP));
    }

    private BigDecimal volumeRatio(BigDecimal volume, BigDecimal volumeMa) {
        if (volume == null || volumeMa == null || volumeMa.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return NumberUtil.scale(volume.divide(volumeMa, 8, RoundingMode.HALF_UP));
    }

    private BigDecimal fiveDayReturn(List<DailyKlineEntity> klines, int index) {
        if (index < 5) {
            return null;
        }
        BigDecimal previous = klines.get(index - 5).getClosePrice();
        if (previous.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return NumberUtil.scale(klines.get(index).getClosePrice()
                .subtract(previous)
                .divide(previous, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
    }

    private BigDecimal distanceToMa(BigDecimal close, BigDecimal ma) {
        if (close == null || ma == null || ma.compareTo(BigDecimal.ZERO) == 0) {
            return null;
        }
        return NumberUtil.scale(close.subtract(ma)
                .divide(ma, 8, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100)));
    }
}
