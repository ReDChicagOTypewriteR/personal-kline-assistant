package com.alexjoker.quant.backtest.controller;

import com.alexjoker.quant.backtest.dto.BacktestBatchResultDTO;
import com.alexjoker.quant.backtest.dto.BacktestBatchRunRequest;
import com.alexjoker.quant.backtest.dto.BacktestRecordDetailDTO;
import com.alexjoker.quant.backtest.dto.BacktestRecordSummaryDTO;
import com.alexjoker.quant.backtest.dto.BacktestResultDTO;
import com.alexjoker.quant.backtest.dto.BacktestRunRequest;
import com.alexjoker.quant.backtest.service.BacktestRecordService;
import com.alexjoker.quant.backtest.service.BacktestService;
import com.alexjoker.quant.common.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/backtest")
@RequiredArgsConstructor
public class BacktestController {
    private final BacktestService backtestService;
    private final BacktestRecordService backtestRecordService;

    @PostMapping("/run")
    public ApiResponse<BacktestResultDTO> run(@Valid @RequestBody BacktestRunRequest request) {
        return ApiResponse.ok(backtestService.run(request));
    }

    @PostMapping("/batch-run")
    public ApiResponse<BacktestBatchResultDTO> batchRun(@RequestBody BacktestBatchRunRequest request) {
        return ApiResponse.ok(backtestService.runBatch(request));
    }

    @GetMapping("/records")
    public ApiResponse<List<BacktestRecordSummaryDTO>> records(
            @RequestParam(required = false) String symbolCode,
            @RequestParam(defaultValue = "20") int limit
    ) {
        return ApiResponse.ok(backtestRecordService.listLatest(symbolCode, limit));
    }

    @GetMapping("/records/{id}")
    public ApiResponse<BacktestRecordDetailDTO> recordDetail(@PathVariable Long id) {
        return ApiResponse.ok(backtestRecordService.detail(id));
    }

    @DeleteMapping("/records/{id}")
    public ApiResponse<Void> deleteRecord(@PathVariable Long id) {
        backtestRecordService.deleteById(id);
        return ApiResponse.ok(null);
    }
}
