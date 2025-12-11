package com.qualityanalyzer.service.analyzer;

import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ContentQualityService {

    private static final int THIN_CONTENT_THRESHOLD = 300;
    private static final Set<String> STOP_WORDS = new HashSet<>(Arrays.asList(
            "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for", "of", "with",
            "by", "from", "is", "are", "was", "were", "be", "been", "being", "have", "has", "had",
            "do", "does", "did", "will", "would", "could", "should", "may", "might", "this", "that",
            "these", "those", "it", "its", "as", "not", "no", "can", "we", "you", "your", "our", "they"
    ));

    public ContentAnalysis analyze(String text, String title) {
        ContentAnalysis result = new ContentAnalysis();
        if (text == null) {
            text = "";
        }

        String[] words = text.trim().split("\\s+");
        int wordCount = text.isBlank() ? 0 : words.length;
        result.setWordCount(wordCount);
        result.setThinContent(wordCount < THIN_CONTENT_THRESHOLD);
        result.setReadabilityScore(calculateFleschReadingEase(text, wordCount));
        result.setKeywordDensity(calculateKeywordDensity(words));
        result.setContentCategory(categorize(text, title));
        result.setContentScore(calculateContentScore(result));
        return result;
    }

    public double calculateSimilarity(String text1, String text2) {
        if (text1 == null || text2 == null || text1.isBlank() || text2.isBlank()) {
            return 0;
        }
        Set<String> words1 = tokenize(text1);
        Set<String> words2 = tokenize(text2);
        if (words1.isEmpty() || words2.isEmpty()) {
            return 0;
        }
        Set<String> intersection = new HashSet<>(words1);
        intersection.retainAll(words2);
        Set<String> union = new HashSet<>(words1);
        union.addAll(words2);
        return union.isEmpty() ? 0 : (intersection.size() * 100.0) / union.size();
    }

    private Set<String> tokenize(String text) {
        return Arrays.stream(text.toLowerCase(Locale.ROOT).split("\\W+"))
                .filter(w -> w.length() > 2 && !STOP_WORDS.contains(w))
                .collect(Collectors.toSet());
    }

    private double calculateFleschReadingEase(String text, int wordCount) {
        if (wordCount == 0) {
            return 0;
        }
        int sentenceCount = Math.max(1, text.split("[.!?]+").length);
        int syllableCount = estimateSyllables(text);
        double wordsPerSentence = (double) wordCount / sentenceCount;
        double syllablesPerWord = (double) syllableCount / wordCount;
        double score = 206.835 - (1.015 * wordsPerSentence) - (84.6 * syllablesPerWord);
        return Math.max(0, Math.min(100, score));
    }

    private int estimateSyllables(String text) {
        String[] words = text.toLowerCase(Locale.ROOT).split("\\s+");
        int count = 0;
        for (String word : words) {
            word = word.replaceAll("[^a-z]", "");
            if (word.isEmpty()) {
                continue;
            }
            int syllables = word.replaceAll("[aeiouy]+", "a").replaceAll("a", "").length();
            syllables = Math.max(1, syllables + 1);
            count += syllables;
        }
        return Math.max(1, count);
    }

    private double calculateKeywordDensity(String[] words) {
        if (words.length == 0) {
            return 0;
        }
        Map<String, Integer> freq = new HashMap<>();
        for (String word : words) {
            String w = word.toLowerCase(Locale.ROOT).replaceAll("[^a-z]", "");
            if (w.length() > 3 && !STOP_WORDS.contains(w)) {
                freq.merge(w, 1, Integer::sum);
            }
        }
        if (freq.isEmpty()) {
            return 0;
        }
        int maxFreq = freq.values().stream().mapToInt(Integer::intValue).max().orElse(0);
        return (maxFreq * 100.0) / words.length;
    }

    private String categorize(String text, String title) {
        String combined = ((title != null ? title : "") + " " + (text != null ? text : "")).toLowerCase(Locale.ROOT);
        if (combined.matches(".*(blog|article|post|news|story).*")) {
            return "Blog/Article";
        }
        if (combined.matches(".*(product|shop|buy|price|cart|store).*")) {
            return "E-commerce";
        }
        if (combined.matches(".*(about|team|company|mission|history).*")) {
            return "About/Company";
        }
        if (combined.matches(".*(contact|support|help|faq).*")) {
            return "Support/Contact";
        }
        if (combined.matches(".*(login|sign|register|account|profile).*")) {
            return "Authentication";
        }
        if (combined.matches(".*(documentation|docs|api|guide|tutorial).*")) {
            return "Documentation";
        }
        return "General";
    }

    private int calculateContentScore(ContentAnalysis analysis) {
        int score = 100;
        if (analysis.isThinContent()) {
            score -= 30;
        }
        if (analysis.getWordCount() < 100) {
            score -= 20;
        }
        if (analysis.getReadabilityScore() < 30) {
            score -= 15;
        } else if (analysis.getReadabilityScore() < 50) {
            score -= 5;
        }
        if (analysis.getKeywordDensity() > 5) {
            score -= 10;
        }
        return Math.max(0, Math.min(100, score));
    }

    public static class ContentAnalysis {
        private int wordCount;
        private double readabilityScore;
        private double keywordDensity;
        private boolean thinContent;
        private String contentCategory;
        private int contentScore;

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

        public int getContentScore() {
            return contentScore;
        }

        public void setContentScore(int contentScore) {
            this.contentScore = contentScore;
        }
    }
}
