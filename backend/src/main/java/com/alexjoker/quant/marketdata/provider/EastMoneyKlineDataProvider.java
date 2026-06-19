package com.alexjoker.quant.marketdata.provider;

import com.alexjoker.quant.common.exception.BusinessException;
import com.alexjoker.quant.marketdata.dto.MarketKlineDTO;
import com.alexjoker.quant.marketdata.entity.DailyKlineEntity;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class EastMoneyKlineDataProvider {
    private static final String API_URL = "https://push2his.eastmoney.com/api/qt/stock/kline/get";
    private static final DateTimeFormatter COMPACT_DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
    private static final ZoneId CHINA_ZONE = ZoneId.of("Asia/Shanghai");

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .version(HttpClient.Version.HTTP_1_1)
            .build();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<DailyKlineEntity> fetchDaily(String symbolCode, String stockCode, LocalDate startDate,
                                             LocalDate endDate, int adjustType) {
        try {
            List<DailyKlineEntity> results = new ArrayList<>();
            for (JsonNode item : fetchKlineNodes(stockCode, startDate, endDate, "101", adjustType)) {
                results.add(parseLine(symbolCode, item.asText()));
            }
            return results;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("获取东方财富日 K 失败: " + ex.getMessage());
        }
    }

    public List<MarketKlineDTO> fetchChartKlines(String stockCode, String period, LocalDate startDate,
                                                LocalDate endDate, int adjustType) {
        String normalizedPeriod = normalizePeriod(period);
        String sourceKlt = sourceKlt(normalizedPeriod);
        try {
            List<MarketKlineDTO> rows = new ArrayList<>();
            for (JsonNode item : fetchKlineNodes(stockCode, startDate, endDate, sourceKlt, adjustType)) {
                rows.add(parseChartLine(item.asText()));
            }
            if ("2h".equals(normalizedPeriod)) {
                return aggregateByHour(rows, 2);
            }
            if ("4h".equals(normalizedPeriod)) {
                return aggregateByHour(rows, 4);
            }
            if ("Y".equals(normalizedPeriod)) {
                return aggregateByYear(rows);
            }
            return rows;
        } catch (BusinessException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new BusinessException("获取东方财富图表行情失败: " + ex.getMessage());
        }
    }

    public String fetchSecurityName(String stockCode) {
        try {
            JsonNode dataNode = fetchKlineDataNode(stockCode, LocalDate.now().minusDays(30), LocalDate.now(), "101", 1, false);
            String name = dataNode.path("name").asText(null);
            return name == null || name.isBlank() ? null : name.trim();
        } catch (Exception ignored) {
            return null;
        }
    }

    public boolean healthCheck() {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(API_URL + "?" + buildQuery("510300", LocalDate.now().minusDays(10), LocalDate.now(), "101", 1)))
                    .timeout(Duration.ofSeconds(5))
                    .GET()
                    .version(HttpClient.Version.HTTP_1_1)
                    .header("Accept", "application/json,text/plain,*/*")
                    .header("Referer", "https://quote.eastmoney.com/")
                    .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                return false;
            }
            JsonNode dataNode = objectMapper.readTree(response.body()).path("data");
            return !dataNode.isMissingNode() && !dataNode.isNull();
        } catch (Exception ignored) {
            return false;
        }
    }

    private JsonNode fetchKlineNodes(String stockCode, LocalDate startDate, LocalDate endDate, String klt, int adjustType) throws Exception {
        JsonNode dataNode = fetchKlineDataNode(stockCode, startDate, endDate, klt, adjustType, true);
        return dataNode.path("klines");
    }

    private JsonNode fetchKlineDataNode(String stockCode, LocalDate startDate, LocalDate endDate, String klt,
                                        int adjustType, boolean requireKlines) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(API_URL + "?" + buildQuery(stockCode, startDate, endDate, klt, adjustType)))
                .timeout(Duration.ofSeconds(20))
                .GET()
                .version(HttpClient.Version.HTTP_1_1)
                .header("Accept", "application/json,text/plain,*/*")
                .header("Referer", "https://quote.eastmoney.com/")
                .header("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0 Safari/537.36")
                .build();
        Exception lastException = null;
        for (int attempt = 1; attempt <= 3; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
                if (response.statusCode() < 200 || response.statusCode() >= 300) {
                    throw new BusinessException("东方财富行情接口请求失败，HTTP " + response.statusCode());
                }
                JsonNode root = objectMapper.readTree(response.body());
                JsonNode dataNode = root.path("data");
                if (dataNode.isMissingNode() || dataNode.isNull()) {
                    throw new BusinessException("东方财富未返回行情数据: " + stockCode);
                }
                JsonNode klines = dataNode.path("klines");
                if (requireKlines && (!klines.isArray() || klines.isEmpty())) {
                    throw new BusinessException("东方财富未返回 K 线数据: " + stockCode);
                }
                return dataNode;
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                throw ex;
            } catch (IOException ex) {
                lastException = ex;
                if (attempt == 3) {
                    break;
                }
                sleepBeforeRetry(attempt);
            }
        }
        throw lastException == null ? new BusinessException("东方财富行情接口请求失败") : lastException;
    }

    private void sleepBeforeRetry(int attempt) {
        try {
            Thread.sleep(300L * attempt);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        }
    }

    private String buildQuery(String stockCode, LocalDate startDate, LocalDate endDate, String klt, int adjustType) {
        String marketId = resolveMarketId(stockCode);
        Map<String, String> params = new LinkedHashMap<>();
        params.put("fields1", "f1,f2,f3,f4,f5,f6");
        params.put("fields2", "f51,f52,f53,f54,f55,f56,f57,f58,f59,f60,f61,f116");
        params.put("ut", "7eea3edcaed734bea9cbfc24409ed989");
        params.put("klt", klt);
        params.put("fqt", String.valueOf(adjustType));
        params.put("secid", marketId + "." + stockCode);
        params.put("beg", formatDate(startDate, "19900101"));
        params.put("end", formatDate(endDate, LocalDate.now().format(COMPACT_DATE)));
        params.put("_", String.valueOf(System.currentTimeMillis()));
        List<String> pairs = new ArrayList<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            pairs.add(encode(entry.getKey()) + "=" + encode(entry.getValue()));
        }
        return String.join("&", pairs);
    }

    private String resolveMarketId(String stockCode) {
        if (stockCode.startsWith("5") || stockCode.startsWith("6") || stockCode.startsWith("9")) {
            return "1";
        }
        return "0";
    }

    private DailyKlineEntity parseLine(String symbolCode, String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 7) {
            throw new BusinessException("东方财富日 K 数据格式异常: " + line);
        }
        DailyKlineEntity entity = new DailyKlineEntity();
        entity.setSymbolCode(symbolCode);
        entity.setTradeDate(LocalDate.parse(parts[0]));
        entity.setOpenPrice(new BigDecimal(parts[1]));
        entity.setClosePrice(new BigDecimal(parts[2]));
        entity.setHighPrice(new BigDecimal(parts[3]));
        entity.setLowPrice(new BigDecimal(parts[4]));
        entity.setVolume(new BigDecimal(parts[5]).multiply(BigDecimal.valueOf(100)));
        entity.setAmount(new BigDecimal(parts[6]));
        return entity;
    }

    private MarketKlineDTO parseChartLine(String line) {
        String[] parts = line.split(",", -1);
        if (parts.length < 7) {
            throw new BusinessException("东方财富 K 线数据格式异常: " + line);
        }
        MarketKlineDTO dto = new MarketKlineDTO();
        dto.setTimestamp(parseTimestamp(parts[0]));
        dto.setOpen(new BigDecimal(parts[1]));
        dto.setClose(new BigDecimal(parts[2]));
        dto.setHigh(new BigDecimal(parts[3]));
        dto.setLow(new BigDecimal(parts[4]));
        dto.setVolume(optionalDecimal(parts[5], true));
        dto.setTurnover(optionalDecimal(parts[6], false));
        return dto;
    }

    private Long parseTimestamp(String value) {
        String text = value.trim();
        if (text.length() > 10) {
            return LocalDateTime.parse(text, DATE_TIME).atZone(CHINA_ZONE).toInstant().toEpochMilli();
        }
        return LocalDate.parse(text).atStartOfDay(CHINA_ZONE).toInstant().toEpochMilli();
    }

    private BigDecimal optionalDecimal(String value, boolean multiplyBy100) {
        if (value == null || value.isBlank()) {
            return null;
        }
        BigDecimal decimal = new BigDecimal(value.trim());
        return multiplyBy100 ? decimal.multiply(BigDecimal.valueOf(100)) : decimal;
    }

    private List<MarketKlineDTO> aggregateByHour(List<MarketKlineDTO> rows, int hours) {
        Map<Long, MarketKlineDTO> grouped = new LinkedHashMap<>();
        for (MarketKlineDTO row : rows) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(row.getTimestamp()), CHINA_ZONE);
            int bucketHour = dateTime.getHour() / hours * hours;
            long bucket = dateTime.toLocalDate().atTime(bucketHour, 0).atZone(CHINA_ZONE).toInstant().toEpochMilli();
            merge(grouped, bucket, row);
        }
        return new ArrayList<>(grouped.values());
    }

    private List<MarketKlineDTO> aggregateByYear(List<MarketKlineDTO> rows) {
        Map<Long, MarketKlineDTO> grouped = new LinkedHashMap<>();
        for (MarketKlineDTO row : rows) {
            LocalDateTime dateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(row.getTimestamp()), CHINA_ZONE);
            long bucket = LocalDate.of(dateTime.getYear(), 1, 1).atStartOfDay(CHINA_ZONE).toInstant().toEpochMilli();
            merge(grouped, bucket, row);
        }
        return new ArrayList<>(grouped.values());
    }

    private void merge(Map<Long, MarketKlineDTO> grouped, long bucket, MarketKlineDTO row) {
        MarketKlineDTO existing = grouped.get(bucket);
        if (existing == null) {
            MarketKlineDTO copy = new MarketKlineDTO();
            copy.setTimestamp(bucket);
            copy.setOpen(row.getOpen());
            copy.setClose(row.getClose());
            copy.setHigh(row.getHigh());
            copy.setLow(row.getLow());
            copy.setVolume(row.getVolume());
            copy.setTurnover(row.getTurnover());
            grouped.put(bucket, copy);
            return;
        }
        existing.setClose(row.getClose());
        if (row.getHigh() != null && (existing.getHigh() == null || row.getHigh().compareTo(existing.getHigh()) > 0)) {
            existing.setHigh(row.getHigh());
        }
        if (row.getLow() != null && (existing.getLow() == null || row.getLow().compareTo(existing.getLow()) < 0)) {
            existing.setLow(row.getLow());
        }
        existing.setVolume(sum(existing.getVolume(), row.getVolume()));
        existing.setTurnover(sum(existing.getTurnover(), row.getTurnover()));
    }

    private BigDecimal sum(BigDecimal left, BigDecimal right) {
        if (left == null) {
            return right;
        }
        if (right == null) {
            return left;
        }
        return left.add(right);
    }

    private String normalizePeriod(String period) {
        String value = period == null || period.isBlank() ? "D" : period.trim();
        if ("1M".equalsIgnoreCase(value)) {
            return "1m";
        }
        if ("5M".equalsIgnoreCase(value)) {
            return "5m";
        }
        if ("15M".equalsIgnoreCase(value)) {
            return "15m";
        }
        if ("1H".equalsIgnoreCase(value)) {
            return "1h";
        }
        if ("2H".equalsIgnoreCase(value)) {
            return "2h";
        }
        if ("4H".equalsIgnoreCase(value)) {
            return "4h";
        }
        String upper = value.toUpperCase();
        if (List.of("D", "W", "M", "Y").contains(upper)) {
            return upper;
        }
        throw new BusinessException("不支持的 K 线周期: " + period);
    }

    private String sourceKlt(String period) {
        return switch (period) {
            case "1m" -> "1";
            case "5m" -> "5";
            case "15m" -> "15";
            case "1h", "2h", "4h" -> "60";
            case "D" -> "101";
            case "W" -> "102";
            case "M", "Y" -> "103";
            default -> throw new BusinessException("不支持的 K 线周期: " + period);
        };
    }

    private String formatDate(LocalDate date, String defaultValue) {
        return date == null ? defaultValue : date.format(COMPACT_DATE);
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
