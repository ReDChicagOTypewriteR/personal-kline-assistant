package com.alexjoker.quant.watchlist.controller;

import com.alexjoker.quant.common.response.ApiResponse;
import com.alexjoker.quant.watchlist.dto.WatchlistDTO;
import com.alexjoker.quant.watchlist.service.WatchlistService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.List;

@RestController
@RequestMapping("/api/watchlist")
@RequiredArgsConstructor
public class WatchlistController {
    private final WatchlistService watchlistService;

    @GetMapping
    public ApiResponse<List<WatchlistDTO>> list() {
        return ApiResponse.ok(watchlistService.list());
    }

    @PostMapping
    public ApiResponse<WatchlistDTO> add(@RequestParam String symbolCode,
                                         @RequestParam(defaultValue = "DEFAULT") String groupName) {
        return ApiResponse.ok(watchlistService.add(symbolCode, groupName));
    }

    @DeleteMapping
    public ApiResponse<Void> remove(@RequestParam String symbolCode,
                                    @RequestParam(defaultValue = "DEFAULT") String groupName) {
        watchlistService.remove(symbolCode, groupName);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> removeById(@PathVariable Long id) {
        watchlistService.removeById(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/batch")
    public ApiResponse<Map<String, Integer>> removeBatch(@RequestBody List<Long> ids) {
        return ApiResponse.ok(Map.of("count", watchlistService.removeByIds(ids)));
    }
}
