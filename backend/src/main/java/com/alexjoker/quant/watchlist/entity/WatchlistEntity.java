package com.alexjoker.quant.watchlist.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("watchlist")
public class WatchlistEntity {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String symbolCode;
    private String groupName;
    private Integer priority;
    private String note;
    private Integer enabled;
    private String createdAt;
    private String updatedAt;
}
