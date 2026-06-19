package com.alexjoker.quant.llm.service.impl;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.indicator.mapper.IndicatorSnapshotMapper;
import com.alexjoker.quant.llm.client.LlmAnalysisClient;
import com.alexjoker.quant.llm.client.LlmAnalysisException;
import com.alexjoker.quant.llm.config.DailyStockAnalysisProperties;
import com.alexjoker.quant.llm.dto.AiAnalysisSnapshotDTO;
import com.alexjoker.quant.llm.dto.AiAnalyzeResultDTO;
import com.alexjoker.quant.llm.dto.AiBatchAnalyzeResultDTO;
import com.alexjoker.quant.llm.dto.LlmRiskAnalysisRequest;
import com.alexjoker.quant.llm.dto.LlmRiskAnalysisResponse;
import com.alexjoker.quant.llm.entity.AiAnalysisSnapshotEntity;
import com.alexjoker.quant.llm.mapper.AiAnalysisSnapshotMapper;
import com.alexjoker.quant.llm.service.AiAnalysisService;
import com.alexjoker.quant.llm.service.FinalDecisionService;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.marketdata.mapper.DailyKlineMapper;
import com.alexjoker.quant.signal.entity.TechnicalSignalEntity;
import com.alexjoker.quant.signal.mapper.TechnicalSignalMapper;
import com.alexjoker.quant.symbol.dto.SymbolDTO;
import com.alexjoker.quant.symbol.service.SymbolService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {
    private final AiAnalysisSnapshotMapper aiAnalysisSnapshotMapper;
    private final TechnicalSignalMapper technicalSignalMapper;
    private final DailyKlineMapper dailyKlineMapper;
    private final IndicatorSnapshotMapper indicatorSnapshotMapper;
    private final SymbolService symbolService;
    private final LlmAnalysisClient llmAnalysisClient;
    private final DailyStockAnalysisProperties properties;
    private final FinalDecisionService finalDecisionService;

    @Override
    @Transactional
    public AiAnalyzeResultDTO analyze(String symbolCode, LocalDate tradeDate) {
        String code = normalize(symbolCode);
        TechnicalSignalEntity signal = findMainSignal(code, tradeDate);
        if (signal == null) {
            throw new BusinessException("没有找到技术信号: " + code + " " + tradeDate);
        }
        AiAnalyzeResultDTO result = new AiAnalyzeResultDTO();
        if (!"BUY_CANDIDATE".equals(signal.getSignalType())) {
            result.setCached(false);
            result.setMessage("非 BUY_CANDIDATE，不需要调用 LLM。");
            result.setFinalDecision(finalDecisionService.generate(signal, null));
            return result;
        }

        AiAnalysisSnapshotEntity cached = getSnapshotEntity(code, tradeDate);
        if (properties.isCacheEnabled() && cached != null) {
            result.setAiAnalysis(toDto(cached));
            result.setFinalDecision(finalDecisionService.generate(signal, cached));
            result.setCached(true);
            result.setMessage("已读取缓存的 AI 分析结果。");
            return result;
        }

        AiAnalysisSnapshotEntity snapshot;
        try {
            LlmRiskAnalysisResponse response = llmAnalysisClient.analyzeRisk(buildRequest(signal));
            snapshot = buildSuccessSnapshot(signal, response);
        } catch (LlmAnalysisException ex) {
            snapshot = buildFailedSnapshot(signal, ex.getMessage());
        } catch (Exception ex) {
            snapshot = buildFailedSnapshot(signal, "LLM 分析调用失败: " + ex.getMessage());
        }

        aiAnalysisSnapshotMapper.upsert(snapshot);
        AiAnalysisSnapshotEntity saved = getSnapshotEntity(code, tradeDate);
        result.setAiAnalysis(toDto(saved));
        result.setFinalDecision(finalDecisionService.generate(signal, saved));
        result.setCached(false);
        result.setMessage("FAILED".equalsIgnoreCase(saved.getCallStatus()) ? "LLM 分析调用失败，已按保守规则处理。" : "AI 风险分析已完成。");
        return result;
    }

    @Override
    @Transactional
    public AiBatchAnalyzeResultDTO analyzeBuyCandidates(LocalDate tradeDate) {
        AiBatchAnalyzeResultDTO batch = new AiBatchAnalyzeResultDTO();
        batch.setTradeDate(tradeDate);
        List<TechnicalSignalEntity> candidates = technicalSignalMapper.selectList(new LambdaQueryWrapper<TechnicalSignalEntity>()
                .eq(TechnicalSignalEntity::getTradeDate, tradeDate)
                .eq(TechnicalSignalEntity::getSignalType, "BUY_CANDIDATE")
                .orderByAsc(TechnicalSignalEntity::getSymbolCode));
        for (TechnicalSignalEntity candidate : candidates) {
            AiAnalyzeResultDTO item = analyze(candidate.getSymbolCode(), tradeDate);
            batch.getResults().add(item);
            if (item.isCached()) {
                batch.setSkippedCount(batch.getSkippedCount() + 1);
            } else if (item.getAiAnalysis() != null && "FAILED".equalsIgnoreCase(item.getAiAnalysis().getCallStatus())) {
                batch.setFailedCount(batch.getFailedCount() + 1);
            } else {
                batch.setSuccessCount(batch.getSuccessCount() + 1);
            }
        }
        return batch;
    }

    @Override
    public AiAnalysisSnapshotDTO getSnapshot(String symbolCode, LocalDate analysisDate) {
        return toDto(getSnapshotEntity(symbolCode, analysisDate));
    }

    @Override
    public AiAnalysisSnapshotEntity getSnapshotEntity(String symbolCode, LocalDate analysisDate) {
        return aiAnalysisSnapshotMapper.selectOne(new LambdaQueryWrapper<AiAnalysisSnapshotEntity>()
                .eq(AiAnalysisSnapshotEntity::getSymbolCode, normalize(symbolCode))
                .eq(AiAnalysisSnapshotEntity::getAnalysisDate, analysisDate)
                .last("LIMIT 1"));
    }

    @Override
    public AiAnalysisSnapshotDTO toDto(AiAnalysisSnapshotEntity entity) {
        if (entity == null) {
            return null;
        }
        AiAnalysisSnapshotDTO dto = new AiAnalysisSnapshotDTO();
        dto.setId(entity.getId());
        dto.setSymbolCode(entity.getSymbolCode());
        dto.setAnalysisDate(entity.getAnalysisDate());
        dto.setAssetName(entity.getAssetName());
        dto.setMarket(entity.getMarket());
        dto.setAssetType(entity.getAssetType());
        dto.setSentimentScore(entity.getSentimentScore());
        dto.setRiskScore(entity.getRiskScore());
        dto.setRiskLevel(entity.getRiskLevel());
        dto.setMarketState(entity.getMarketState());
        dto.setActionConstraint(entity.getActionConstraint());
        dto.setSummary(entity.getSummary());
        dto.setPositiveFactors(entity.getPositiveFactors());
        dto.setNegativeFactors(entity.getNegativeFactors());
        dto.setRiskFactors(entity.getRiskFactors());
        dto.setContrarianView(entity.getContrarianView());
        dto.setRawReport(entity.getRawReport());
        dto.setSourceSystem(entity.getSourceSystem());
        dto.setCallStatus(entity.getCallStatus());
        dto.setErrorMessage(entity.getErrorMessage());
        return dto;
    }

    private LlmRiskAnalysisRequest buildRequest(TechnicalSignalEntity signal) {
        DailyKlineEntity kline = findKline(signal.getSymbolCode(), signal.getTradeDate());
        IndicatorSnapshotEntity indicator = findIndicator(signal.getSymbolCode(), signal.getTradeDate());
        SymbolDTO symbol = symbolService.getByCode(signal.getSymbolCode());

        LlmRiskAnalysisRequest request = new LlmRiskAnalysisRequest();
        request.setSymbolCode(signal.getSymbolCode());
        request.setAssetName(defaultText(symbol.getSymbolName(), signal.getSymbolCode()));
        request.setMarket(defaultText(symbol.getMarket(), "UNKNOWN"));
        request.setAssetType(defaultText(symbol.getAssetType(), "UNKNOWN"));
        request.setTrackingTarget(symbol.getAssetType() != null && symbol.getAssetType().equalsIgnoreCase("ETF") ? symbol.getSymbolName() : null);
        request.setTradeDate(signal.getTradeDate());
        request.setTechnicalSignal(signal.getSignalType());
        request.setTechnicalScore(signal.getTechnicalScore());
        if (kline != null) {
            request.setClosePrice(kline.getClosePrice());
        }
        if (indicator != null) {
            request.setMa20(indicator.getMa20());
            request.setRsi14(indicator.getRsi14());
            request.setAtr14(indicator.getAtr14());
        }
        request.setAnalysisScope(List.of(
                "A股大盘",
                "沪深300指数",
                "权重板块",
                "政策新闻",
                "宏观经济",
                "市场情绪",
                "成交量变化",
                "外围市场"
        ));
        return request;
    }

    private AiAnalysisSnapshotEntity buildSuccessSnapshot(TechnicalSignalEntity signal, LlmRiskAnalysisResponse response) {
        SymbolDTO symbol = symbolService.getByCode(signal.getSymbolCode());
        AiAnalysisSnapshotEntity entity = new AiAnalysisSnapshotEntity();
        entity.setSymbolCode(defaultText(response.getSymbolCode(), signal.getSymbolCode()));
        entity.setAnalysisDate(response.getAnalysisDate() == null ? signal.getTradeDate() : response.getAnalysisDate());
        entity.setAssetName(defaultText(symbol.getSymbolName(), signal.getSymbolCode()));
        entity.setMarket(defaultText(symbol.getMarket(), "UNKNOWN"));
        entity.setAssetType(defaultText(symbol.getAssetType(), "UNKNOWN"));
        entity.setSentimentScore(response.getSentimentScore());
        entity.setRiskScore(response.getRiskScore());
        entity.setRiskLevel(defaultText(response.getRiskLevel(), "UNKNOWN"));
        entity.setMarketState(defaultText(response.getMarketState(), "UNKNOWN"));
        entity.setActionConstraint(defaultText(response.getActionConstraint(), "UNKNOWN"));
        entity.setSummary(response.getSummary());
        entity.setPositiveFactors(joinLines(response.getPositiveFactors()));
        entity.setNegativeFactors(joinLines(response.getNegativeFactors()));
        entity.setRiskFactors(joinLines(response.getRiskFactors()));
        entity.setContrarianView(response.getContrarianView());
        entity.setRawReport(response.getRawReport());
        entity.setSourceSystem("daily_stock_analysis");
        entity.setCallStatus("SUCCESS");
        entity.setErrorMessage(null);
        return entity;
    }

    private AiAnalysisSnapshotEntity buildFailedSnapshot(TechnicalSignalEntity signal, String message) {
        SymbolDTO symbol = symbolService.getByCode(signal.getSymbolCode());
        AiAnalysisSnapshotEntity entity = new AiAnalysisSnapshotEntity();
        entity.setSymbolCode(signal.getSymbolCode());
        entity.setAnalysisDate(signal.getTradeDate());
        entity.setAssetName(defaultText(symbol.getSymbolName(), signal.getSymbolCode()));
        entity.setMarket(defaultText(symbol.getMarket(), "UNKNOWN"));
        entity.setAssetType(defaultText(symbol.getAssetType(), "UNKNOWN"));
        entity.setRiskLevel("UNKNOWN");
        entity.setMarketState("UNKNOWN");
        entity.setActionConstraint("UNKNOWN");
        entity.setSourceSystem("daily_stock_analysis");
        entity.setCallStatus("FAILED");
        entity.setErrorMessage(defaultText(message, "LLM 分析服务不可用"));
        return entity;
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

    private DailyKlineEntity findKline(String symbolCode, LocalDate date) {
        return dailyKlineMapper.selectOne(new LambdaQueryWrapper<DailyKlineEntity>()
                .eq(DailyKlineEntity::getSymbolCode, normalize(symbolCode))
                .eq(DailyKlineEntity::getTradeDate, date)
                .last("LIMIT 1"));
    }

    private IndicatorSnapshotEntity findIndicator(String symbolCode, LocalDate date) {
        return indicatorSnapshotMapper.selectOne(new LambdaQueryWrapper<IndicatorSnapshotEntity>()
                .eq(IndicatorSnapshotEntity::getSymbolCode, normalize(symbolCode))
                .eq(IndicatorSnapshotEntity::getTradeDate, date)
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

    private String joinLines(List<String> values) {
        if (values == null || values.isEmpty()) {
            return null;
        }
        return String.join("\n", values.stream().filter(value -> value != null && !value.isBlank()).toList());
    }
}
