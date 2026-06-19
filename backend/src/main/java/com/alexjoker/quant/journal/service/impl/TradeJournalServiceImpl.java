package com.alexjoker.quant.journal.service.impl;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.common.util.NumberUtil;
import com.alexjoker.quant.common.util.TextUtil;
import com.alexjoker.quant.indicator.entity.IndicatorSnapshotEntity;
import com.alexjoker.quant.journal.converter.TradeJournalConverter;
import com.alexjoker.quant.journal.dto.TradeJournalDTO;
import com.alexjoker.quant.journal.dto.UpdateTradeJournalRequest;
import com.alexjoker.quant.journal.entity.TradeJournalEntity;
import com.alexjoker.quant.journal.mapper.TradeJournalMapper;
import com.alexjoker.quant.journal.service.TradeJournalService;
import com.alexjoker.quant.llm.entity.FinalTradeDecisionEntity;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.alexjoker.quant.signal.entity.TechnicalSignalEntity;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TradeJournalServiceImpl implements TradeJournalService {
    private final TradeJournalMapper tradeJournalMapper;

    @Override
    @Transactional
    public void recordSignal(TechnicalSignalEntity signal, DailyKlineEntity kline, IndicatorSnapshotEntity indicator) {
        if (signal == null) {
            return;
        }
        TradeJournalEntity entity = TradeJournalConverter.fromSignal(signal, kline, indicator);
        tradeJournalMapper.upsertSignalSnapshot(entity);
    }

    @Override
    @Transactional
    public void recordDecision(FinalTradeDecisionEntity decision) {
        if (decision == null) {
            return;
        }
        TradeJournalEntity entity = TradeJournalConverter.fromDecision(decision, defaultText(decision.getTechnicalSignal(), "UNKNOWN"));
        tradeJournalMapper.upsertDecisionSnapshot(entity);
    }

    @Override
    public List<TradeJournalDTO> listLatest(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 500));
        return tradeJournalMapper.selectList(new LambdaQueryWrapper<TradeJournalEntity>()
                .orderByDesc(TradeJournalEntity::getTradeDate)
                .orderByDesc(TradeJournalEntity::getId)
                .last("LIMIT " + safeLimit))
                .stream().map(TradeJournalConverter::toDto).toList();
    }

    @Override
    public List<TradeJournalDTO> listHistory(String symbolCode) {
        return tradeJournalMapper.selectList(new LambdaQueryWrapper<TradeJournalEntity>()
                .eq(TradeJournalEntity::getSymbolCode, normalize(symbolCode))
                .orderByDesc(TradeJournalEntity::getTradeDate)
                .orderByDesc(TradeJournalEntity::getId))
                .stream().map(TradeJournalConverter::toDto).toList();
    }

    @Override
    @Transactional
    public TradeJournalDTO update(Long id, UpdateTradeJournalRequest request) {
        TradeJournalEntity entity = tradeJournalMapper.selectById(id);
        if (entity == null) {
            throw new BusinessException("交易日志不存在: " + id);
        }
        entity.setExecuted(Boolean.TRUE.equals(request.getExecuted()) ? 1 : 0);
        entity.setExecutionDate(request.getExecutionDate());
        entity.setExecutionPrice(request.getExecutionPrice());
        entity.setExecutionNote(TextUtil.cleanOrNull(request.getExecutionNote()));
        entity.setFollowUpStatus(defaultText(request.getFollowUpStatus(), "PENDING"));
        entity.setFollowUpDate(request.getFollowUpDate());
        entity.setFollowUpPrice(request.getFollowUpPrice());
        entity.setFollowUpNote(TextUtil.cleanOrNull(request.getFollowUpNote()));
        entity.setFollowUpReturnRate(calculateFollowUpReturn(entity));
        tradeJournalMapper.updateById(entity);
        return TradeJournalConverter.toDto(tradeJournalMapper.selectById(id));
    }

    @Override
    @Transactional
    public void deleteById(Long id) {
        if (id != null) {
            tradeJournalMapper.deleteById(id);
        }
    }

    @Override
    @Transactional
    public int deleteByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return tradeJournalMapper.deleteBatchIds(ids);
    }

    @Override
    @Transactional
    public int deleteBySignal(String symbolCode, LocalDate tradeDate) {
        return tradeJournalMapper.delete(new LambdaQueryWrapper<TradeJournalEntity>()
                .eq(TradeJournalEntity::getSymbolCode, normalize(symbolCode))
                .eq(TradeJournalEntity::getTradeDate, tradeDate));
    }

    private BigDecimal calculateFollowUpReturn(TradeJournalEntity entity) {
        if (entity.getExecutionPrice() == null || entity.getFollowUpPrice() == null
                || entity.getExecutionPrice().compareTo(BigDecimal.ZERO) <= 0) {
            return null;
        }
        return NumberUtil.scale(entity.getFollowUpPrice()
                .subtract(entity.getExecutionPrice())
                .multiply(BigDecimal.valueOf(100))
                .divide(entity.getExecutionPrice(), 6, RoundingMode.HALF_UP));
    }

    private String normalize(String value) {
        return TextUtil.normalizeCode(value);
    }

    private String defaultText(String value, String fallback) {
        return TextUtil.defaultText(value, fallback);
    }
}
