package com.alexjoker.quant.dashboard.service;

import com.alexjoker.quant.dashboard.dto.DashboardDTO;
import com.alexjoker.quant.dashboard.dto.SymbolTechnicalDetailDTO;

import java.time.LocalDate;

public interface DashboardService {
    DashboardDTO getDashboard();

    SymbolTechnicalDetailDTO getSymbolDetail(String symbolCode, LocalDate start, LocalDate end);
}
