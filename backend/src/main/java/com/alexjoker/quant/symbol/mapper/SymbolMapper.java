package com.alexjoker.quant.symbol.mapper;

import com.alexjoker.quant.symbol.entity.SymbolEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface SymbolMapper extends BaseMapper<SymbolEntity> {
    @Insert("""
            INSERT INTO symbol (symbol_code, symbol_name, market, asset_type, currency, enabled)
            VALUES (#{symbolCode}, #{symbolName}, #{market}, #{assetType}, #{currency}, #{enabled})
            ON DUPLICATE KEY UPDATE
                symbol_name = CASE
                    WHEN symbol_name IS NULL OR symbol_name = '' OR symbol_name = symbol_code
                    THEN VALUES(symbol_name)
                    ELSE symbol_name
                END,
                market = CASE
                    WHEN market IS NULL OR market = ''
                    THEN VALUES(market)
                    ELSE market
                END,
                asset_type = CASE
                    WHEN asset_type IS NULL OR asset_type = ''
                    THEN VALUES(asset_type)
                    ELSE asset_type
                END,
                currency = CASE
                    WHEN currency IS NULL OR currency = ''
                    THEN VALUES(currency)
                    ELSE currency
                END,
                enabled = 1,
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(SymbolEntity entity);
}
