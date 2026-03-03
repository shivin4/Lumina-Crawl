package com.qualityanalyzer.service.crawler;

import com.qualityanalyzer.config.AnalyzerProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class SitemapDiscoveryService {

    private static final Logger log = LoggerFactory.getLogger(SitemapDiscoveryService.class);
    private static final int MAX_CHILD_SITEMAPS = 4;
    private static final Pattern ROBOTS_SITEMAP = Pattern.compile("(?im)^Sitemap:\\s*(.+)$");
    private static final Pattern LOC_TAG = Pattern.compile("<loc>\\s*([^<]+?)\\s*</loc>", Pattern.CASE_INSENSITIVE);

    private final AnalyzerProperties properties;
    private final RestTemplate restTemplate;

    public SitemapDiscoveryService(AnalyzerProperties properties, RestTemplate restTemplate) {
        this.properties = properties;
        this.restTemplate = restTemplate;
    }

    public List<String> discoverUrls(String rootUrl, int limit) {
        Set<String> urls = new LinkedHashSet<>();
        List<String> sitemapCandidates = findSitemapLocations(rootUrl);

        for (String sitemapUrl : sitemapCandidates) {
            if (urls.size() >= limit) {
                break;
            }
            collectFromSitemap(sitemapUrl, rootUrl, urls, limit, true);
        }

        List<String> result = new ArrayList<>(urls);
        log.info("Sitemap discovery found {} URLs for {}", result.size(), rootUrl);
        return result;
    }

    private List<String> findSitemapLocations(String rootUrl) {
        List<String> locations = new ArrayList<>();
        String base = rootUrl.endsWith("/") ? rootUrl.substring(0, rootUrl.length() - 1) : rootUrl;
        locations.add(base + "/sitemap.xml");

        try {
            String robots = fetchText(base + "/robots.txt");
            Matcher matcher = ROBOTS_SITEMAP.matcher(robots);
            while (matcher.find()) {
                String loc = UrlNormalizer.normalize(matcher.group(1).trim());
                if (loc != null && !locations.contains(loc)) {
                    locations.add(0, loc);
                }
            }
        } catch (Exception ignored) {
            // robots.txt is optional
        }
        return locations;
    }

    private void collectFromSitemap(String sitemapUrl, String rootUrl, Set<String> urls, int limit,
                                    boolean allowChildSitemaps) {
        try {
            String xml = fetchText(sitemapUrl);
            if (xml == null || xml.isBlank()) {
                return;
            }

            List<String> locs = extractLocs(xml);
            boolean isIndex = xml.toLowerCase().contains("<sitemapindex");

            if (isIndex && allowChildSitemaps) {
                int childCount = 0;
                for (String loc : locs) {
                    if (urls.size() >= limit || childCount >= MAX_CHILD_SITEMAPS) {
                        break;
                    }
                    String child = UrlNormalizer.normalize(loc);
                    if (child != null) {
                        childCount++;
                        collectFromSitemap(child, rootUrl, urls, limit, false);
                    }
                }
                return;
            }

            for (String loc : locs) {
                if (urls.size() >= limit) {
                    break;
                }
                String normalized = UrlNormalizer.normalize(loc);
                if (normalized != null && UrlNormalizer.isSameHost(rootUrl, normalized)) {
                    urls.add(normalized);
                }
            }
        } catch (Exception e) {
            log.warn("Could not read sitemap {}: {}", sitemapUrl, e.getMessage());
        }
    }

    private List<String> extractLocs(String xml) {
        List<String> locs = new ArrayList<>();
        Matcher matcher = LOC_TAG.matcher(xml);
        while (matcher.find()) {
            locs.add(matcher.group(1).trim());
        }
        return locs;
    }

    private String fetchText(String url) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", properties.getCrawl().getUserAgent());
        headers.set("Accept", "application/xml,text/xml,text/plain,*/*;q=0.8");
        ResponseEntity<String> response = restTemplate.exchange(
                url, HttpMethod.GET, new HttpEntity<>(headers), String.class);
        return response.getBody();
    }
}
