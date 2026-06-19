package com.alexjoker.quant.symbol.service.impl;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.common.util.TextUtil;
import com.alexjoker.quant.marketdata.provider.AdataKlineDataProvider;
import com.alexjoker.quant.marketdata.provider.EastMoneyKlineDataProvider;
import com.alexjoker.quant.symbol.converter.SymbolConverter;
import com.alexjoker.quant.symbol.dto.CreateSymbolRequest;
import com.alexjoker.quant.symbol.dto.SymbolDTO;
import com.alexjoker.quant.symbol.entity.SymbolEntity;
import com.alexjoker.quant.symbol.mapper.SymbolMapper;
import com.alexjoker.quant.symbol.service.SymbolDataCleanupService;
import com.alexjoker.quant.symbol.service.SymbolService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class SymbolServiceImpl implements SymbolService {
    private final SymbolMapper symbolMapper;
    private final EastMoneyKlineDataProvider eastMoneyKlineDataProvider;
    private final AdataKlineDataProvider adataKlineDataProvider;
    private final SymbolDataCleanupService symbolDataCleanupService;

    @Override
    public List<SymbolDTO> listAll() {
        return symbolMapper.selectList(new LambdaQueryWrapper<SymbolEntity>().orderByAsc(SymbolEntity::getSymbolCode))
                .stream().map(SymbolConverter::toDto).toList();
    }

    @Override
    public List<SymbolDTO> listEnabled() {
        return symbolMapper.selectList(new LambdaQueryWrapper<SymbolEntity>()
                .eq(SymbolEntity::getEnabled, 1)
                .orderByAsc(SymbolEntity::getSymbolCode))
                .stream().map(SymbolConverter::toDto).toList();
    }

    @Override
    @Transactional
    public SymbolDTO create(CreateSymbolRequest request) {
        String code = normalize(request.getSymbolCode());
        SymbolEntity existing = findByNormalizedCode(code);
        if (existing != null) {
            return reuseExisting(existing, request);
        }
        SymbolEntity entity = new SymbolEntity();
        entity.setSymbolCode(code);
        entity.setSymbolName(request.getSymbolName());
        entity.setMarket(normalize(request.getMarket()));
        entity.setAssetType(normalizeNullable(request.getAssetType()));
        entity.setCurrency(normalizeNullable(request.getCurrency()));
        entity.setEnabled(1);
        symbolMapper.upsert(entity);
        return SymbolConverter.toDto(findEntity(code));
    }

    @Override
    @Transactional
    public void setEnabled(String symbolCode, boolean enabled) {
        SymbolEntity entity = findEntity(symbolCode);
        entity.setEnabled(enabled ? 1 : 0);
        symbolMapper.updateById(entity);
    }

    @Override
    @Transactional
    public void deleteByCode(String symbolCode) {
        String code = normalize(symbolCode);
        findEntity(code);
        symbolDataCleanupService.deleteAllBySymbolCode(code);
        symbolMapper.delete(new LambdaQueryWrapper<SymbolEntity>().eq(SymbolEntity::getSymbolCode, code));
    }

    @Override
    public SymbolDTO getByCode(String symbolCode) {
        return SymbolConverter.toDto(findEntity(symbolCode));
    }

    @Override
    @Transactional
    public SymbolDTO getOrCreateCnSymbol(String symbolCode) {
        return resolveCnSymbol(symbolCode);
    }

    @Override
    @Transactional
    public SymbolDTO resolveCnSymbol(String symbolCode) {
        String code = normalize(symbolCode);
        SymbolEntity existing = findByNormalizedCode(code);
        if (existing != null) {
            return reuseResolvedSymbol(existing);
        }
        if (!isAshareCode(code)) {
            throw new BusinessException("标的不存在: " + symbolCode);
        }
        SymbolEntity entity = new SymbolEntity();
        entity.setSymbolCode(code);
        entity.setSymbolName(resolveSymbolName(code));
        entity.setMarket("CN");
        entity.setAssetType(inferAssetType(code));
        entity.setCurrency("CNY");
        entity.setEnabled(1);
        symbolMapper.upsert(entity);
        return SymbolConverter.toDto(findEntity(code));
    }

    @Override
    public boolean exists(String symbolCode) {
        return symbolMapper.selectCount(new LambdaQueryWrapper<SymbolEntity>()
                .eq(SymbolEntity::getSymbolCode, normalize(symbolCode))) > 0;
    }

    private SymbolEntity findEntity(String symbolCode) {
        SymbolEntity entity = findByNormalizedCode(normalize(symbolCode));
        if (entity == null) {
            throw new BusinessException("标的不存在: " + symbolCode);
        }
        return entity;
    }

    private SymbolEntity findByNormalizedCode(String code) {
        return symbolMapper.selectOne(new LambdaQueryWrapper<SymbolEntity>().eq(SymbolEntity::getSymbolCode, code));
    }

    private SymbolDTO reuseResolvedSymbol(SymbolEntity entity) {
        enrichNameIfNeeded(entity);
        if (entity.getEnabled() == null || entity.getEnabled() == 0) {
            entity.setEnabled(1);
            symbolMapper.updateById(entity);
        }
        return SymbolConverter.toDto(entity);
    }

    private SymbolDTO reuseExisting(SymbolEntity entity, CreateSymbolRequest request) {
        boolean changed = false;
        if (entity.getEnabled() == null || entity.getEnabled() == 0) {
            entity.setEnabled(1);
            changed = true;
        }
        if ((entity.getSymbolName() == null || entity.getSymbolName().isBlank() || entity.getSymbolName().equalsIgnoreCase(entity.getSymbolCode()))
                && request.getSymbolName() != null && !request.getSymbolName().isBlank()) {
            entity.setSymbolName(request.getSymbolName().trim());
            changed = true;
        }
        if ((entity.getMarket() == null || entity.getMarket().isBlank()) && request.getMarket() != null && !request.getMarket().isBlank()) {
            entity.setMarket(normalize(request.getMarket()));
            changed = true;
        }
        if ((entity.getAssetType() == null || entity.getAssetType().isBlank()) && request.getAssetType() != null && !request.getAssetType().isBlank()) {
            entity.setAssetType(normalizeNullable(request.getAssetType()));
            changed = true;
        }
        if ((entity.getCurrency() == null || entity.getCurrency().isBlank()) && request.getCurrency() != null && !request.getCurrency().isBlank()) {
            entity.setCurrency(normalizeNullable(request.getCurrency()));
            changed = true;
        }
        if (changed) {
            symbolMapper.updateById(entity);
        }
        return SymbolConverter.toDto(entity);
    }

    private String normalize(String value) {
        return TextUtil.normalizeCode(value);
    }

    private String normalizeNullable(String value) {
        return TextUtil.upperOrNull(value);
    }

    private boolean isAshareCode(String value) {
        if (value == null) {
            return false;
        }
        String code = value;
        int dotIndex = code.indexOf('.');
        if (dotIndex > 0) {
            code = code.substring(0, dotIndex);
        }
        return code.matches("\\d{6}");
    }

    private void enrichNameIfNeeded(SymbolEntity entity) {
        if (entity.getSymbolName() != null && !entity.getSymbolName().isBlank()
                && !entity.getSymbolName().equalsIgnoreCase(entity.getSymbolCode())) {
            return;
        }
        String resolvedName = resolveSymbolName(entity.getSymbolCode());
        if (resolvedName != null && !resolvedName.equals(entity.getSymbolName())) {
            entity.setSymbolName(resolvedName);
            if (entity.getAssetType() == null || entity.getAssetType().isBlank()) {
                entity.setAssetType(inferAssetType(entity.getSymbolCode()));
            }
            symbolMapper.updateById(entity);
        }
    }

    private String resolveSymbolName(String code) {
        try {
            var symbol = adataKlineDataProvider.resolveSymbol(code);
            if (symbol != null && symbol.symbolName != null && !symbol.symbolName.isBlank()
                    && !symbol.symbolName.equalsIgnoreCase(code)) {
                return symbol.symbolName.trim();
            }
        } catch (BusinessException ignored) {
            // Fall back to the Java EastMoney resolver below.
        }
        String name = eastMoneyKlineDataProvider.fetchSecurityName(code);
        return name == null || name.isBlank() ? code : name;
    }

    private String inferAssetType(String code) {
        if (code == null) {
            return "STOCK";
        }
        String value = code.trim();
        if (value.startsWith("15") || value.startsWith("16") || value.startsWith("50")
                || value.startsWith("51") || value.startsWith("56") || value.startsWith("58")) {
            return "ETF";
        }
        return "STOCK";
    }
}
