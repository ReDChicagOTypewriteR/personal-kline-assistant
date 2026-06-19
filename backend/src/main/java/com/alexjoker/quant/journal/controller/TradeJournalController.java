package com.alexjoker.quant.journal.controller;

import com.alexjoker.quant.common.response.ApiResponse;
import com.alexjoker.quant.journal.dto.TradeJournalDTO;
import com.alexjoker.quant.journal.dto.UpdateTradeJournalRequest;
import com.alexjoker.quant.journal.service.TradeJournalService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/trade-journal")
public class TradeJournalController {
    private final TradeJournalService tradeJournalService;

    @GetMapping("/latest")
    public ApiResponse<List<TradeJournalDTO>> latest(@RequestParam(defaultValue = "100") int limit) {
        return ApiResponse.ok(tradeJournalService.listLatest(limit));
    }

    @GetMapping("/history")
    public ApiResponse<List<TradeJournalDTO>> history(@RequestParam String symbolCode) {
        return ApiResponse.ok(tradeJournalService.listHistory(symbolCode));
    }

    @PutMapping("/{id}")
    public ApiResponse<TradeJournalDTO> update(@PathVariable Long id, @RequestBody UpdateTradeJournalRequest request) {
        return ApiResponse.ok(tradeJournalService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        tradeJournalService.deleteById(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/batch")
    public ApiResponse<Map<String, Integer>> deleteBatch(@RequestBody List<Long> ids) {
        return ApiResponse.ok(Map.of("count", tradeJournalService.deleteByIds(ids)));
    }
}
