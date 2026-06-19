package com.alexjoker.quant.watchlist.service;

import com.alexjoker.quant.watchlist.dto.WatchlistDTO;

import java.util.List;

public interface WatchlistService {
    List<WatchlistDTO> list();

    WatchlistDTO add(String symbolCode, String groupName);

    void remove(String symbolCode, String groupName);

    void removeById(Long id);

    int removeByIds(List<Long> ids);

    long countEnabled();
}
