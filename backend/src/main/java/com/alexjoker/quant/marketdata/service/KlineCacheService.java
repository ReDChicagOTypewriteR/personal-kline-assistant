package com.alexjoker.quant.marketdata.service;

import com.alexjoker.quant.marketdata.config.KlineCacheProperties;
import com.alexjoker.quant.marketdata.dto.MarketKlineDTO;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class KlineCacheService {
    private static final TypeReference<List<MarketKlineDTO>> KLINE_LIST_TYPE = new TypeReference<>() {
    };

    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final KlineCacheProperties properties;

    public List<MarketKlineDTO> getChart(String symbolCode, String period, LocalDate start, LocalDate end, int adjustType) {
        if (!properties.isEnabled()) {
            return null;
        }
        try {
            String json = redisTemplate.opsForValue().get(chartKey(symbolCode, period, start, end, adjustType));
            if (json == null || json.isBlank()) {
                return null;
            }
            return objectMapper.readValue(json, KLINE_LIST_TYPE);
        } catch (Exception ignored) {
            return null;
        }
    }

    public void putChart(String symbolCode, String period, LocalDate start, LocalDate end, int adjustType, List<MarketKlineDTO> rows) {
        if (!properties.isEnabled() || rows == null || rows.isEmpty()) {
            return;
        }
        try {
            redisTemplate.opsForValue().set(
                    chartKey(symbolCode, period, start, end, adjustType),
                    objectMapper.writeValueAsString(rows),
                    ttl(end)
            );
        } catch (Exception ignored) {
            // Redis is an accelerator only. MySQL and external data sources remain the source of truth.
        }
    }

    public int evictSymbol(String symbolCode) {
        if (!properties.isEnabled()) {
            return 0;
        }
        String normalized = normalize(symbolCode);
        return deletePattern(properties.getKeyPrefix() + ":" + normalized + ":*");
    }

    public int evictAll() {
        if (!properties.isEnabled()) {
            return 0;
        }
        return deletePattern(properties.getKeyPrefix() + ":*");
    }

    public boolean isEnabled() {
        return properties.isEnabled();
    }

    private int deletePattern(String pattern) {
        try {
            Set<String> keys = redisTemplate.keys(pattern);
            if (keys == null || keys.isEmpty()) {
                return 0;
            }
            Long deleted = redisTemplate.delete(keys);
            return deleted == null ? 0 : deleted.intValue();
        } catch (Exception ignored) {
            return 0;
        }
    }

    private Duration ttl(LocalDate end) {
        LocalDate today = LocalDate.now();
        if (end == null || !end.isBefore(today)) {
            return Duration.ofMinutes(Math.max(1, properties.getLiveTtlMinutes()));
        }
        return Duration.ofHours(Math.max(1, properties.getHistoricalTtlHours()));
    }

    private String chartKey(String symbolCode, String period, LocalDate start, LocalDate end, int adjustType) {
        return String.join(":",
                properties.getKeyPrefix(),
                normalize(symbolCode),
                normalizePeriod(period),
                String.valueOf(adjustType),
                Objects.toString(start, "none"),
                Objects.toString(end, "none")
        );
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toUpperCase();
    }

    private String normalizePeriod(String value) {
        return value == null || value.isBlank() ? "D" : value.trim().toUpperCase();
    }
}
