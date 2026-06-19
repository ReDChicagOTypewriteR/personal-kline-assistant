package com.alexjoker.quant.system.controller;

import com.alexjoker.quant.common.response.ApiResponse;
import com.alexjoker.quant.system.dto.CacheRefreshResultDTO;
import com.alexjoker.quant.system.dto.MarketDataSourceOverviewDTO;
import com.alexjoker.quant.system.service.SystemOpsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/system")
public class SystemOpsController {
    private final SystemOpsService systemOpsService;

    @GetMapping("/market-data/sources")
    public ApiResponse<MarketDataSourceOverviewDTO> marketDataSources() {
        return ApiResponse.ok(systemOpsService.marketDataSources());
    }

    @PostMapping("/cache/refresh")
    public ApiResponse<CacheRefreshResultDTO> refreshCache() {
        return ApiResponse.ok(systemOpsService.refreshCache());
    }

    @PostMapping("/cache/kline/refresh")
    public ApiResponse<CacheRefreshResultDTO> refreshKlineCache(@RequestParam(required = false) String symbolCode) {
        return ApiResponse.ok(systemOpsService.refreshKlineCache(symbolCode));
    }
}
