package com.qualityanalyzer.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public class AnalyzeRequest {

    @NotBlank(message = "URL is required")
    @Pattern(regexp = "^https?://.+", message = "URL must start with http:// or https://")
    private String url;

    @Min(1)
    @Max(200)
    private int maxPages = 50;

    @Min(1)
    @Max(10)
    private int maxDepth = 5;

    private String crawlStrategy = "BFS";

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public int getMaxPages() {
        return maxPages;
    }

    public void setMaxPages(int maxPages) {
        this.maxPages = maxPages;
    }

    public int getMaxDepth() {
        return maxDepth;
    }

    public void setMaxDepth(int maxDepth) {
        this.maxDepth = maxDepth;
    }

    public String getCrawlStrategy() {
        return crawlStrategy;
    }

    public void setCrawlStrategy(String crawlStrategy) {
        this.crawlStrategy = crawlStrategy;
    }
}
