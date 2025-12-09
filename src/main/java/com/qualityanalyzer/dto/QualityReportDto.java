package com.qualityanalyzer.dto;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class QualityReportDto {

    private String rootUrl;
    private String crawlStrategy;
    private int overallScore;
    private SummaryDto summary = new SummaryDto();
    private List<PageAnalysisDto> pages = new ArrayList<>();
    private List<BrokenLinkDto> brokenLinks = new ArrayList<>();
    private List<DuplicatePageDto> duplicatePages = new ArrayList<>();
    private List<UrlClassificationDto> urlClassifications = new ArrayList<>();
    private SiteTreeNodeDto siteTree = new SiteTreeNodeDto();
    private List<AiRecommendationDto> aiRecommendations = new ArrayList<>();
    private Map<String, Integer> crawlDepthDistribution = new HashMap<>();
    private Map<String, Integer> contentCategories = new HashMap<>();
    private List<String> warnings = new ArrayList<>();

    public String getRootUrl() {
        return rootUrl;
    }

    public void setRootUrl(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    public String getCrawlStrategy() {
        return crawlStrategy;
    }

    public void setCrawlStrategy(String crawlStrategy) {
        this.crawlStrategy = crawlStrategy;
    }

    public int getOverallScore() {
        return overallScore;
    }

    public void setOverallScore(int overallScore) {
        this.overallScore = overallScore;
    }

    public SummaryDto getSummary() {
        return summary;
    }

    public void setSummary(SummaryDto summary) {
        this.summary = summary;
    }

    public List<PageAnalysisDto> getPages() {
        return pages;
    }

    public void setPages(List<PageAnalysisDto> pages) {
        this.pages = pages;
    }

    public List<BrokenLinkDto> getBrokenLinks() {
        return brokenLinks;
    }

    public void setBrokenLinks(List<BrokenLinkDto> brokenLinks) {
        this.brokenLinks = brokenLinks;
    }

    public List<DuplicatePageDto> getDuplicatePages() {
        return duplicatePages;
    }

    public void setDuplicatePages(List<DuplicatePageDto> duplicatePages) {
        this.duplicatePages = duplicatePages;
    }

    public List<UrlClassificationDto> getUrlClassifications() {
        return urlClassifications;
    }

    public void setUrlClassifications(List<UrlClassificationDto> urlClassifications) {
        this.urlClassifications = urlClassifications;
    }

    public SiteTreeNodeDto getSiteTree() {
        return siteTree;
    }

    public void setSiteTree(SiteTreeNodeDto siteTree) {
        this.siteTree = siteTree;
    }

    public List<AiRecommendationDto> getAiRecommendations() {
        return aiRecommendations;
    }

    public void setAiRecommendations(List<AiRecommendationDto> aiRecommendations) {
        this.aiRecommendations = aiRecommendations;
    }

    public Map<String, Integer> getCrawlDepthDistribution() {
        return crawlDepthDistribution;
    }

    public void setCrawlDepthDistribution(Map<String, Integer> crawlDepthDistribution) {
        this.crawlDepthDistribution = crawlDepthDistribution;
    }

    public Map<String, Integer> getContentCategories() {
        return contentCategories;
    }

    public void setContentCategories(Map<String, Integer> contentCategories) {
        this.contentCategories = contentCategories;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public void setWarnings(List<String> warnings) {
        this.warnings = warnings;
    }

    public static class SummaryDto {
        private int totalPages;
        private int internalLinks;
        private int externalLinks;
        private int brokenLinkCount;
        private int duplicatePageCount;
        private int thinContentPages;
        private int missingSeoPages;
        private double averageUrlScore;
        private double averageContentScore;
        private int maxCrawlDepth;

        public int getTotalPages() {
            return totalPages;
        }

        public void setTotalPages(int totalPages) {
            this.totalPages = totalPages;
        }

        public int getInternalLinks() {
            return internalLinks;
        }

        public void setInternalLinks(int internalLinks) {
            this.internalLinks = internalLinks;
        }

        public int getExternalLinks() {
            return externalLinks;
        }

        public void setExternalLinks(int externalLinks) {
            this.externalLinks = externalLinks;
        }

        public int getBrokenLinkCount() {
            return brokenLinkCount;
        }

        public void setBrokenLinkCount(int brokenLinkCount) {
            this.brokenLinkCount = brokenLinkCount;
        }

        public int getDuplicatePageCount() {
            return duplicatePageCount;
        }

        public void setDuplicatePageCount(int duplicatePageCount) {
            this.duplicatePageCount = duplicatePageCount;
        }

        public int getThinContentPages() {
            return thinContentPages;
        }

        public void setThinContentPages(int thinContentPages) {
            this.thinContentPages = thinContentPages;
        }

        public int getMissingSeoPages() {
            return missingSeoPages;
        }

        public void setMissingSeoPages(int missingSeoPages) {
            this.missingSeoPages = missingSeoPages;
        }

        public double getAverageUrlScore() {
            return averageUrlScore;
        }

        public void setAverageUrlScore(double averageUrlScore) {
            this.averageUrlScore = averageUrlScore;
        }

        public double getAverageContentScore() {
            return averageContentScore;
        }

        public void setAverageContentScore(double averageContentScore) {
            this.averageContentScore = averageContentScore;
        }

        public int getMaxCrawlDepth() {
            return maxCrawlDepth;
        }

        public void setMaxCrawlDepth(int maxCrawlDepth) {
            this.maxCrawlDepth = maxCrawlDepth;
        }
    }

    public static class PageAnalysisDto {
        private String url;
        private String title;
        private int depth;
        private int urlScore;
        private int contentScore;
        private int wordCount;
        private double readabilityScore;
        private double keywordDensity;
        private boolean thinContent;
        private String contentCategory;
        private SeoAnalysisDto seo = new SeoAnalysisDto();
        private List<String> issues = new ArrayList<>();

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }

        public int getUrlScore() {
            return urlScore;
        }

        public void setUrlScore(int urlScore) {
            this.urlScore = urlScore;
        }

        public int getContentScore() {
            return contentScore;
        }

        public void setContentScore(int contentScore) {
            this.contentScore = contentScore;
        }

        public int getWordCount() {
            return wordCount;
        }

        public void setWordCount(int wordCount) {
            this.wordCount = wordCount;
        }

        public double getReadabilityScore() {
            return readabilityScore;
        }

        public void setReadabilityScore(double readabilityScore) {
            this.readabilityScore = readabilityScore;
        }

        public double getKeywordDensity() {
            return keywordDensity;
        }

        public void setKeywordDensity(double keywordDensity) {
            this.keywordDensity = keywordDensity;
        }

        public boolean isThinContent() {
            return thinContent;
        }

        public void setThinContent(boolean thinContent) {
            this.thinContent = thinContent;
        }

        public String getContentCategory() {
            return contentCategory;
        }

        public void setContentCategory(String contentCategory) {
            this.contentCategory = contentCategory;
        }

        public SeoAnalysisDto getSeo() {
            return seo;
        }

        public void setSeo(SeoAnalysisDto seo) {
            this.seo = seo;
        }

        public List<String> getIssues() {
            return issues;
        }

        public void setIssues(List<String> issues) {
            this.issues = issues;
        }
    }

    public static class SeoAnalysisDto {
        private boolean hasTitle;
        private boolean hasMetaDescription;
        private int h1Count;
        private int h2Count;
        private int imagesWithoutAlt;
        private int totalImages;
        private int seoScore;

        public boolean isHasTitle() {
            return hasTitle;
        }

        public void setHasTitle(boolean hasTitle) {
            this.hasTitle = hasTitle;
        }

        public boolean isHasMetaDescription() {
            return hasMetaDescription;
        }

        public void setHasMetaDescription(boolean hasMetaDescription) {
            this.hasMetaDescription = hasMetaDescription;
        }

        public int getH1Count() {
            return h1Count;
        }

        public void setH1Count(int h1Count) {
            this.h1Count = h1Count;
        }

        public int getH2Count() {
            return h2Count;
        }

        public void setH2Count(int h2Count) {
            this.h2Count = h2Count;
        }

        public int getImagesWithoutAlt() {
            return imagesWithoutAlt;
        }

        public void setImagesWithoutAlt(int imagesWithoutAlt) {
            this.imagesWithoutAlt = imagesWithoutAlt;
        }

        public int getTotalImages() {
            return totalImages;
        }

        public void setTotalImages(int totalImages) {
            this.totalImages = totalImages;
        }

        public int getSeoScore() {
            return seoScore;
        }

        public void setSeoScore(int seoScore) {
            this.seoScore = seoScore;
        }
    }

    public static class BrokenLinkDto {
        private String sourceUrl;
        private String brokenUrl;
        private int statusCode;
        private String linkType;

        public String getSourceUrl() {
            return sourceUrl;
        }

        public void setSourceUrl(String sourceUrl) {
            this.sourceUrl = sourceUrl;
        }

        public String getBrokenUrl() {
            return brokenUrl;
        }

        public void setBrokenUrl(String brokenUrl) {
            this.brokenUrl = brokenUrl;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public void setStatusCode(int statusCode) {
            this.statusCode = statusCode;
        }

        public String getLinkType() {
            return linkType;
        }

        public void setLinkType(String linkType) {
            this.linkType = linkType;
        }
    }

    public static class DuplicatePageDto {
        private String url1;
        private String url2;
        private double similarityPercent;

        public String getUrl1() {
            return url1;
        }

        public void setUrl1(String url1) {
            this.url1 = url1;
        }

        public String getUrl2() {
            return url2;
        }

        public void setUrl2(String url2) {
            this.url2 = url2;
        }

        public double getSimilarityPercent() {
            return similarityPercent;
        }

        public void setSimilarityPercent(double similarityPercent) {
            this.similarityPercent = similarityPercent;
        }
    }

    public static class UrlClassificationDto {
        private String url;
        private String classification;
        private String matchedRule;

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public String getClassification() {
            return classification;
        }

        public void setClassification(String classification) {
            this.classification = classification;
        }

        public String getMatchedRule() {
            return matchedRule;
        }

        public void setMatchedRule(String matchedRule) {
            this.matchedRule = matchedRule;
        }
    }

    public static class SiteTreeNodeDto {
        private String name;
        private String url;
        private int depth;
        private List<SiteTreeNodeDto> children = new ArrayList<>();

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getDepth() {
            return depth;
        }

        public void setDepth(int depth) {
            this.depth = depth;
        }

        public List<SiteTreeNodeDto> getChildren() {
            return children;
        }

        public void setChildren(List<SiteTreeNodeDto> children) {
            this.children = children;
        }
    }

    public static class AiRecommendationDto {
        private String pageUrl;
        private String issue;
        private String suggestion;
        private String priority;

        public String getPageUrl() {
            return pageUrl;
        }

        public void setPageUrl(String pageUrl) {
            this.pageUrl = pageUrl;
        }

        public String getIssue() {
            return issue;
        }

        public void setIssue(String issue) {
            this.issue = issue;
        }

        public String getSuggestion() {
            return suggestion;
        }

        public void setSuggestion(String suggestion) {
            this.suggestion = suggestion;
        }

        public String getPriority() {
            return priority;
        }

        public void setPriority(String priority) {
            this.priority = priority;
        }
    }
}
