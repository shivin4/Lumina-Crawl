package com.qualityanalyzer.service.crawler;

import com.qualityanalyzer.config.AnalyzerProperties;
import org.jsoup.Connection;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class WebCrawlerService {

    private final AnalyzerProperties properties;
    private final SitemapDiscoveryService sitemapDiscoveryService;

    public WebCrawlerService(AnalyzerProperties properties, SitemapDiscoveryService sitemapDiscoveryService) {
        this.properties = properties;
        this.sitemapDiscoveryService = sitemapDiscoveryService;
    }

    public CrawlResult crawl(String rootUrl, int maxPages, int maxDepth, String strategy) {
        String normalizedRoot = UrlNormalizer.normalize(rootUrl);
        if (normalizedRoot == null) {
            throw new IllegalArgumentException("Invalid URL: " + rootUrl);
        }

        CrawlResult result = new CrawlResult();
        Set<String> visited = new HashSet<>();
        List<String> seeds = List.of(normalizedRoot);

        List<String> order = "DFS".equalsIgnoreCase(strategy)
                ? crawlDfs(normalizedRoot, maxPages, maxDepth, visited, result, seeds)
                : crawlBfs(normalizedRoot, maxPages, maxDepth, visited, result, seeds);

        for (String url : order) {
            CrawlResult.CrawledPage page = result.getPages().get(url);
            if (page == null || page.getDocument() == null) {
                continue;
            }
            extractLinks(page, normalizedRoot, result);
        }
        return result;
    }

    private List<String> crawlBfs(String root, int maxPages, int maxDepth, Set<String> visited,
                                  CrawlResult result, List<String> seeds) {
        Deque<String> queue = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();
        List<String> order = new ArrayList<>();

        for (String seed : seeds) {
            if (visited.add(seed)) {
                queue.add(seed);
                depths.add(seed.equals(root) ? 0 : 1);
                if (!seed.equals(root)) {
                    result.getParentMap().put(seed, root);
                }
            }
        }

        while (!queue.isEmpty() && order.size() < maxPages) {
            String url = queue.poll();
            int depth = depths.poll();
            fetchAndStore(url, depth, result);
            order.add(url);

            if (depth == 0) {
                maybeSeedFromSitemap(root, maxPages, maxDepth, visited, result, queue, depths);
            }

            if (depth >= maxDepth) {
                continue;
            }
            for (String link : discoverLinksFromPage(result, url, root)) {
                if (!visited.contains(link) && order.size() + queue.size() < maxPages) {
                    visited.add(link);
                    queue.add(link);
                    depths.add(depth + 1);
                    result.getParentMap().put(link, url);
                }
            }
        }
        return order;
    }

    private List<String> crawlDfs(String root, int maxPages, int maxDepth, Set<String> visited,
                                  CrawlResult result, List<String> seeds) {
        Deque<String> stack = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();
        List<String> order = new ArrayList<>();

        for (int i = seeds.size() - 1; i >= 0; i--) {
            String seed = seeds.get(i);
            if (visited.add(seed)) {
                stack.push(seed);
                depths.push(seed.equals(root) ? 0 : 1);
                if (!seed.equals(root)) {
                    result.getParentMap().put(seed, root);
                }
            }
        }

        while (!stack.isEmpty() && order.size() < maxPages) {
            String url = stack.pop();
            int depth = depths.pop();
            fetchAndStore(url, depth, result);
            order.add(url);

            if (depth == 0) {
                maybeSeedFromSitemapDfs(root, maxPages, maxDepth, visited, result, stack, depths);
            }

            if (depth >= maxDepth) {
                continue;
            }
            List<String> links = discoverLinksFromPage(result, url, root);
            for (int i = links.size() - 1; i >= 0; i--) {
                String link = links.get(i);
                if (!visited.contains(link) && order.size() + stack.size() < maxPages) {
                    visited.add(link);
                    stack.push(link);
                    depths.push(depth + 1);
                    result.getParentMap().put(link, url);
                }
            }
        }
        return order;
    }

    private void maybeSeedFromSitemap(String root, int maxPages, int maxDepth, Set<String> visited,
                                      CrawlResult result, Deque<String> queue, Deque<Integer> depths) {
        CrawlResult.CrawledPage home = result.getPages().get(root);
        if (!shouldUseSitemap(home, result, root)) {
            return;
        }
        List<String> sitemapUrls = sitemapDiscoveryService.discoverUrls(root, maxPages * 2);
        int added = 0;
        for (String url : sitemapUrls) {
            if (url.equals(root) || visited.contains(url)) {
                continue;
            }
            if (orderLimitReached(visited, queue, maxPages)) {
                break;
            }
            visited.add(url);
            queue.add(url);
            depths.add(Math.min(1, maxDepth));
            result.getParentMap().put(url, root);
            added++;
        }
        if (added > 0) {
            result.setSitemapUrlsUsed(added);
            result.addWarning("HTML link discovery was limited — supplemented crawl with " + added
                    + " URLs from sitemap.xml.");
        }
    }

    private void maybeSeedFromSitemapDfs(String root, int maxPages, int maxDepth, Set<String> visited,
                                         CrawlResult result, Deque<String> stack, Deque<Integer> depths) {
        CrawlResult.CrawledPage home = result.getPages().get(root);
        if (!shouldUseSitemap(home, result, root)) {
            return;
        }
        List<String> sitemapUrls = sitemapDiscoveryService.discoverUrls(root, maxPages * 2);
        int added = 0;
        for (int i = sitemapUrls.size() - 1; i >= 0; i--) {
            String url = sitemapUrls.get(i);
            if (url.equals(root) || visited.contains(url)) {
                continue;
            }
            if (visited.size() >= maxPages) {
                break;
            }
            visited.add(url);
            stack.push(url);
            depths.push(Math.min(1, maxDepth));
            result.getParentMap().put(url, root);
            added++;
        }
        if (added > 0) {
            result.setSitemapUrlsUsed(added);
            result.addWarning("HTML link discovery was limited — supplemented crawl with " + added
                    + " URLs from sitemap.xml.");
        }
    }

    private boolean shouldUseSitemap(CrawlResult.CrawledPage home, CrawlResult result, String root) {
        if (BotProtectionDetector.isBlocked(home)) {
            result.addWarning("Site appears to block automated crawlers (e.g. Cloudflare/JS challenge). "
                    + "Results may be incomplete — using sitemap.xml where available.");
            return true;
        }
        int internalLinks = discoverLinksFromPage(result, root, root).size();
        return internalLinks < 3;
    }

    private boolean orderLimitReached(Set<String> visited, Deque<String> queue, int maxPages) {
        return visited.size() >= maxPages;
    }

    private List<String> discoverLinksFromPage(CrawlResult result, String url, String root) {
        CrawlResult.CrawledPage page = result.getPages().get(url);
        if (page == null || page.getDocument() == null) {
            return List.of();
        }
        List<String> links = new ArrayList<>();
        Elements anchors = page.getDocument().select("a[href]");
        for (Element anchor : anchors) {
            try {
                String resolved = UrlNormalizer.resolve(url, anchor.attr("href"));
                if (resolved != null && UrlNormalizer.isSameHost(root, resolved)) {
                    links.add(resolved);
                }
            } catch (Exception ignored) {
                // Skip malformed hrefs
            }
        }
        return links;
    }

    private void fetchAndStore(String url, int depth, CrawlResult result) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(properties.getCrawl().getUserAgent())
                    .timeout(properties.getCrawl().getTimeoutMs())
                    .followRedirects(true)
                    .ignoreHttpErrors(true)
                    .execute();

            int status = response.statusCode();
            Document doc = status >= 200 && status < 400 ? response.parse() : null;
            String title = doc != null && doc.title() != null ? doc.title() : "";
            String text = doc != null ? doc.body() != null ? doc.body().text() : doc.text() : "";

            result.getPages().put(url, new CrawlResult.CrawledPage(url, depth, status, doc, text, title));
        } catch (IOException e) {
            result.getPages().put(url, new CrawlResult.CrawledPage(url, depth, 0, null, "", ""));
        }
    }

    private void extractLinks(CrawlResult.CrawledPage page, String root, CrawlResult result) {
        Document doc = page.getDocument();
        if (doc == null) {
            return;
        }
        Elements anchors = doc.select("a[href]");
        for (Element anchor : anchors) {
            try {
                String resolved = UrlNormalizer.resolve(page.getUrl(), anchor.attr("href"));
                if (resolved == null) {
                    continue;
                }
                boolean internal = UrlNormalizer.isSameHost(root, resolved);
                result.getAllLinks().add(new CrawlResult.LinkRef(page.getUrl(), resolved, internal));
            } catch (Exception ignored) {
                // Skip malformed or unparseable hrefs
            }
        }
    }
}
