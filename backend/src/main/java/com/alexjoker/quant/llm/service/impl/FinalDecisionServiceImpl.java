package com.alexjoker.quant.llm.service.impl;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.journal.service.TradeJournalService;
import com.alexjoker.quant.llm.dto.FinalTradeDecisionDTO;
import com.alexjoker.quant.llm.entity.AiAnalysisSnapshotEntity;
import com.alexjoker.quant.llm.entity.FinalTradeDecisionEntity;
import com.alexjoker.quant.llm.mapper.AiAnalysisSnapshotMapper;
import com.alexjoker.quant.llm.mapper.FinalTradeDecisionMapper;
import com.alexjoker.quant.llm.service.FinalDecisionService;
import com.alexjoker.quant.signal.entity.TechnicalSignalEntity;
import com.alexjoker.quant.signal.mapper.TechnicalSignalMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FinalDecisionServiceImpl implements FinalDecisionService {
    private final FinalTradeDecisionMapper finalTradeDecisionMapper;
    private final TechnicalSignalMapper technicalSignalMapper;
    private final AiAnalysisSnapshotMapper aiAnalysisSnapshotMapper;
    private final TradeJournalService tradeJournalService;

    @Override
    @Transactional
    public FinalTradeDecisionDTO generate(TechnicalSignalEntity signal, AiAnalysisSnapshotEntity aiAnalysis) {
        if (signal == null) {
            throw new BusinessException("缺少技术信号，无法生成最终决策");
        }
        FinalTradeDecisionEntity decision = new FinalTradeDecisionEntity();
        decision.setSymbolCode(signal.getSymbolCode());
        decision.setDecisionDate(signal.getTradeDate());
        decision.setTechnicalSignal(signal.getSignalType());
        decision.setTechnicalScore(signal.getTechnicalScore());
        if (aiAnalysis != null) {
            decision.setAiSentimentScore(aiAnalysis.getSentimentScore());
            decision.setAiRiskScore(aiAnalysis.getRiskScore());
            decision.setAiRiskLevel(defaultText(aiAnalysis.getRiskLevel(), "UNKNOWN"));
        }

        applyRules(decision, signal, aiAnalysis);
        finalTradeDecisionMapper.upsert(decision);
        FinalTradeDecisionEntity saved = findBySymbolDate(signal.getSymbolCode(), signal.getTradeDate());
        tradeJournalService.recordDecision(saved);
        return toDto(saved);
    }

    @Override
    @Transactional
    public FinalTradeDecisionDTO generate(String symbolCode, LocalDate decisionDate) {
        TechnicalSignalEntity signal = findMainSignal(symbolCode, decisionDate);
        if (signal == null) {
            throw new BusinessException("没有找到技术信号: " + symbolCode + " " + decisionDate);
        }
        AiAnalysisSnapshotEntity ai = aiAnalysisSnapshotMapper.selectOne(new LambdaQueryWrapper<AiAnalysisSnapshotEntity>()
                .eq(AiAnalysisSnapshotEntity::getSymbolCode, normalize(symbolCode))
                .eq(AiAnalysisSnapshotEntity::getAnalysisDate, decisionDate)
                .last("LIMIT 1"));
        return generate(signal, ai);
    }

    @Override
    public FinalTradeDecisionDTO getLatestForSymbol(String symbolCode) {
        FinalTradeDecisionEntity entity = finalTradeDecisionMapper.selectOne(new LambdaQueryWrapper<FinalTradeDecisionEntity>()
                .eq(FinalTradeDecisionEntity::getSymbolCode, normalize(symbolCode))
                .orderByDesc(FinalTradeDecisionEntity::getDecisionDate)
                .last("LIMIT 1"));
        return toDto(entity);
    }

    @Override
    public List<FinalTradeDecisionDTO> listLatest() {
        List<FinalTradeDecisionEntity> all = finalTradeDecisionMapper.selectList(new LambdaQueryWrapper<FinalTradeDecisionEntity>()
                .orderByDesc(FinalTradeDecisionEntity::getDecisionDate));
        Map<String, FinalTradeDecisionEntity> latestBySymbol = new LinkedHashMap<>();
        for (FinalTradeDecisionEntity item : all) {
            latestBySymbol.putIfAbsent(item.getSymbolCode(), item);
        }
        return latestBySymbol.values().stream().map(this::toDto).toList();
    }

    @Override
    public List<FinalTradeDecisionDTO> listHistory(String symbolCode) {
        return finalTradeDecisionMapper.selectList(new LambdaQueryWrapper<FinalTradeDecisionEntity>()
                        .eq(FinalTradeDecisionEntity::getSymbolCode, normalize(symbolCode))
                        .orderByDesc(FinalTradeDecisionEntity::getDecisionDate))
                .stream().map(this::toDto).toList();
    }

    @Override
    public FinalTradeDecisionDTO toDto(FinalTradeDecisionEntity entity) {
        if (entity == null) {
            return null;
        }
        FinalTradeDecisionDTO dto = new FinalTradeDecisionDTO();
        dto.setId(entity.getId());
        dto.setSymbolCode(entity.getSymbolCode());
        dto.setDecisionDate(entity.getDecisionDate());
        dto.setTechnicalSignal(entity.getTechnicalSignal());
        dto.setTechnicalScore(entity.getTechnicalScore());
        dto.setAiSentimentScore(entity.getAiSentimentScore());
        dto.setAiRiskScore(entity.getAiRiskScore());
        dto.setAiRiskLevel(entity.getAiRiskLevel());
        dto.setFinalAction(entity.getFinalAction());
        dto.setPositionPolicy(entity.getPositionPolicy());
        dto.setDecisionReason(entity.getDecisionReason());
        dto.setRejectReason(entity.getRejectReason());
        return dto;
    }

    private void applyRules(FinalTradeDecisionEntity decision, TechnicalSignalEntity signal, AiAnalysisSnapshotEntity aiAnalysis) {
        if (!"BUY_CANDIDATE".equals(signal.getSignalType())) {
            decision.setFinalAction("NO_ACTION");
            decision.setPositionPolicy("WATCH");
            decision.setDecisionReason("K线信号不是 BUY_CANDIDATE，不进入 AI 风险过滤和交易候选。");
            decision.setRejectReason(null);
            return;
        }
        if (aiAnalysis == null) {
            decision.setFinalAction("WATCH_ONLY");
            decision.setPositionPolicy("WATCH");
            decision.setDecisionReason("K线信号成立，但缺少 AI 风险分析，暂时只观察。");
            decision.setRejectReason(null);
            return;
        }
        if ("FAILED".equalsIgnoreCase(aiAnalysis.getCallStatus())) {
            decision.setFinalAction("WATCH_ONLY");
            decision.setPositionPolicy("WATCH");
            decision.setDecisionReason("LLM 分析调用失败，出于保守原则仅观察。");
            decision.setRejectReason(aiAnalysis.getErrorMessage());
            return;
        }
        Integer riskScore = aiAnalysis.getRiskScore();
        if (riskScore == null) {
            decision.setFinalAction("WATCH_ONLY");
            decision.setPositionPolicy("WATCH");
            decision.setDecisionReason("K线信号成立，但 AI 风险分缺失，暂时只观察。");
            decision.setRejectReason(null);
            return;
        }
        if (riskScore >= 70) {
            decision.setFinalAction("BLOCK");
            decision.setPositionPolicy("ZERO");
            decision.setDecisionReason(null);
            decision.setRejectReason("K线信号成立，但 AI 风险评分过高，禁止交易。");
        } else if (riskScore >= 40) {
            decision.setFinalAction("WATCH_ONLY");
            decision.setPositionPolicy("HALF");
            decision.setDecisionReason("K线信号成立，但 AI 风险中等，建议继续观察或仅进行半仓模拟。");
            decision.setRejectReason(null);
        } else {
            decision.setFinalAction("ALLOW_SIMULATION");
            decision.setPositionPolicy("NORMAL");
            decision.setDecisionReason("K线信号成立，AI 风险较低，允许进入模拟交易。");
            decision.setRejectReason(null);
        }
    }

    private TechnicalSignalEntity findMainSignal(String symbolCode, LocalDate date) {
        List<TechnicalSignalEntity> signals = technicalSignalMapper.selectList(new LambdaQueryWrapper<TechnicalSignalEntity>()
                .eq(TechnicalSignalEntity::getSymbolCode, normalize(symbolCode))
                .eq(TechnicalSignalEntity::getTradeDate, date));
        if (signals.isEmpty()) {
            return null;
        }
        return signals.stream()
                .min(Comparator.comparingInt(signal -> priority(signal.getSignalType())))
                .orElse(null);
    }

    private FinalTradeDecisionEntity findBySymbolDate(String symbolCode, LocalDate date) {
        return finalTradeDecisionMapper.selectOne(new LambdaQueryWrapper<FinalTradeDecisionEntity>()
                .eq(FinalTradeDecisionEntity::getSymbolCode, normalize(symbolCode))
                .eq(FinalTradeDecisionEntity::getDecisionDate, date)
                .last("LIMIT 1"));
    }

    private int priority(String signalType) {
        return switch (signalType) {
            case "AVOID" -> 1;
            case "BUY_CANDIDATE" -> 2;
            case "WATCH" -> 3;
            case "SELL_WARNING" -> 4;
            case "NEUTRAL" -> 5;
            default -> 99;
        };
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    private String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
