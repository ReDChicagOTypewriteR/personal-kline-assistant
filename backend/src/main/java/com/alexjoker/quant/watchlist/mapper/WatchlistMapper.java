package com.alexjoker.quant.watchlist.mapper;

import com.alexjoker.quant.watchlist.entity.WatchlistEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WatchlistMapper extends BaseMapper<WatchlistEntity> {
    @Insert("""
            INSERT INTO watchlist(symbol_code, group_name, priority, note, enabled, updated_at)
            VALUES(#{symbolCode}, #{groupName}, #{priority}, #{note}, 1, CURRENT_TIMESTAMP)
            ON DUPLICATE KEY UPDATE
                enabled = 1,
                priority = VALUES(priority),
                note = VALUES(note),
                updated_at = CURRENT_TIMESTAMP
            """)
    int upsert(WatchlistEntity entity);
}
