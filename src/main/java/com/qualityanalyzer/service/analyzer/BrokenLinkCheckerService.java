package com.qualityanalyzer.service.analyzer;

import com.qualityanalyzer.config.AnalyzerProperties;
import com.qualityanalyzer.dto.QualityReportDto;
import com.qualityanalyzer.service.crawler.CrawlResult;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
public class BrokenLinkCheckerService {

    private static final Logger log = LoggerFactory.getLogger(BrokenLinkCheckerService.class);
    private static final int MAX_EXTERNAL_CHECKS = 25;
    private static final int MAX_UNCRAWLED_INTERNAL_CHECKS = 30;
    private static final int LINK_CHECK_TIMEOUT_MS = 4000;

    private final AnalyzerProperties properties;

    public BrokenLinkCheckerService(AnalyzerProperties properties) {
        this.properties = properties;
    }

    public List<QualityReportDto.BrokenLinkDto> findBrokenLinks(CrawlResult crawlResult) {
        List<QualityReportDto.BrokenLinkDto> broken = new ArrayList<>();
        Set<String> checkedPairs = new HashSet<>();
        Map<String, Integer> targetStatusCache = new HashMap<>();
        int externalChecks = 0;
        int uncrawledInternalChecks = 0;

        for (CrawlResult.LinkRef link : crawlResult.getAllLinks()) {
            try {
                String target = link.getTargetUrl();
                String pairKey = link.getSourceUrl() + "->" + target;
                if (checkedPairs.contains(pairKey)) {
                    continue;
                }
                checkedPairs.add(pairKey);

                CrawlResult.CrawledPage crawled = crawlResult.getPages().get(target);
                int status;
                if (crawled != null) {
                    status = crawled.getStatusCode();
                } else if (targetStatusCache.containsKey(target)) {
                    status = targetStatusCache.get(target);
                } else if (link.isInternal()) {
                    if (uncrawledInternalChecks >= MAX_UNCRAWLED_INTERNAL_CHECKS) {
                        continue;
                    }
                    uncrawledInternalChecks++;
                    status = probeLink(target);
                    targetStatusCache.put(target, status);
                } else {
                    if (externalChecks >= MAX_EXTERNAL_CHECKS) {
                        continue;
                    }
                    if (targetStatusCache.containsKey(target)) {
                        status = targetStatusCache.get(target);
                    } else {
                        externalChecks++;
                        status = probeLink(target);
                        targetStatusCache.put(target, status);
                    }
                }

                if (status == 0 || status >= 400) {
                    QualityReportDto.BrokenLinkDto dto = new QualityReportDto.BrokenLinkDto();
                    dto.setSourceUrl(link.getSourceUrl());
                    dto.setBrokenUrl(target);
                    dto.setStatusCode(status);
                    dto.setLinkType(link.isInternal() ? "INTERNAL" : "EXTERNAL");
                    broken.add(dto);
                }
            } catch (Exception ignored) {
                // Skip links that cannot be checked
            }
        }

        log.info("Broken link check done: {} broken, {} external probed, {} internal probed",
                broken.size(), externalChecks, uncrawledInternalChecks);
        return broken;
    }

    private int probeLink(String url) {
        try {
            Connection.Response head = Jsoup.connect(url)
                    .userAgent(properties.getCrawl().getUserAgent())
                    .timeout(LINK_CHECK_TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .method(Connection.Method.HEAD)
                    .execute();
            int status = head.statusCode();
            if (status == 405 || status == 501) {
                return probeWithGet(url);
            }
            return status;
        } catch (Exception e) {
            return probeWithGet(url);
        }
    }

    private int probeWithGet(String url) {
        try {
            return Jsoup.connect(url)
                    .userAgent(properties.getCrawl().getUserAgent())
                    .timeout(LINK_CHECK_TIMEOUT_MS)
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .maxBodySize(0)
                    .execute()
                    .statusCode();
        } catch (Exception e) {
            return 0;
        }
    }
}
