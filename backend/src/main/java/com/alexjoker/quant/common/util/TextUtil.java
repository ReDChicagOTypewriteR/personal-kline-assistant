package com.alexjoker.quant.common.util;

import java.util.Arrays;
import java.util.List;

public final class TextUtil {
    private TextUtil() {
    }

    public static String normalizeCode(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }

    public static String upperOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim().toUpperCase();
    }

    public static String cleanOrNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    public static String defaultText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim();
    }

    public static String defaultUpper(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toUpperCase();
    }

    public static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(value, max));
    }

    public static List<String> splitLines(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split("\\R"))
                .map(String::trim)
                .filter(item -> !item.isBlank())
                .toList();
    }
}
