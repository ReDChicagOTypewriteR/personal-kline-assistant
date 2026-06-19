package com.alexjoker.quant.marketdata.provider;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.marketdata.config.MarketDataEngineProperties;
import com.alexjoker.quant.marketdata.dto.MarketKlineDTO;
import com.alexjoker.quant.marketdata.engine.MarketDataEngineManager;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AdataKlineDataProvider {
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final MarketDataEngineProperties properties;
    private final MarketDataEngineManager engineManager;

    public boolean isEnabled() {
        return properties.isEnabled() && engineManager.isAvailable();
    }

    public List<DailyKlineEntity> fetchDaily(String source, String symbolCode, String stockCode, LocalDate start,
                                             LocalDate end, int adjustType) {
        List<EngineRow> rows = fetchRows(source, stockCode, start, end, "D", adjustType);
        List<DailyKlineEntity> results = new ArrayList<>();
        for (EngineRow row : rows) {
            DailyKlineEntity entity = new DailyKlineEntity();
            entity.setSymbolCode(symbolCode);
            entity.setTradeDate(LocalDate.parse(requiredText(row, "trade_date")));
            entity.setOpenPrice(requiredDecimal(row, "open"));
            entity.setHighPrice(requiredDecimal(row, "high"));
            entity.setLowPrice(requiredDecimal(row, "low"));
            entity.setClosePrice(requiredDecimal(row, "close"));
            entity.setVolume(optionalDecimal(row, "volume"));
            entity.setAmount(optionalDecimal(row, "amount"));
            results.add(entity);
        }
        return results;
    }

    public List<MarketKlineDTO> fetchChartKlines(String source, String stockCode, String period, LocalDate start,
                                                 LocalDate end, int adjustType) {
        List<EngineRow> rows = fetchRows(source, stockCode, start, end, period, adjustType);
        List<MarketKlineDTO> results = new ArrayList<>();
        for (EngineRow row : rows) {
            MarketKlineDTO dto = new MarketKlineDTO();
            dto.setTimestamp(parseTimestamp(row));
            dto.setOpen(optionalDecimal(row, "open"));
            dto.setHigh(optionalDecimal(row, "high"));
            dto.setLow(optionalDecimal(row, "low"));
            dto.setClose(optionalDecimal(row, "close"));
            dto.setVolume(optionalDecimal(row, "volume"));
            dto.setTurnover(optionalDecimal(row, "amount"));
            results.add(dto);
        }
        return results;
    }

    public EngineSymbolResponse resolveSymbol(String symbolCode) {
        if (!isEnabled()) {
            throw new BusinessException("adata 行情引擎不可用");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("symbolCode", symbolCode);
        try {
            return restClient()
                    .post()
                    .uri("/symbols/resolve")
                    .body(body)
                    .retrieve()
                    .body(EngineSymbolResponse.class);
        } catch (Exception ex) {
            throw new BusinessException("adata 行情引擎解析标的失败: " + ex.getMessage());
        }
    }

    private List<EngineRow> fetchRows(String source, String stockCode, LocalDate start, LocalDate end, String period,
                                      int adjustType) {
        if (!isEnabled()) {
            throw new BusinessException("adata 行情引擎不可用");
        }
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("source", source);
        body.put("symbolCode", stockCode);
        body.put("start", start == null ? "1990-01-01" : start.toString());
        body.put("end", end == null ? "" : end.toString());
        body.put("period", period == null ? "D" : period);
        body.put("adjustType", adjustType);
        EngineRowsResponse response;
        try {
            response = restClient()
                    .post()
                    .uri("/klines/daily")
                    .body(body)
                    .retrieve()
                    .body(EngineRowsResponse.class);
        } catch (Exception ex) {
            throw new BusinessException("adata 行情引擎请求失败(" + source + "): " + ex.getMessage());
        }
        if (response == null) {
            throw new BusinessException("adata 行情引擎返回空响应: " + source);
        }
        if (response.error != null && !response.error.isBlank()) {
            throw new BusinessException(response.error);
        }
        if (response.rows == null || response.rows.isEmpty()) {
            throw new BusinessException("adata 行情引擎未返回 K 线数据: " + source);
        }
        return response.rows;
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(3));
        factory.setReadTimeout(Duration.ofSeconds(properties.getRequestTimeoutSeconds()));
        return RestClient.builder()
                .baseUrl(engineManager.baseUrl())
                .requestFactory(factory)
                .build();
    }

    private Long parseTimestamp(EngineRow row) {
        String text = text(row, "trade_time");
        if (text != null && text.length() >= 19) {
            return LocalDateTime.parse(text.substring(0, 19), DATE_TIME).atZone(CHINA_ZONE).toInstant().toEpochMilli();
        }
        return LocalDate.parse(requiredText(row, "trade_date")).atStartOfDay(CHINA_ZONE).toInstant().toEpochMilli();
    }

    private String requiredText(EngineRow row, String field) {
        String value = text(row, field);
        if (value == null || value.isBlank() || "None".equalsIgnoreCase(value) || "nan".equalsIgnoreCase(value)) {
            throw new BusinessException("adata 数据缺少字段: " + field);
        }
        return value.trim();
    }

    private String text(EngineRow row, String field) {
        Object value = switch (field) {
            case "trade_date" -> row.tradeDate;
            case "trade_time" -> row.tradeTime;
            case "open" -> row.open;
            case "high" -> row.high;
            case "low" -> row.low;
            case "close" -> row.close;
            case "volume" -> row.volume;
            case "amount" -> row.amount;
            default -> null;
        };
        return value == null ? null : String.valueOf(value);
    }

    private BigDecimal requiredDecimal(EngineRow row, String field) {
        String value = requiredText(row, field);
        return new BigDecimal(value);
    }

    private BigDecimal optionalDecimal(EngineRow row, String field) {
        String value = text(row, field);
        if (value == null || value.isBlank() || "None".equalsIgnoreCase(value) || "nan".equalsIgnoreCase(value)) {
            return null;
        }
        return new BigDecimal(value);
    }

    public static class EngineRowsResponse {
        public String source;
        public String error;
        public List<EngineRow> rows;
    }

    public static class EngineRow {
        public String tradeDate;
        public String tradeTime;
        public Object open;
        public Object high;
        public Object low;
        public Object close;
        public Object volume;
        public Object amount;
    }

    public static class EngineSymbolResponse {
        public String symbolCode;
        public String symbolName;
        public String market;
        public String assetType;
        public String currency;
    }
}
