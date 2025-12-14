package com.qualityanalyzer.service.ai;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualityanalyzer.config.AnalyzerProperties;
import com.qualityanalyzer.dto.QualityReportDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class GeminiAiService {

    private static final Logger log = LoggerFactory.getLogger(GeminiAiService.class);

    private final AnalyzerProperties properties;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public GeminiAiService(AnalyzerProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    @PostConstruct
    void logGeminiConfig() {
        boolean keySet = properties.getGemini().getApiKey() != null
                && !properties.getGemini().getApiKey().isBlank();
        log.info("Gemini AI: model={}, enabled={}, apiKeyConfigured={}",
                properties.getGemini().getModel(),
                properties.getGemini().isEnabled(),
                keySet);
    }

    public List<QualityReportDto.AiRecommendationDto> generateRecommendations(QualityReportDto report) {
        List<QualityReportDto.AiRecommendationDto> fallback = buildFallbackRecommendations(report);

        if (!properties.getGemini().isEnabled()) {
            return fallback;
        }
        String apiKey = properties.getGemini().getApiKey();
        if (apiKey == null || apiKey.isBlank()) {
            return fallback;
        }

        try {
            String prompt = buildPrompt(report);
            String response = callGemini(apiKey, prompt);
            List<QualityReportDto.AiRecommendationDto> parsed = parseResponse(response);
            if (parsed.isEmpty()) {
                log.warn("Gemini returned an empty or unparseable response, using local recommendations");
                return fallback;
            }
            log.debug("Gemini generated {} recommendations", parsed.size());
            return parsed;
        } catch (Exception e) {
            log.warn("Gemini API call failed, using local recommendations: {}", e.getMessage());
            return fallback;
        }
    }

    private String buildPrompt(QualityReportDto report) {
        StringBuilder sb = new StringBuilder();
        sb.append("You are an SEO and website quality expert. Analyze this website crawl report and provide ");
        sb.append("5 actionable recommendations. Return ONLY a JSON array with objects containing: ");
        sb.append("pageUrl, issue, suggestion, priority (HIGH/MEDIUM/LOW).\n\n");
        sb.append("Website: ").append(report.getRootUrl()).append("\n");
        sb.append("Overall Score: ").append(report.getOverallScore()).append("\n");
        sb.append("Pages crawled: ").append(report.getSummary().getTotalPages()).append("\n");
        sb.append("Broken links: ").append(report.getSummary().getBrokenLinkCount()).append("\n");
        sb.append("Thin content pages: ").append(report.getSummary().getThinContentPages()).append("\n");
        sb.append("Missing SEO pages: ").append(report.getSummary().getMissingSeoPages()).append("\n\n");

        int count = 0;
        for (QualityReportDto.PageAnalysisDto page : report.getPages()) {
            if (count++ >= 5) {
                break;
            }
            sb.append("Page: ").append(page.getUrl()).append("\n");
            sb.append("  Issues: ").append(String.join(", ", page.getIssues())).append("\n");
            sb.append("  SEO Score: ").append(page.getSeo().getSeoScore()).append("\n");
            sb.append("  Content Score: ").append(page.getContentScore()).append("\n");
        }
        return sb.toString();
    }

    private String callGemini(String apiKey, String prompt) throws Exception {
        String model = properties.getGemini().getModel();
        String url = "https://generativelanguage.googleapis.com/v1beta/models/" + model + ":generateContent?key=" + apiKey;

        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> body = Map.of("contents", List.of(content));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(url, entity, String.class);
        String responseBody = response.getBody();
        if (responseBody != null && responseBody.contains("\"error\"")) {
            JsonNode error = objectMapper.readTree(responseBody).path("error");
            throw new IllegalStateException(error.path("message").asText("Unknown Gemini API error"));
        }
        return responseBody;
    }

    private List<QualityReportDto.AiRecommendationDto> parseResponse(String responseBody) throws Exception {
        List<QualityReportDto.AiRecommendationDto> results = new ArrayList<>();
        if (responseBody == null) {
            return results;
        }

        JsonNode root = objectMapper.readTree(responseBody);
        JsonNode candidates = root.path("candidates");
        if (!candidates.isArray() || candidates.isEmpty()) {
            return results;
        }

        String text = candidates.get(0).path("content").path("parts").get(0).path("text").asText("");
        int start = text.indexOf('[');
        int end = text.lastIndexOf(']');
        if (start < 0 || end < 0) {
            return results;
        }

        JsonNode array = objectMapper.readTree(text.substring(start, end + 1));
        for (JsonNode node : array) {
            QualityReportDto.AiRecommendationDto dto = new QualityReportDto.AiRecommendationDto();
            dto.setPageUrl(node.path("pageUrl").asText(""));
            dto.setIssue(node.path("issue").asText(""));
            dto.setSuggestion(node.path("suggestion").asText(""));
            dto.setPriority(node.path("priority").asText("MEDIUM"));
            if (!dto.getIssue().isBlank()) {
                results.add(dto);
            }
        }
        return results;
    }

    private List<QualityReportDto.AiRecommendationDto> buildFallbackRecommendations(QualityReportDto report) {
        List<QualityReportDto.AiRecommendationDto> recommendations = new ArrayList<>();

        for (QualityReportDto.PageAnalysisDto page : report.getPages()) {
            if (recommendations.size() >= 8) {
                break;
            }
            for (String issue : page.getIssues()) {
                QualityReportDto.AiRecommendationDto dto = new QualityReportDto.AiRecommendationDto();
                dto.setPageUrl(page.getUrl());
                dto.setIssue(issue);
                dto.setSuggestion(generateLocalSuggestion(issue, page));
                dto.setPriority(issue.toLowerCase().contains("missing") ? "HIGH" : "MEDIUM");
                recommendations.add(dto);
                if (recommendations.size() >= 8) {
                    break;
                }
            }
        }

        if (report.getSummary().getBrokenLinkCount() > 0) {
            QualityReportDto.AiRecommendationDto dto = new QualityReportDto.AiRecommendationDto();
            dto.setPageUrl(report.getRootUrl());
            dto.setIssue("Found " + report.getSummary().getBrokenLinkCount() + " broken links");
            dto.setSuggestion("Audit and fix or remove broken links to improve user experience and SEO crawlability.");
            dto.setPriority("HIGH");
            recommendations.add(dto);
        }

        return recommendations;
    }

    private String generateLocalSuggestion(String issue, QualityReportDto.PageAnalysisDto page) {
        String lower = issue.toLowerCase();
        if (lower.contains("meta description")) {
            String topic = page.getTitle() != null && !page.getTitle().isBlank()
                    ? page.getTitle() : "this page";
            return "Add a compelling meta description (50-160 chars) summarizing " + topic + ".";
        }
        if (lower.contains("title")) {
            return "Add a unique, descriptive title tag between 10-60 characters.";
        }
        if (lower.contains("h1")) {
            return "Ensure exactly one H1 tag that clearly describes the main topic of the page.";
        }
        if (lower.contains("alt")) {
            return "Add descriptive alt text to all images for accessibility and SEO.";
        }
        if (lower.contains("thin")) {
            return "Expand content with valuable information - aim for at least 300 words.";
        }
        return "Review and address this issue to improve overall page quality.";
    }
}
