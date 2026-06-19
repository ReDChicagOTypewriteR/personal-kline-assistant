package com.alexjoker.quant.signal.rule;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TechnicalScoreResult {
    private int score;
    private List<String> reasons = new ArrayList<>();
    private List<String> riskNotes = new ArrayList<>();
}
