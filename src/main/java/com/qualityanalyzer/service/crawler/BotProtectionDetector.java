package com.qualityanalyzer.service.crawler;

public final class BotProtectionDetector {

    private BotProtectionDetector() {
    }

    public static boolean isBlocked(CrawlResult.CrawledPage page) {
        if (page == null) {
            return true;
        }
        if (page.getStatusCode() == 403 || page.getStatusCode() == 401 || page.getDocument() == null) {
            return true;
        }
        String title = page.getTitle() != null ? page.getTitle().toLowerCase() : "";
        String text = page.getPlainText() != null ? page.getPlainText().toLowerCase() : "";
        String combined = title + " " + text;

        return combined.contains("enable javascript")
                || combined.contains("cloudflare")
                || combined.contains("cf-challenge")
                || combined.contains("access denied")
                || combined.contains("captcha")
                || combined.contains("bot detection")
                || (page.getDocument() != null && wordCount(page.getPlainText()) < 50
                && combined.contains("cookie"));
    }

    private static int wordCount(String text) {
        if (text == null || text.isBlank()) {
            return 0;
        }
        return text.trim().split("\\s+").length;
    }
}
