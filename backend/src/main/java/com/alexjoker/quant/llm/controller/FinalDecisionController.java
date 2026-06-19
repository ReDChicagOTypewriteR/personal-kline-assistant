package com.alexjoker.quant.llm.controller;

import com.alexjoker.quant.common.response.ApiResponse;
import com.alexjoker.quant.llm.dto.FinalTradeDecisionDTO;
import com.alexjoker.quant.llm.service.FinalDecisionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/final-decisions")
@RequiredArgsConstructor
public class FinalDecisionController {
    private final FinalDecisionService finalDecisionService;

    @GetMapping("/latest")
    public ApiResponse<List<FinalTradeDecisionDTO>> latest() {
        return ApiResponse.ok(finalDecisionService.listLatest());
    }

    @GetMapping("/history")
    public ApiResponse<List<FinalTradeDecisionDTO>> history(@RequestParam String symbolCode) {
        return ApiResponse.ok(finalDecisionService.listHistory(symbolCode));
    }
}
