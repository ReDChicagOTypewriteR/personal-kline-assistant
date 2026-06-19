package com.alexjoker.quant.llm.dto;

import lombok.Data;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Data
public class AiBatchAnalyzeResultDTO {
    private LocalDate tradeDate;
    private int successCount;
    private int failedCount;
    private int skippedCount;
    private List<AiAnalyzeResultDTO> results = new ArrayList<>();
}
