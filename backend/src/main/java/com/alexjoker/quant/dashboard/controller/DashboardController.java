package com.alexjoker.quant.dashboard.controller;

import com.alexjoker.quant.common.response.ApiResponse;
import com.alexjoker.quant.dashboard.dto.DashboardDTO;
import com.alexjoker.quant.dashboard.dto.SymbolTechnicalDetailDTO;
import com.alexjoker.quant.dashboard.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardController {
    private final DashboardService dashboardService;

    @GetMapping
    public ApiResponse<DashboardDTO> dashboard() {
        return ApiResponse.ok(dashboardService.getDashboard());
    }

    @GetMapping("/symbol/{symbolCode}")
    public ApiResponse<SymbolTechnicalDetailDTO> symbolDetail(@PathVariable String symbolCode,
                                                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                                                              @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return ApiResponse.ok(dashboardService.getSymbolDetail(symbolCode, start, end));
    }
}
