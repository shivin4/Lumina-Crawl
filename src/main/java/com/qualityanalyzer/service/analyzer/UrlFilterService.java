package com.qualityanalyzer.service.analyzer;

import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
public class UrlFilterService {

    private static final List<FilterRule> IRRELEVANT_RULES = List.of(
            new FilterRule(".*login.*", "Login page"),
            new FilterRule(".*sign-?in.*", "Sign-in page"),
            new FilterRule(".*cart.*", "Shopping cart"),
            new FilterRule(".*checkout.*", "Checkout page"),
            new FilterRule(".*\\?sessionid=.*", "Session ID parameter"),
            new FilterRule(".*\\?session=.*", "Session parameter"),
            new FilterRule(".*logout.*", "Logout page"),
            new FilterRule(".*wp-admin.*", "Admin panel"),
            new FilterRule(".*\\.pdf$", "PDF document"),
            new FilterRule(".*\\.zip$", "Archive file")
    );

    private final Map<String, Pattern> patternCache = new HashMap<>();

    public Classification classify(String url) {
        for (FilterRule rule : IRRELEVANT_RULES) {
            Pattern pattern = patternCache.computeIfAbsent(rule.pattern, Pattern::compile);
            if (pattern.matcher(url).matches()) {
                return new Classification(url, "IRRELEVANT", rule.description);
            }
        }
        return new Classification(url, "USEFUL", null);
    }

    public List<Classification> classifyAll(List<String> urls) {
        Map<String, Classification> seen = new HashMap<>();
        List<Classification> results = new ArrayList<>();
        for (String url : urls) {
            Classification c = classify(url);
            if ("USEFUL".equals(c.classification) && seen.containsKey(normalizeForDuplicate(url))) {
                results.add(new Classification(url, "DUPLICATE", "Normalized URL already seen"));
            } else {
                results.add(c);
                if ("USEFUL".equals(c.classification)) {
                    seen.put(normalizeForDuplicate(url), c);
                }
            }
        }
        return results;
    }

    private String normalizeForDuplicate(String url) {
        return url.replaceAll("/$", "").toLowerCase().split("\\?")[0];
    }

    private record FilterRule(String pattern, String description) {
    }

    public static class Classification {
        private final String url;
        private final String classification;
        private final String matchedRule;

        public Classification(String url, String classification, String matchedRule) {
            this.url = url;
            this.classification = classification;
            this.matchedRule = matchedRule;
        }

        public String getUrl() {
            return url;
        }

        public String getClassification() {
            return classification;
        }

        public String getMatchedRule() {
            return matchedRule;
        }
    }
}
