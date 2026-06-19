package com.alexjoker.quant.backtest.mapper;

import com.alexjoker.quant.backtest.entity.BacktestResultSnapshotEntity;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface BacktestResultSnapshotMapper extends BaseMapper<BacktestResultSnapshotEntity> {
}
