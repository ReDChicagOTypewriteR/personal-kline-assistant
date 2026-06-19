package com.alexjoker.quant.watchlist.service.impl;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.common.util.TextUtil;
import com.alexjoker.quant.symbol.service.SymbolService;
import com.alexjoker.quant.watchlist.converter.WatchlistConverter;
import com.alexjoker.quant.watchlist.dto.WatchlistDTO;
import com.alexjoker.quant.watchlist.entity.WatchlistEntity;
import com.alexjoker.quant.watchlist.mapper.WatchlistMapper;
import com.alexjoker.quant.watchlist.service.WatchlistService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class WatchlistServiceImpl implements WatchlistService {
    private final WatchlistMapper watchlistMapper;
    private final SymbolService symbolService;

    @Override
    public List<WatchlistDTO> list() {
        return watchlistMapper.selectList(new LambdaQueryWrapper<WatchlistEntity>()
                .eq(WatchlistEntity::getEnabled, 1)
                .orderByDesc(WatchlistEntity::getPriority)
                .orderByAsc(WatchlistEntity::getSymbolCode))
                .stream().map(WatchlistConverter::toDto).toList();
    }

    @Override
    @Transactional
    public WatchlistDTO add(String symbolCode, String groupName) {
        String code = normalize(symbolCode);
        if (!symbolService.exists(code)) {
            throw new BusinessException("标的不存在，无法加入自选池: " + code);
        }
        WatchlistEntity entity = new WatchlistEntity();
        entity.setSymbolCode(code);
        entity.setGroupName(defaultGroup(groupName));
        entity.setPriority(0);
        entity.setEnabled(1);
        watchlistMapper.upsert(entity);
        WatchlistEntity saved = watchlistMapper.selectOne(new LambdaQueryWrapper<WatchlistEntity>()
                .eq(WatchlistEntity::getSymbolCode, code)
                .eq(WatchlistEntity::getGroupName, entity.getGroupName()));
        return WatchlistConverter.toDto(saved);
    }

    @Override
    @Transactional
    public void remove(String symbolCode, String groupName) {
        WatchlistEntity entity = watchlistMapper.selectOne(new LambdaQueryWrapper<WatchlistEntity>()
                .eq(WatchlistEntity::getSymbolCode, normalize(symbolCode))
                .eq(WatchlistEntity::getGroupName, defaultGroup(groupName)));
        if (entity == null) {
            return;
        }
        entity.setEnabled(0);
        watchlistMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void removeById(Long id) {
        if (id == null) {
            return;
        }
        WatchlistEntity entity = watchlistMapper.selectById(id);
        if (entity == null) {
            return;
        }
        entity.setEnabled(0);
        watchlistMapper.updateById(entity);
    }

    @Override
    @Transactional
    public int removeByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return 0;
        }
        return watchlistMapper.update(null, new LambdaUpdateWrapper<WatchlistEntity>()
                .in(WatchlistEntity::getId, ids)
                .set(WatchlistEntity::getEnabled, 0));
    }

    @Override
    public long countEnabled() {
        return watchlistMapper.selectCount(new LambdaQueryWrapper<WatchlistEntity>().eq(WatchlistEntity::getEnabled, 1));
    }

    private String normalize(String value) {
        return TextUtil.normalizeCode(value);
    }

    private String defaultGroup(String groupName) {
        return TextUtil.defaultUpper(groupName, "DEFAULT");
    }
}
