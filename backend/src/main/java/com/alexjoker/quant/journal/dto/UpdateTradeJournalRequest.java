package com.alexjoker.quant.journal.dto;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class UpdateTradeJournalRequest {
    private Boolean executed;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate executionDate;
    private BigDecimal executionPrice;
    private String executionNote;
    private String followUpStatus;
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate followUpDate;
    private BigDecimal followUpPrice;
    private String followUpNote;
}
