package com.alexjoker.quant.marketdata.service;

import com.alexjoker.quant.marketdata.dto.DailyKlineDTO;
import com.alexjoker.quant.marketdata.dto.ImportKlineResultDTO;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDate;
import java.util.List;

public interface DailyKlineService {
    ImportKlineResultDTO importDaily(String symbolCode, MultipartFile file);

    ImportKlineResultDTO importFromEastMoney(String symbolCode, String stockCode, LocalDate start, LocalDate end, Integer adjustType);

    List<DailyKlineDTO> listDaily(String symbolCode, LocalDate start, LocalDate end);

    DailyKlineDTO getLatest(String symbolCode);
}
