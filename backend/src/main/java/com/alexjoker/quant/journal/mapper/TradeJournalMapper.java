package com.alexjoker.quant.journal.mapper;

import com.alexjoker.quant.journal.entity.TradeJournalEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface TradeJournalMapper extends BaseMapper<TradeJournalEntity> {
    @Insert("""
            INSERT INTO trade_journal(
                symbol_code, trade_date, signal_type, signal_level, trend_state,
                close_price, ma5, ma10, ma20, ma60, rsi14, atr14, volume_ratio,
                technical_score, signal_reason, risk_note, updated_at
            )
            VALUES(
                #{symbolCode}, #{tradeDate}, #{signalType}, #{signalLevel}, #{trendState},
                #{closePrice}, #{ma5}, #{ma10}, #{ma20}, #{ma60}, #{rsi14}, #{atr14}, #{volumeRatio},
                #{technicalScore}, #{signalReason}, #{riskNote}, CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE
                signal_level = VALUES(signal_level),
                trend_state = VALUES(trend_state),
                close_price = VALUES(close_price),
                ma5 = VALUES(ma5),
                ma10 = VALUES(ma10),
                ma20 = VALUES(ma20),
                ma60 = VALUES(ma60),
                rsi14 = VALUES(rsi14),
                atr14 = VALUES(atr14),
                volume_ratio = VALUES(volume_ratio),
                technical_score = VALUES(technical_score),
                signal_reason = VALUES(signal_reason),
                risk_note = VALUES(risk_note),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsertSignalSnapshot(TradeJournalEntity entity);

    @Insert("""
            INSERT INTO trade_journal(
                symbol_code, trade_date, signal_type, technical_score,
                ai_risk_score, ai_risk_level, final_action, position_policy,
                decision_reason, reject_reason, updated_at
            )
            VALUES(
                #{symbolCode}, #{tradeDate}, #{signalType}, #{technicalScore},
                #{aiRiskScore}, #{aiRiskLevel}, #{finalAction}, #{positionPolicy},
                #{decisionReason}, #{rejectReason}, CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE
                technical_score = COALESCE(VALUES(technical_score), technical_score),
                ai_risk_score = VALUES(ai_risk_score),
                ai_risk_level = VALUES(ai_risk_level),
                final_action = VALUES(final_action),
                position_policy = VALUES(position_policy),
                decision_reason = VALUES(decision_reason),
                reject_reason = VALUES(reject_reason),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsertDecisionSnapshot(TradeJournalEntity entity);
}
