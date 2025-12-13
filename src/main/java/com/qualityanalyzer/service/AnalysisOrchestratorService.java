package com.qualityanalyzer.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qualityanalyzer.dto.AnalyzeRequest;
import com.qualityanalyzer.dto.QualityReportDto;
import com.qualityanalyzer.model.AnalysisReport;
import com.qualityanalyzer.repository.AnalysisReportRepository;
import com.qualityanalyzer.service.ai.GeminiAiService;
import com.qualityanalyzer.service.analyzer.BrokenLinkCheckerService;
import com.qualityanalyzer.service.analyzer.ContentQualityService;
import com.qualityanalyzer.service.analyzer.HtmlSeoAnalyzerService;
import com.qualityanalyzer.service.analyzer.SiteTreeBuilderService;
import com.qualityanalyzer.service.analyzer.UrlAnalyzerService;
import com.qualityanalyzer.service.analyzer.UrlFilterService;
import com.qualityanalyzer.service.crawler.CrawlResult;
import com.qualityanalyzer.service.crawler.UrlNormalizer;
import com.qualityanalyzer.service.crawler.WebCrawlerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AnalysisOrchestratorService {

    private static final Logger log = LoggerFactory.getLogger(AnalysisOrchestratorService.class);

    private final WebCrawlerService crawlerService;
    private final UrlAnalyzerService urlAnalyzerService;
    private final UrlFilterService urlFilterService;
    private final HtmlSeoAnalyzerService seoAnalyzerService;
    private final ContentQualityService contentQualityService;
    private final BrokenLinkCheckerService brokenLinkCheckerService;
    private final SiteTreeBuilderService siteTreeBuilderService;
    private final GeminiAiService geminiAiService;
    private final AnalysisReportRepository reportRepository;
    private final ObjectMapper objectMapper;

    public AnalysisOrchestratorService(
            WebCrawlerService crawlerService,
            UrlAnalyzerService urlAnalyzerService,
            UrlFilterService urlFilterService,
            HtmlSeoAnalyzerService seoAnalyzerService,
            ContentQualityService contentQualityService,
            BrokenLinkCheckerService brokenLinkCheckerService,
            SiteTreeBuilderService siteTreeBuilderService,
            GeminiAiService geminiAiService,
            AnalysisReportRepository reportRepository,
            ObjectMapper objectMapper) {
        this.crawlerService = crawlerService;
        this.urlAnalyzerService = urlAnalyzerService;
        this.urlFilterService = urlFilterService;
        this.seoAnalyzerService = seoAnalyzerService;
        this.contentQualityService = contentQualityService;
        this.brokenLinkCheckerService = brokenLinkCheckerService;
        this.siteTreeBuilderService = siteTreeBuilderService;
        this.geminiAiService = geminiAiService;
        this.reportRepository = reportRepository;
        this.objectMapper = objectMapper;
    }

    public QualityReportDto analyze(AnalyzeRequest request) {
        long start = System.currentTimeMillis();
        String rootUrl = UrlNormalizer.normalize(request.getUrl());
        log.info("Starting analysis for {} (maxPages={}, maxDepth={}, strategy={})",
                rootUrl, request.getMaxPages(), request.getMaxDepth(), request.getCrawlStrategy());

        CrawlResult crawlResult = crawlerService.crawl(
                rootUrl, request.getMaxPages(), request.getMaxDepth(), request.getCrawlStrategy());
        log.info("Crawl finished: {} pages in {}ms", crawlResult.getPages().size(), System.currentTimeMillis() - start);

        QualityReportDto report = new QualityReportDto();
        report.setRootUrl(rootUrl);
        report.setCrawlStrategy(request.getCrawlStrategy().toUpperCase());

        List<QualityReportDto.PageAnalysisDto> pages = new ArrayList<>();
        Map<String, Integer> depthDistribution = new HashMap<>();
        Map<String, Integer> categories = new HashMap<>();

        double totalUrlScore = 0;
        double totalContentScore = 0;
        int internalLinks = 0;
        int externalLinks = 0;
        int thinContent = 0;
        int missingSeo = 0;
        int maxDepth = 0;

        List<String> allUrls = new ArrayList<>(crawlResult.getPages().keySet());

        for (CrawlResult.CrawledPage crawled : crawlResult.getPages().values()) {
            QualityReportDto.PageAnalysisDto page = new QualityReportDto.PageAnalysisDto();
            page.setUrl(crawled.getUrl());
            page.setTitle(crawled.getTitle());
            page.setDepth(crawled.getDepth());
            maxDepth = Math.max(maxDepth, crawled.getDepth());

            depthDistribution.merge("Depth " + crawled.getDepth(), 1, Integer::sum);

            UrlAnalyzerService.UrlAnalysisResult urlAnalysis = urlAnalyzerService.analyze(crawled.getUrl());
            page.setUrlScore(urlAnalysis.getScore());
            totalUrlScore += urlAnalysis.getScore();

            ContentQualityService.ContentAnalysis content = contentQualityService.analyze(
                    crawled.getPlainText(), crawled.getTitle());
            page.setWordCount(content.getWordCount());
            page.setReadabilityScore(content.getReadabilityScore());
            page.setKeywordDensity(content.getKeywordDensity());
            page.setThinContent(content.isThinContent());
            page.setContentCategory(content.getContentCategory());
            page.setContentScore(content.getContentScore());
            totalContentScore += content.getContentScore();

            if (content.isThinContent()) {
                thinContent++;
            }

            categories.merge(content.getContentCategory(), 1, Integer::sum);

            QualityReportDto.SeoAnalysisDto seo = seoAnalyzerService.analyze(crawled.getDocument());
            page.setSeo(seo);
            if (seo.getSeoScore() < 70) {
                missingSeo++;
            }

            List<String> issues = new ArrayList<>(seoAnalyzerService.collectIssues(crawled.getDocument()));
            issues.addAll(urlAnalysis.getIssues());
            if (content.isThinContent()) {
                issues.add("Thin content (" + content.getWordCount() + " words)");
            }
            page.setIssues(issues);
            pages.add(page);
        }

        for (CrawlResult.LinkRef link : crawlResult.getAllLinks()) {
            if (link.isInternal()) {
                internalLinks++;
            } else {
                externalLinks++;
            }
        }

        List<QualityReportDto.DuplicatePageDto> duplicates = findDuplicates(crawlResult);
        List<QualityReportDto.BrokenLinkDto> brokenLinks = brokenLinkCheckerService.findBrokenLinks(crawlResult);

        List<UrlFilterService.Classification> classifications = urlFilterService.classifyAll(allUrls);
        List<QualityReportDto.UrlClassificationDto> urlClassifications = classifications.stream()
                .map(c -> {
                    QualityReportDto.UrlClassificationDto dto = new QualityReportDto.UrlClassificationDto();
                    dto.setUrl(c.getUrl());
                    dto.setClassification(c.getClassification());
                    dto.setMatchedRule(c.getMatchedRule());
                    return dto;
                }).toList();

        QualityReportDto.SummaryDto summary = new QualityReportDto.SummaryDto();
        int pageCount = pages.size();
        summary.setTotalPages(pageCount);
        summary.setInternalLinks(internalLinks);
        summary.setExternalLinks(externalLinks);
        summary.setBrokenLinkCount(brokenLinks.size());
        summary.setDuplicatePageCount(duplicates.size());
        summary.setThinContentPages(thinContent);
        summary.setMissingSeoPages(missingSeo);
        summary.setAverageUrlScore(pageCount > 0 ? totalUrlScore / pageCount : 0);
        summary.setAverageContentScore(pageCount > 0 ? totalContentScore / pageCount : 0);
        summary.setMaxCrawlDepth(maxDepth);

        report.setPages(pages);
        report.setSummary(summary);
        report.setBrokenLinks(brokenLinks);
        report.setDuplicatePages(duplicates);
        report.setUrlClassifications(urlClassifications);
        report.setCrawlDepthDistribution(depthDistribution);
        report.setContentCategories(categories);
        report.setSiteTree(siteTreeBuilderService.buildTree(rootUrl, crawlResult));
        report.setWarnings(new ArrayList<>(crawlResult.getWarnings()));

        int overallScore = calculateOverallScore(summary, pages);
        report.setOverallScore(overallScore);

        report.setAiRecommendations(geminiAiService.generateRecommendations(report));
        log.info("Analysis complete for {} in {}ms", rootUrl, System.currentTimeMillis() - start);

        persistReport(report);
        return report;
    }

    private List<QualityReportDto.DuplicatePageDto> findDuplicates(CrawlResult crawlResult) {
        List<QualityReportDto.DuplicatePageDto> duplicates = new ArrayList<>();
        List<CrawlResult.CrawledPage> pageList = new ArrayList<>(crawlResult.getPages().values());

        for (int i = 0; i < pageList.size(); i++) {
            for (int j = i + 1; j < pageList.size(); j++) {
                CrawlResult.CrawledPage p1 = pageList.get(i);
                CrawlResult.CrawledPage p2 = pageList.get(j);
                double similarity = contentQualityService.calculateSimilarity(p1.getPlainText(), p2.getPlainText());
                if (similarity >= 80) {
                    QualityReportDto.DuplicatePageDto dup = new QualityReportDto.DuplicatePageDto();
                    dup.setUrl1(p1.getUrl());
                    dup.setUrl2(p2.getUrl());
                    dup.setSimilarityPercent(Math.round(similarity * 10.0) / 10.0);
                    duplicates.add(dup);
                }
            }
        }
        return duplicates;
    }

    private int calculateOverallScore(QualityReportDto.SummaryDto summary, List<QualityReportDto.PageAnalysisDto> pages) {
        if (pages.isEmpty()) {
            return 0;
        }
        double avgSeo = pages.stream().mapToInt(p -> p.getSeo().getSeoScore()).average().orElse(0);
        double score = (summary.getAverageUrlScore() * 0.2)
                + (summary.getAverageContentScore() * 0.3)
                + (avgSeo * 0.3)
                + (Math.max(0, 100 - summary.getBrokenLinkCount() * 5) * 0.1)
                + (Math.max(0, 100 - summary.getDuplicatePageCount() * 10) * 0.1);
        return (int) Math.round(Math.max(0, Math.min(100, score)));
    }

    private void persistReport(QualityReportDto report) {
        try {
            AnalysisReport entity = new AnalysisReport();
            entity.setRootUrl(report.getRootUrl());
            entity.setStatus("COMPLETED");
            entity.setOverallScore(report.getOverallScore());
            entity.setPagesCrawled(report.getSummary().getTotalPages());
            entity.setReportJson(objectMapper.writeValueAsString(report));
            reportRepository.save(entity);
        } catch (Exception ignored) {
            // Persistence failure should not block analysis response
        }
    }

    public List<AnalysisReport> getRecentReports() {
        return reportRepository.findTop10ByOrderByCreatedAtDesc();
    }

    public QualityReportDto getReportById(Long id) throws Exception {
        AnalysisReport entity = reportRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Report not found"));
        return objectMapper.readValue(entity.getReportJson(), QualityReportDto.class);
    }
}
