package com.alexjoker.quant.marketdata.controller;

import com.alexjoker.quant.common.response.ApiResponse;
import com.alexjoker.quant.marketdata.dto.DailyKlineDTO;
import com.alexjoker.quant.marketdata.dto.ImportKlineResultDTO;
import com.alexjoker.quant.marketdata.service.DailyKlineService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/klines/daily")
@RequiredArgsConstructor
public class DailyKlineController {
    private final DailyKlineService dailyKlineService;

    @PostMapping("/import")
    public ApiResponse<ImportKlineResultDTO> importDaily(@RequestParam String symbolCode,
                                                        @RequestParam MultipartFile file) {
        return ApiResponse.ok(dailyKlineService.importDaily(symbolCode, file));
    }

    @PostMapping("/fetch/eastmoney")
    public ApiResponse<ImportKlineResultDTO> fetchFromEastMoney(@RequestParam String symbolCode,
                                                                @RequestParam(required = false) String stockCode,
                                                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                                                                @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end,
                                                                @RequestParam(required = false, defaultValue = "1") Integer adjustType) {
        return ApiResponse.ok(dailyKlineService.importFromEastMoney(symbolCode, stockCode, start, end, adjustType));
    }

    @GetMapping
    public ApiResponse<List<DailyKlineDTO>> listDaily(@RequestParam String symbolCode,
                                                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate start,
                                                      @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate end) {
        return ApiResponse.ok(dailyKlineService.listDaily(symbolCode, start, end));
    }

    @GetMapping("/latest")
    public ApiResponse<DailyKlineDTO> latest(@RequestParam String symbolCode) {
        return ApiResponse.ok(dailyKlineService.getLatest(symbolCode));
    }
}
