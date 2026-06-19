package com.alexjoker.quant.watchlist.dto;

import lombok.Data;

@Data
public class WatchlistDTO {
    private Long id;
    private String symbolCode;
    private String groupName;
    private Integer priority;
    private String note;
    private Integer enabled;
}
