package com.qualityanalyzer.service.analyzer;

import com.qualityanalyzer.config.AnalyzerProperties;
import com.qualityanalyzer.dto.QualityReportDto;
import com.qualityanalyzer.service.crawler.CrawlResult;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class BrokenLinkCheckerService {

    private final AnalyzerProperties properties;

    public BrokenLinkCheckerService(AnalyzerProperties properties) {
        this.properties = properties;
    }

    public List<QualityReportDto.BrokenLinkDto> findBrokenLinks(CrawlResult crawlResult) {
        List<QualityReportDto.BrokenLinkDto> broken = new ArrayList<>();
        Set<String> checked = new HashSet<>();
        for (CrawlResult.LinkRef link : crawlResult.getAllLinks()) {
            String target = link.getTargetUrl();
            String key = link.getSourceUrl() + "->" + target;
            if (checked.contains(key)) continue;
            checked.add(key);
            CrawlResult.CrawledPage crawled = crawlResult.getPages().get(target);
            int status = crawled != null ? crawled.getStatusCode() : checkExternalLink(target);
            if (status == 0 || status >= 400) {
                QualityReportDto.BrokenLinkDto dto = new QualityReportDto.BrokenLinkDto();
                dto.setSourceUrl(link.getSourceUrl());
                dto.setBrokenUrl(target);
                dto.setStatusCode(status);
                dto.setLinkType(link.isInternal() ? "INTERNAL" : "EXTERNAL");
                broken.add(dto);
            }
        }
        return broken;
    }

    private int checkExternalLink(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(properties.getCrawl().getUserAgent())
                    .timeout(properties.getCrawl().getTimeoutMs())
                    .followRedirects(true).ignoreHttpErrors(true).execute().statusCode();
        } catch (IOException e) {
            return 0;
        }
    }
}
