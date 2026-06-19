package com.alexjoker.quant.llm.mapper;

import com.alexjoker.quant.llm.entity.AiAnalysisSnapshotEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AiAnalysisSnapshotMapper extends BaseMapper<AiAnalysisSnapshotEntity> {
    @Insert("""
            INSERT INTO ai_analysis_snapshot(
                symbol_code, analysis_date, asset_name, market, asset_type,
                sentiment_score, risk_score, risk_level, market_state, action_constraint,
                summary, positive_factors, negative_factors, risk_factors, contrarian_view,
                raw_report, source_system, call_status, error_message, updated_at
            )
            VALUES(
                #{symbolCode}, #{analysisDate}, #{assetName}, #{market}, #{assetType},
                #{sentimentScore}, #{riskScore}, #{riskLevel}, #{marketState}, #{actionConstraint},
                #{summary}, #{positiveFactors}, #{negativeFactors}, #{riskFactors}, #{contrarianView},
                #{rawReport}, #{sourceSystem}, #{callStatus}, #{errorMessage}, CURRENT_TIMESTAMP
            )
            ON DUPLICATE KEY UPDATE
                asset_name = VALUES(asset_name),
                market = VALUES(market),
                asset_type = VALUES(asset_type),
                sentiment_score = VALUES(sentiment_score),
                risk_score = VALUES(risk_score),
                risk_level = VALUES(risk_level),
                market_state = VALUES(market_state),
                action_constraint = VALUES(action_constraint),
                summary = VALUES(summary),
                positive_factors = VALUES(positive_factors),
                negative_factors = VALUES(negative_factors),
                risk_factors = VALUES(risk_factors),
                contrarian_view = VALUES(contrarian_view),
                raw_report = VALUES(raw_report),
                source_system = VALUES(source_system),
                call_status = VALUES(call_status),
                error_message = VALUES(error_message),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(AiAnalysisSnapshotEntity entity);
}
