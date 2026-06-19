package com.alexjoker.quant.llm.mapper;

import com.alexjoker.quant.llm.entity.FinalTradeDecisionEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface FinalTradeDecisionMapper extends BaseMapper<FinalTradeDecisionEntity> {
    @Insert("""
            INSERT INTO final_trade_decision(
                symbol_code, decision_date, technical_signal, technical_score,
                ai_sentiment_score, ai_risk_score, ai_risk_level,
                final_action, position_policy, decision_reason, reject_reason, updated_at
            )
            VALUES(
                #{symbolCode}, #{decisionDate}, #{technicalSignal}, #{technicalScore},
                #{aiSentimentScore}, #{aiRiskScore}, #{aiRiskLevel},
                #{finalAction}, #{positionPolicy}, #{decisionReason}, #{rejectReason}, CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE
                technical_signal = VALUES(technical_signal),
                technical_score = VALUES(technical_score),
                ai_sentiment_score = VALUES(ai_sentiment_score),
                ai_risk_score = VALUES(ai_risk_score),
                ai_risk_level = VALUES(ai_risk_level),
                final_action = VALUES(final_action),
                position_policy = VALUES(position_policy),
                decision_reason = VALUES(decision_reason),
                reject_reason = VALUES(reject_reason),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(FinalTradeDecisionEntity entity);
}
