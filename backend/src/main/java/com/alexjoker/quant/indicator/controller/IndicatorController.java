package com.alexjoker.quant.indicator.controller;

import com.alexjoker.quant.common.response.ApiResponse;
import com.alexjoker.quant.indicator.dto.IndicatorSnapshotDTO;
import com.alexjoker.quant.indicator.service.IndicatorService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/indicators")
@RequiredArgsConstructor
public class IndicatorController {
    private final IndicatorService indicatorService;

    @PostMapping("/calculate")
    public ApiResponse<Map<String, Integer>> calculate(@RequestParam String symbolCode) {
        return ApiResponse.ok(Map.of("count", indicatorService.calculateForSymbol(symbolCode)));
    }

    @PostMapping("/calculate-all")
    public ApiResponse<Map<String, Integer>> calculateAll() {
        return ApiResponse.ok(Map.of("count", indicatorService.calculateForAllEnabledSymbols()));
    }

    @GetMapping
    public ApiResponse<List<IndicatorSnapshotDTO>> list(@RequestParam String symbolCode,
                                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                                                        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return ApiResponse.ok(indicatorService.listIndicators(symbolCode, start, end));
    }

    @GetMapping("/latest")
    public ApiResponse<IndicatorSnapshotDTO> latest(@RequestParam String symbolCode) {
        return ApiResponse.ok(indicatorService.getLatestIndicator(symbolCode));
    }
}
