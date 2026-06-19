package com.alexjoker.quant.symbol.controller;

import com.alexjoker.quant.common.response.ApiResponse;
import com.alexjoker.quant.symbol.dto.CreateSymbolRequest;
import com.alexjoker.quant.symbol.dto.SymbolDTO;
import com.alexjoker.quant.symbol.service.SymbolService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/symbols")
@RequiredArgsConstructor
public class SymbolController {
    private final SymbolService symbolService;

    @GetMapping
    public ApiResponse<List<SymbolDTO>> listAll() {
        return ApiResponse.ok(symbolService.listAll());
    }

    @PostMapping
    public ApiResponse<SymbolDTO> create(@Valid @RequestBody CreateSymbolRequest request) {
        return ApiResponse.ok(symbolService.create(request));
    }

    @GetMapping("/enabled")
    public ApiResponse<List<SymbolDTO>> listEnabled() {
        return ApiResponse.ok(symbolService.listEnabled());
    }

    @GetMapping("/resolve")
    public ApiResponse<SymbolDTO> resolve(String symbolCode) {
        return ApiResponse.ok(symbolService.resolveCnSymbol(symbolCode));
    }

    @PutMapping("/{symbolCode}/disable")
    public ApiResponse<Void> disable(@PathVariable String symbolCode) {
        symbolService.setEnabled(symbolCode, false);
        return ApiResponse.ok(null);
    }

    @PutMapping("/{symbolCode}/enable")
    public ApiResponse<Void> enable(@PathVariable String symbolCode) {
        symbolService.setEnabled(symbolCode, true);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{symbolCode}")
    public ApiResponse<Void> delete(@PathVariable String symbolCode) {
        symbolService.deleteByCode(symbolCode);
        return ApiResponse.ok(null);
    }
}
