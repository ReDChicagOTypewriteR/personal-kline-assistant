package com.alexjoker.quant.marketdata.mapper;

import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface DailyKlineMapper extends BaseMapper<DailyKlineEntity> {
    @Insert("""
            INSERT INTO daily_kline(symbol_code, trade_date, open_price, high_price, low_price, close_price, volume, amount, updated_at)
            VALUES(#{symbolCode}, #{tradeDate}, #{openPrice}, #{highPrice}, #{lowPrice}, #{closePrice}, #{volume}, #{amount}, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                open_price = VALUES(open_price),
                high_price = VALUES(high_price),
                low_price = VALUES(low_price),
                close_price = VALUES(close_price),
                volume = VALUES(volume),
                amount = VALUES(amount),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(DailyKlineEntity entity);
}
