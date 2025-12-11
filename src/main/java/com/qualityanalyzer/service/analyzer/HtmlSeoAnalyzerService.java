package com.qualityanalyzer.service.analyzer;

import com.qualityanalyzer.dto.QualityReportDto;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class HtmlSeoAnalyzerService {

    private static final int MIN_META_DESC_LENGTH = 50;
    private static final int MAX_META_DESC_LENGTH = 160;

    public QualityReportDto.SeoAnalysisDto analyze(Document document) {
        QualityReportDto.SeoAnalysisDto seo = new QualityReportDto.SeoAnalysisDto();
        List<String> issues = new ArrayList<>();
        int score = 100;

        if (document == null) {
            seo.setSeoScore(0);
            return seo;
        }

        String title = document.title();
        boolean hasTitle = title != null && !title.isBlank();
        seo.setHasTitle(hasTitle);
        if (!hasTitle) {
            score -= 25;
            issues.add("Missing page title");
        } else if (title.length() < 10 || title.length() > 60) {
            score -= 10;
            issues.add("Title length not optimal (10-60 chars)");
        }

        Element metaDesc = document.selectFirst("meta[name=description]");
        String description = metaDesc != null ? metaDesc.attr("content") : "";
        boolean hasMeta = description != null && !description.isBlank();
        seo.setHasMetaDescription(hasMeta);
        if (!hasMeta) {
            score -= 25;
            issues.add("Missing meta description");
        } else if (description.length() < MIN_META_DESC_LENGTH || description.length() > MAX_META_DESC_LENGTH) {
            score -= 10;
            issues.add("Meta description length not optimal (50-160 chars)");
        }

        Elements h1s = document.select("h1");
        seo.setH1Count(h1s.size());
        if (h1s.isEmpty()) {
            score -= 20;
            issues.add("Missing H1 tag");
        } else if (h1s.size() > 1) {
            score -= 15;
            issues.add("Multiple H1 tags (" + h1s.size() + ")");
        }

        seo.setH2Count(document.select("h2").size());

        Elements images = document.select("img");
        int withoutAlt = 0;
        for (Element img : images) {
            String alt = img.attr("alt");
            if (alt == null || alt.isBlank()) {
                withoutAlt++;
            }
        }
        seo.setTotalImages(images.size());
        seo.setImagesWithoutAlt(withoutAlt);
        if (withoutAlt > 0) {
            score -= Math.min(20, withoutAlt * 5);
            issues.add(withoutAlt + " image(s) missing alt text");
        }

        seo.setSeoScore(Math.max(0, Math.min(100, score)));
        return seo;
    }

    public List<String> collectIssues(Document document) {
        List<String> issues = new ArrayList<>();
        QualityReportDto.SeoAnalysisDto seo = analyze(document);

        if (!seo.isHasTitle()) {
            issues.add("Missing page title");
        }
        if (!seo.isHasMetaDescription()) {
            issues.add("Missing meta description");
        }
        if (seo.getH1Count() == 0) {
            issues.add("Missing H1 tag");
        } else if (seo.getH1Count() > 1) {
            issues.add("Multiple H1 tags");
        }
        if (seo.getImagesWithoutAlt() > 0) {
            issues.add(seo.getImagesWithoutAlt() + " images without alt text");
        }
        return issues;
    }
}
