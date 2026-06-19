package com.alexjoker.quant.indicator.mapper;

import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface IndicatorSnapshotMapper extends BaseMapper<IndicatorSnapshotEntity> {
    @Insert("""
            INSERT INTO indicator_snapshot(
                symbol_code, trade_date, ma5, ma10, ma20, ma60, rsi14, atr14,
                volume_ma20, volume_ratio, five_day_return, distance_to_ma20, updated_at
            )
            VALUES(
                #{symbolCode}, #{tradeDate}, #{ma5}, #{ma10}, #{ma20}, #{ma60}, #{rsi14}, #{atr14},
                #{volumeMa20}, #{volumeRatio}, #{fiveDayReturn}, #{distanceToMa20}, CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE
                ma5 = VALUES(ma5),
                ma10 = VALUES(ma10),
                ma20 = VALUES(ma20),
                ma60 = VALUES(ma60),
                rsi14 = VALUES(rsi14),
                atr14 = VALUES(atr14),
                volume_ma20 = VALUES(volume_ma20),
                volume_ratio = VALUES(volume_ratio),
                five_day_return = VALUES(five_day_return),
                distance_to_ma20 = VALUES(distance_to_ma20),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(IndicatorSnapshotEntity entity);
}
