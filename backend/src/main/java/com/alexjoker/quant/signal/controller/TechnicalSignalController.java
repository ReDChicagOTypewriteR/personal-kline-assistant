package com.alexjoker.quant.signal.controller;

import com.alexjoker.quant.common.response.ApiResponse;
import com.alexjoker.quant.signal.dto.TechnicalSignalDTO;
import com.alexjoker.quant.signal.service.TechnicalSignalService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/signals")
@RequiredArgsConstructor
public class TechnicalSignalController {
    private final TechnicalSignalService technicalSignalService;

    @PostMapping("/generate")
    public ApiResponse<List<TechnicalSignalDTO>> generate(@RequestParam String symbolCode) {
        return ApiResponse.ok(technicalSignalService.generateSignalForSymbol(symbolCode));
    }

    @PostMapping("/generate-all")
    public ApiResponse<Map<String, Integer>> generateAll() {
        return ApiResponse.ok(Map.of("count", technicalSignalService.generateSignalsForAllEnabledSymbols()));
    }

    @GetMapping("/latest")
    public ApiResponse<List<TechnicalSignalDTO>> latest() {
        return ApiResponse.ok(technicalSignalService.listLatestSignals());
    }

    @GetMapping("/history")
    public ApiResponse<List<TechnicalSignalDTO>> history(@RequestParam String symbolCode) {
        return ApiResponse.ok(technicalSignalService.listSignalHistory(symbolCode));
    }

    @DeleteMapping
    public ApiResponse<Map<String, Integer>> delete(@RequestParam String symbolCode,
                                                    @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate tradeDate) {
        return ApiResponse.ok(Map.of("count", technicalSignalService.deleteSignals(symbolCode, tradeDate)));
    }
}
