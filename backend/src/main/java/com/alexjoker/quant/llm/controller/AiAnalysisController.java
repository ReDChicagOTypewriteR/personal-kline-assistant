package com.alexjoker.quant.llm.controller;

import com.alexjoker.quant.common.response.ApiResponse;
import com.alexjoker.quant.llm.dto.AiAnalysisSnapshotDTO;
import com.alexjoker.quant.llm.dto.AiAnalyzeResultDTO;
import com.alexjoker.quant.llm.dto.AiBatchAnalyzeResultDTO;
import com.alexjoker.quant.llm.service.AiAnalysisService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/ai-analysis")
@RequiredArgsConstructor
public class AiAnalysisController {
    private final AiAnalysisService aiAnalysisService;

    @PostMapping("/analyze")
    public ApiResponse<AiAnalyzeResultDTO> analyze(
            @RequestParam String symbolCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate) {
        return ApiResponse.ok(aiAnalysisService.analyze(symbolCode, tradeDate));
    }

    @PostMapping("/analyze-buy-candidates")
    public ApiResponse<AiBatchAnalyzeResultDTO> analyzeBuyCandidates(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate) {
        return ApiResponse.ok(aiAnalysisService.analyzeBuyCandidates(tradeDate));
    }

    @GetMapping("/snapshot")
    public ApiResponse<AiAnalysisSnapshotDTO> snapshot(
            @RequestParam String symbolCode,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate analysisDate) {
        return ApiResponse.ok(aiAnalysisService.getSnapshot(symbolCode, analysisDate));
    }
}
