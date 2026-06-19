package com.alexjoker.quant.dashboard.dto;

import com.alexjoker.quant.signal.dto.TechnicalSignalDTO;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class DashboardDTO {
    private long totalSymbols;
    private long totalWatchlist;
    private long buyCandidateCount;
    private long watchCount;
    private long avoidCount;
    private List<TechnicalSignalDTO> latestSignals = new ArrayList<>();
}
