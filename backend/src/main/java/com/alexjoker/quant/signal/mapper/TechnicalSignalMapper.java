package com.alexjoker.quant.signal.mapper;

import com.alexjoker.quant.signal.entity.TechnicalSignalEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TechnicalSignalMapper extends BaseMapper<TechnicalSignalEntity> {
    @Insert("""
            INSERT INTO technical_signal(
                symbol_code, trade_date, signal_type, signal_level, trend_state, technical_score,
                entry_reference, stop_loss_reference, take_profit_reference, reason, risk_note, updated_at
            )
            VALUES(
                #{symbolCode}, #{tradeDate}, #{signalType}, #{signalLevel}, #{trendState}, #{technicalScore},
                #{entryReference}, #{stopLossReference}, #{takeProfitReference}, #{reason}, #{riskNote}, CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE
                signal_level = VALUES(signal_level),
                trend_state = VALUES(trend_state),
                technical_score = VALUES(technical_score),
                entry_reference = VALUES(entry_reference),
                stop_loss_reference = VALUES(stop_loss_reference),
                take_profit_reference = VALUES(take_profit_reference),
                reason = VALUES(reason),
                risk_note = VALUES(risk_note),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(TechnicalSignalEntity entity);
}
