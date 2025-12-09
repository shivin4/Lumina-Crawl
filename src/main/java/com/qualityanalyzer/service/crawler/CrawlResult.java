package com.qualityanalyzer.service.crawler;

import org.jsoup.nodes.Document;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CrawlResult {

    private final Map<String, CrawledPage> pages = new HashMap<>();
    private final List<LinkRef> allLinks = new ArrayList<>();
    private final Map<String, String> parentMap = new HashMap<>();
    private final List<String> warnings = new ArrayList<>();
    private int sitemapUrlsUsed;

    public Map<String, CrawledPage> getPages() {
        return pages;
    }

    public List<LinkRef> getAllLinks() {
        return allLinks;
    }

    public Map<String, String> getParentMap() {
        return parentMap;
    }

    public List<String> getWarnings() {
        return warnings;
    }

    public int getSitemapUrlsUsed() {
        return sitemapUrlsUsed;
    }

    public void setSitemapUrlsUsed(int sitemapUrlsUsed) {
        this.sitemapUrlsUsed = sitemapUrlsUsed;
    }

    public void addWarning(String warning) {
        warnings.add(warning);
    }

    public static class CrawledPage {
        private final String url;
        private final int depth;
        private final int statusCode;
        private final Document document;
        private final String plainText;
        private final String title;

        public CrawledPage(String url, int depth, int statusCode, Document document, String plainText, String title) {
            this.url = url;
            this.depth = depth;
            this.statusCode = statusCode;
            this.document = document;
            this.plainText = plainText;
            this.title = title;
        }

        public String getUrl() {
            return url;
        }

        public int getDepth() {
            return depth;
        }

        public int getStatusCode() {
            return statusCode;
        }

        public Document getDocument() {
            return document;
        }

        public String getPlainText() {
            return plainText;
        }

        public String getTitle() {
            return title;
        }
    }

    public static class LinkRef {
        private final String sourceUrl;
        private final String targetUrl;
        private final boolean internal;

        public LinkRef(String sourceUrl, String targetUrl, boolean internal) {
            this.sourceUrl = sourceUrl;
            this.targetUrl = targetUrl;
            this.internal = internal;
        }

        public String getSourceUrl() {
            return sourceUrl;
        }

        public String getTargetUrl() {
            return targetUrl;
        }

        public boolean isInternal() {
            return internal;
        }
    }
}
