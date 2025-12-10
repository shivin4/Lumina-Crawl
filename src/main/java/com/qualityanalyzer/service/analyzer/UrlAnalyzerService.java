package com.qualityanalyzer.service.analyzer;

import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

@Service
public class UrlAnalyzerService {

    private static final Pattern SPECIAL_CHARS = Pattern.compile("[^a-zA-Z0-9\\-_/]");
    private static final Pattern DYNAMIC_PARAMS = Pattern.compile("[?&](id|session|token|sid|uid|ref)=", Pattern.CASE_INSENSITIVE);
    private static final Pattern READABLE_SLUG = Pattern.compile("/[a-z0-9]+(?:-[a-z0-9]+)+");

    public UrlAnalysisResult analyze(String url) {
        UrlAnalysisResult result = new UrlAnalysisResult();
        result.setUrl(url);
        List<String> issues = new ArrayList<>();
        int score = 100;

        String path = extractPath(url);
        int length = url.length();
        result.setLength(length);

        if (length > 100) {
            score -= 20;
            issues.add("URL is too long (" + length + " chars)");
        } else if (length > 75) {
            score -= 10;
            issues.add("URL length is moderately long");
        }

        if (SPECIAL_CHARS.matcher(path).find()) {
            score -= 15;
            issues.add("Contains special characters in path");
        }

        if (url.contains("?")) {
            score -= 10;
            issues.add("Contains query parameters");
            if (DYNAMIC_PARAMS.matcher(url).find()) {
                score -= 15;
                issues.add("Contains dynamic/session parameters");
            }
        }

        if (url.contains("_") && !url.contains("-")) {
            score -= 5;
            issues.add("Uses underscores instead of hyphens");
        }

        if (READABLE_SLUG.matcher(path).find()) {
            score += 5;
            result.setReadable(true);
        } else if (!path.equals("/") && !path.isEmpty()) {
            score -= 5;
            issues.add("Path is not a readable slug format");
        }

        if (url.toLowerCase().contains("%20") || url.contains(" ")) {
            score -= 10;
            issues.add("Contains encoded spaces");
        }

        result.setScore(Math.max(0, Math.min(100, score)));
        result.setIssues(issues);
        return result;
    }

    private String extractPath(String url) {
        try {
            URI uri = new URI(url);
            return uri.getPath() != null ? uri.getPath() : "/";
        } catch (URISyntaxException | IllegalArgumentException e) {
            return url;
        }
    }

    public static class UrlAnalysisResult {
        private String url;
        private int score;
        private int length;
        private boolean readable;
        private List<String> issues = new ArrayList<>();

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public int getScore() {
            return score;
        }

        public void setScore(int score) {
            this.score = score;
        }

        public int getLength() {
            return length;
        }

        public void setLength(int length) {
            this.length = length;
        }

        public boolean isReadable() {
            return readable;
        }

        public void setReadable(boolean readable) {
            this.readable = readable;
        }

        public List<String> getIssues() {
            return issues;
        }

        public void setIssues(List<String> issues) {
            this.issues = issues;
        }
    }
}
