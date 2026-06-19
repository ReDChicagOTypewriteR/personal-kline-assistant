package com.alexjoker.quant.backtest.service.impl;

import com.alexjoker.quant.backtest.converter.BacktestRecordConverter;
import com.alexjoker.quant.backtest.dto.BacktestRecordDetailDTO;
import com.alexjoker.quant.backtest.dto.BacktestRecordSummaryDTO;
import com.alexjoker.quant.backtest.dto.BacktestResultDTO;
import com.alexjoker.quant.backtest.dto.BacktestRunRequest;
import com.alexjoker.quant.backtest.entity.BacktestResultSnapshotEntity;
import com.alexjoker.quant.backtest.mapper.BacktestResultSnapshotMapper;
import com.alexjoker.quant.backtest.service.BacktestRecordService;
import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.common.util.TextUtil;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BacktestRecordServiceImpl implements BacktestRecordService {
    private final BacktestResultSnapshotMapper snapshotMapper;
    private final ObjectMapper objectMapper;

    @Override
    @Transactional
    public Long save(BacktestRunRequest request, BacktestResultDTO result) {
        if (request == null || result == null || result.getSummary() == null) {
            throw new BusinessException("回测结果为空，无法保存");
        }
        BacktestResultSnapshotEntity entity = new BacktestResultSnapshotEntity();
        entity.setSymbolCode(result.getSymbolCode());
        entity.setStrategyType(result.getStrategyType());
        entity.setStartDate(result.getStart());
        entity.setEndDate(result.getEnd());
        entity.setInitialCapital(result.getSummary().getInitialCapital());
        entity.setPositionRatio(request.getPositionRatio());
        entity.setFeeRate(request.getFeeRate());
        entity.setSlippageRate(request.getSlippageRate());
        entity.setLotSize(request.getLotSize());
        entity.setFinalEquity(result.getSummary().getFinalEquity());
        entity.setTotalReturnRate(result.getSummary().getTotalReturnRate());
        entity.setAnnualizedReturnRate(result.getSummary().getAnnualizedReturnRate());
        entity.setMaxDrawdownRate(result.getSummary().getMaxDrawdownRate());
        entity.setWinRate(result.getSummary().getWinRate());
        entity.setTradeCount(result.getSummary().getTradeCount());
        try {
            entity.setRequestJson(objectMapper.writeValueAsString(request));
            entity.setResultJson(objectMapper.writeValueAsString(result));
        } catch (JsonProcessingException ex) {
            throw new BusinessException("回测结果序列化失败: " + ex.getMessage());
        }
        snapshotMapper.insert(entity);
        return entity.getId();
    }

    @Override
    public List<BacktestRecordSummaryDTO> listLatest(String symbolCode, int limit) {
        int safeLimit = TextUtil.clamp(limit, 1, 100);
        LambdaQueryWrapper<BacktestResultSnapshotEntity> wrapper = new LambdaQueryWrapper<>();
        if (symbolCode != null && !symbolCode.isBlank()) {
            wrapper.eq(BacktestResultSnapshotEntity::getSymbolCode, normalize(symbolCode));
        }
        wrapper.orderByDesc(BacktestResultSnapshotEntity::getCreatedAt)
                .orderByDesc(BacktestResultSnapshotEntity::getId)
                .last("LIMIT " + safeLimit);
        return snapshotMapper.selectList(wrapper).stream().map(this::toSummary).toList();
    }

    @Override
    public BacktestRecordDetailDTO detail(Long id) {
        BacktestResultSnapshotEntity entity = snapshotMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("回测记录不存在: " + id);
        }
        BacktestRecordDetailDTO dto = new BacktestRecordDetailDTO();
        dto.setSummary(toSummary(entity));
        try {
            dto.setRequest(objectMapper.readValue(entity.getRequestJson(), BacktestRunRequest.class));
            BacktestResultDTO result = objectMapper.readValue(entity.getResultJson(), BacktestResultDTO.class);
            result.setRecordId(entity.getId());
            dto.setResult(result);
        } catch (JsonProcessingException ex) {
            throw new BusinessException("回测记录解析失败: " + ex.getMessage());
        }
        return dto;
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id != null) {
            snapshotMapper.deleteById(id);
        }
    }

    private BacktestRecordSummaryDTO toSummary(BacktestResultSnapshotEntity entity) {
        BacktestRecordSummaryDTO dto = BacktestRecordConverter.toSummary(entity);
        applyBenchmarkFields(entity, dto);
        return dto;
    }

    private void applyBenchmarkFields(BacktestResultSnapshotEntity entity, BacktestRecordSummaryDTO dto) {
        try {
            BacktestResultDTO result = objectMapper.readValue(entity.getResultJson(), BacktestResultDTO.class);
            BacktestRecordConverter.applyBenchmark(dto, result);
        } catch (JsonProcessingException ignored) {
            // Older or corrupted snapshots can still be listed without benchmark fields.
        }
    }

    private String normalize(String value) {
        return TextUtil.normalizeCode(value);
    }
}
