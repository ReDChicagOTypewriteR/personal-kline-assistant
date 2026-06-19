package com.alexjoker.quant.llm.client;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.alexjoker.quant.llm.config.DailyStockAnalysisProperties;
import com.alexjoker.quant.llm.dto.LlmRiskAnalysisRequest;
import com.alexjoker.quant.llm.dto.LlmRiskAnalysisResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class LlmAnalysisClient {
    private final DailyStockAnalysisProperties properties;
    private final ObjectMapper objectMapper;

    public LlmRiskAnalysisResponse analyzeRisk(LlmRiskAnalysisRequest request) {
        if (!properties.isEnabled()) {
            throw new LlmAnalysisException("LLM 分析服务未启用");
        }
        RestClient client = restClient();
        try {
            LlmRiskAnalysisResponse response = client
                    .post()
                    .uri(properties.getRiskAnalysisPath())
                    .body(request)
                    .retrieve()
                    .body(LlmRiskAnalysisResponse.class);
            if (response == null) {
                throw new LlmAnalysisException("LLM 分析服务返回空响应");
            }
            return response;
        } catch (ResourceAccessException ex) {
            throw new LlmAnalysisException("LLM 分析服务不可用或请求超时", ex);
        } catch (HttpClientErrorException ex) {
            if (shouldFallbackToNativeAnalysis(ex)) {
                return analyzeRiskByNativeAnalysisApi(client, request);
            }
            throw new LlmAnalysisException("LLM 分析服务返回异常: " + ex.getMessage(), ex);
        } catch (RestClientException ex) {
            throw new LlmAnalysisException("LLM 分析服务返回异常: " + ex.getMessage(), ex);
        }
    }

    private LlmRiskAnalysisResponse analyzeRiskByNativeAnalysisApi(RestClient client, LlmRiskAnalysisRequest request) {
        try {
            JsonNode response = client
                    .post()
                    .uri(properties.getFallbackAnalysisPath())
                    .body(Map.of(
                            "stock_code", request.getSymbolCode(),
                            "report_type", "detailed",
                            "force_refresh", false,
                            "async_mode", false,
                            "notify", false
                    ))
                    .retrieve()
                    .body(JsonNode.class);
            if (response == null) {
                throw new LlmAnalysisException("LLM 原生分析接口返回空响应");
            }
            return convertNativeAnalysisResponse(request, response);
        } catch (ResourceAccessException ex) {
            throw new LlmAnalysisException("LLM 分析服务不可用或请求超时", ex);
        } catch (RestClientException ex) {
            throw new LlmAnalysisException("LLM 分析服务返回异常: " + ex.getMessage(), ex);
        }
    }

    private LlmRiskAnalysisResponse convertNativeAnalysisResponse(LlmRiskAnalysisRequest request, JsonNode response) {
        JsonNode report = response.path("report");
        JsonNode summary = report.path("summary");
        JsonNode details = report.path("details");
        JsonNode dashboard = report.path("dashboard");
        JsonNode intelligence = dashboard.path("intelligence");

        Integer sentimentScore = safeInt(summary.path("sentiment_score"));
        List<String> riskFactors = dedupe(toTextList(intelligence.path("risk_alerts")));
        addIfText(riskFactors, text(details, "risk_warning"));
        List<String> positiveFactors = dedupe(toTextList(intelligence.path("positive_catalysts")));
        addIfText(positiveFactors, text(details, "technical_analysis"));
        addIfText(positiveFactors, text(details, "fundamental_analysis"));
        List<String> negativeFactors = new ArrayList<>();
        addIfText(negativeFactors, text(details, "risk_warning"));
        addIfText(negativeFactors, text(details, "news_summary"));
        negativeFactors = dedupe(negativeFactors);

        int riskScore = calculateRiskScore(
                sentimentScore,
                riskFactors,
                text(summary, "operation_advice"),
                request.getTechnicalScore()
        );

        LlmRiskAnalysisResponse result = new LlmRiskAnalysisResponse();
        result.setSymbolCode(request.getSymbolCode());
        result.setAnalysisDate(request.getTradeDate());
        result.setSentimentScore(sentimentScore);
        result.setRiskScore(riskScore);
        result.setRiskLevel(riskLevel(riskScore));
        result.setMarketState(marketState(sentimentScore));
        result.setActionConstraint(actionConstraint(riskScore));
        result.setSummary(firstText(
                text(summary, "analysis_summary"),
                text(summary, "operation_advice"),
                "daily_stock_analysis 已返回分析结果，但摘要字段为空。"
        ));
        result.setPositiveFactors(limit(positiveFactors, 5));
        result.setNegativeFactors(limit(negativeFactors, 5));
        result.setRiskFactors(limit(riskFactors, 8));
        result.setContrarianView(firstText(text(details, "risk_warning"), defaultContrarianView(result.getRiskLevel())));
        result.setRawReport(renderMarkdown(request, response, result));
        return result;
    }

    private boolean shouldFallbackToNativeAnalysis(HttpClientErrorException ex) {
        int statusCode = ex.getStatusCode().value();
        return statusCode == 404 || statusCode == 405;
    }

    private RestClient restClient() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(properties.getConnectTimeoutSeconds()));
        factory.setReadTimeout(Duration.ofSeconds(properties.getReadTimeoutSeconds()));
        return RestClient.builder()
                .baseUrl(trimRight(properties.getBaseUrl()))
                .requestFactory(factory)
                .build();
    }

    private Integer safeInt(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        try {
            int value = node.isNumber() ? node.asInt() : (int) Double.parseDouble(node.asText());
            return Math.max(0, Math.min(100, value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private String text(JsonNode node, String fieldName) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        JsonNode value = node.path(fieldName);
        if (value.isMissingNode() || value.isNull()) {
            return null;
        }
        String text = value.asText(null);
        return text == null || text.isBlank() ? null : text.trim();
    }

    private List<String> toTextList(JsonNode node) {
        List<String> values = new ArrayList<>();
        if (node == null || node.isMissingNode() || node.isNull()) {
            return values;
        }
        if (node.isArray()) {
            node.forEach(item -> addIfText(values, item.asText(null)));
            return values;
        }
        addIfText(values, node.asText(null));
        return values;
    }

    private void addIfText(List<String> values, String value) {
        if (value != null && !value.isBlank()) {
            values.add(value.trim());
        }
    }

    private List<String> dedupe(List<String> values) {
        return new ArrayList<>(new LinkedHashSet<>(values));
    }

    private List<String> limit(List<String> values, int maxSize) {
        if (values.size() <= maxSize) {
            return values;
        }
        return new ArrayList<>(values.subList(0, maxSize));
    }

    private int calculateRiskScore(
            Integer sentimentScore,
            List<String> riskFactors,
            String operationAdvice,
            Integer technicalScore
    ) {
        int score = sentimentScore == null ? 50 : 100 - sentimentScore;
        score += Math.min(30, riskFactors.size() * 8);
        String advice = operationAdvice == null ? "" : operationAdvice.toLowerCase();
        if (advice.contains("卖") || advice.contains("减") || advice.contains("观望")
                || advice.contains("sell") || advice.contains("reduce") || advice.contains("wait")) {
            score += 10;
        }
        if (technicalScore != null && technicalScore < 80) {
            score += 5;
        }
        return Math.max(0, Math.min(100, score));
    }

    private String riskLevel(Integer riskScore) {
        if (riskScore == null) {
            return "UNKNOWN";
        }
        if (riskScore >= 70) {
            return "HIGH";
        }
        if (riskScore >= 40) {
            return "MEDIUM";
        }
        return "LOW";
    }

    private String marketState(Integer sentimentScore) {
        if (sentimentScore == null) {
            return "UNKNOWN";
        }
        if (sentimentScore >= 65) {
            return "STRONG";
        }
        if (sentimentScore >= 40) {
            return "NEUTRAL";
        }
        return "WEAK";
    }

    private String actionConstraint(Integer riskScore) {
        if (riskScore == null) {
            return "UNKNOWN";
        }
        if (riskScore >= 70) {
            return "BLOCK";
        }
        if (riskScore >= 40) {
            return "WATCH_OR_HALF_POSITION";
        }
        return "ALLOW";
    }

    private String firstText(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String defaultContrarianView(String riskLevel) {
        if ("HIGH".equals(riskLevel)) {
            return "技术信号成立，但消息面或市场风险偏高，需警惕假突破。";
        }
        if ("MEDIUM".equals(riskLevel)) {
            return "当前上涨可能仍是短期技术反弹，还不能确认趋势完全反转。";
        }
        return "即使风险较低，也需要等待成交量和后续价格行为继续确认。";
    }

    private String renderMarkdown(
            LlmRiskAnalysisRequest request,
            JsonNode nativeResponse,
            LlmRiskAnalysisResponse riskResponse
    ) {
        StringBuilder builder = new StringBuilder();
        builder.append("# ")
                .append(firstText(request.getAssetName(), request.getSymbolCode()))
                .append(" AI 风险过滤报告\n\n");
        builder.append("- 标的：").append(request.getSymbolCode()).append('\n');
        builder.append("- 日期：").append(request.getTradeDate()).append('\n');
        builder.append("- K线信号：").append(request.getTechnicalSignal()).append('\n');
        builder.append("- 技术评分：").append(request.getTechnicalScore() == null ? "N/A" : request.getTechnicalScore()).append('\n');
        builder.append("- AI 情绪分：").append(riskResponse.getSentimentScore() == null ? "N/A" : riskResponse.getSentimentScore()).append('\n');
        builder.append("- AI 风险分：").append(riskResponse.getRiskScore() == null ? "N/A" : riskResponse.getRiskScore()).append('\n');
        builder.append("- AI 风险等级：").append(riskResponse.getRiskLevel()).append("\n\n");
        builder.append("## 摘要\n").append(firstText(riskResponse.getSummary(), "暂无摘要")).append("\n\n");
        appendSection(builder, "利好因素", riskResponse.getPositiveFactors());
        appendSection(builder, "利空因素", riskResponse.getNegativeFactors());
        appendSection(builder, "风险因素", riskResponse.getRiskFactors());
        builder.append("## 反方观点\n").append(firstText(riskResponse.getContrarianView(), "暂无反方观点")).append("\n\n");
        builder.append("## daily_stock_analysis 原始响应\n");
        builder.append("```json\n").append(toPrettyJson(nativeResponse)).append("\n```\n");
        return builder.toString();
    }

    private void appendSection(StringBuilder builder, String title, List<String> values) {
        builder.append("## ").append(title).append('\n');
        if (values == null || values.isEmpty()) {
            builder.append("- 暂无\n\n");
            return;
        }
        for (String value : values) {
            builder.append("- ").append(value).append('\n');
        }
        builder.append('\n');
    }

    private String toPrettyJson(JsonNode node) {
        try {
            return objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(node);
        } catch (JsonProcessingException ex) {
            return String.valueOf(node);
        }
    }

    private String trimRight(String value) {
        if (value == null || value.isBlank()) {
            return "http://localhost:8000";
        }
        return value.endsWith("/") ? value.substring(0, value.length() - 1) : value;
    }
}
