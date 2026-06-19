package com.alexjoker.quant.dashboard.service.impl;

import com.alexjoker.quant.dashboard.dto.DashboardDTO;
import com.alexjoker.quant.dashboard.dto.SymbolTechnicalDetailDTO;
import com.alexjoker.quant.dashboard.service.DashboardService;
import com.alexjoker.quant.indicator.service.IndicatorService;
import com.alexjoker.quant.llm.dto.FinalTradeDecisionDTO;
import com.alexjoker.quant.llm.service.AiAnalysisService;
import com.alexjoker.quant.llm.service.FinalDecisionService;
import com.alexjoker.quant.marketdata.service.DailyKlineService;
import com.alexjoker.quant.signal.dto.TechnicalSignalDTO;
import com.alexjoker.quant.signal.service.TechnicalSignalService;
import com.alexjoker.quant.symbol.service.SymbolService;
import com.alexjoker.quant.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {
    private final SymbolService symbolService;
    private final WatchlistService watchlistService;
    private final DailyKlineService dailyKlineService;
    private final IndicatorService indicatorService;
    private final TechnicalSignalService technicalSignalService;
    private final AiAnalysisService aiAnalysisService;
    private final FinalDecisionService finalDecisionService;

    @Override
    public DashboardDTO getDashboard() {
        List<TechnicalSignalDTO> latestSignals = technicalSignalService.listLatestSignals();
        latestSignals.forEach(this::attachFinalDecisionSummary);
        DashboardDTO dto = new DashboardDTO();
        dto.setTotalSymbols(symbolService.listAll().size());
        dto.setTotalWatchlist(watchlistService.countEnabled());
        dto.setBuyCandidateCount(count(latestSignals, "BUY_CANDIDATE"));
        dto.setWatchCount(count(latestSignals, "WATCH"));
        dto.setAvoidCount(count(latestSignals, "AVOID"));
        dto.setLatestSignals(latestSignals);
        return dto;
    }

    @Override
    public SymbolTechnicalDetailDTO getSymbolDetail(String symbolCode, LocalDate start, LocalDate end) {
        SymbolTechnicalDetailDTO dto = new SymbolTechnicalDetailDTO();
        var symbol = symbolService.getOrCreateCnSymbol(symbolCode);
        dto.setSymbol(symbol);
        dto.setKlines(dailyKlineService.listDaily(symbol.getSymbolCode(), start, end));
        dto.setIndicators(indicatorService.listIndicators(symbol.getSymbolCode(), start, end));
        dto.setLatestSignal(technicalSignalService.listLatestSignals().stream()
                .filter(signal -> signal.getSymbolCode().equalsIgnoreCase(symbol.getSymbolCode()))
                .findFirst()
                .orElse(null));
        if (dto.getLatestSignal() != null) {
            attachFinalDecisionSummary(dto.getLatestSignal());
            dto.setAiAnalysis(aiAnalysisService.getSnapshot(symbol.getSymbolCode(), dto.getLatestSignal().getTradeDate()));
            dto.setFinalDecision(finalDecisionService.listHistory(symbol.getSymbolCode()).stream()
                    .filter(decision -> dto.getLatestSignal().getTradeDate().equals(decision.getDecisionDate()))
                    .findFirst()
                    .orElse(null));
        }
        return dto;
    }

    private long count(List<TechnicalSignalDTO> signals, String type) {
        return signals.stream().filter(signal -> type.equals(signal.getSignalType())).count();
    }

    private void attachFinalDecisionSummary(TechnicalSignalDTO signal) {
        if (signal == null) {
            return;
        }
        FinalTradeDecisionDTO decision = finalDecisionService.getLatestForSymbol(signal.getSymbolCode());
        if (decision == null || !signal.getTradeDate().equals(decision.getDecisionDate())) {
            return;
        }
        signal.setAiSentimentScore(decision.getAiSentimentScore());
        signal.setAiRiskScore(decision.getAiRiskScore());
        signal.setAiRiskLevel(decision.getAiRiskLevel());
        signal.setFinalAction(decision.getFinalAction());
        signal.setPositionPolicy(decision.getPositionPolicy());
    }
}
