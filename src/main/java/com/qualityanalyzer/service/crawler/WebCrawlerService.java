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

    public WebCrawlerService(AnalyzerProperties properties) {
        this.properties = properties;
    }

    public CrawlResult crawl(String rootUrl, int maxPages, int maxDepth, String strategy) {
        String normalizedRoot = UrlNormalizer.normalize(rootUrl);
        if (normalizedRoot == null) {
            throw new IllegalArgumentException("Invalid URL: " + rootUrl);
        }
        CrawlResult result = new CrawlResult();
        Set<String> visited = new HashSet<>();
        List<String> order = "DFS".equalsIgnoreCase(strategy)
                ? crawlDfs(normalizedRoot, maxPages, maxDepth, visited, result)
                : crawlBfs(normalizedRoot, maxPages, maxDepth, visited, result);
        for (String url : order) {
            CrawlResult.CrawledPage page = result.getPages().get(url);
            if (page == null || page.getDocument() == null) continue;
            extractLinks(page, normalizedRoot, result);
        }
        return result;
    }

    private List<String> crawlBfs(String root, int maxPages, int maxDepth, Set<String> visited, CrawlResult result) {
        Deque<String> queue = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();
        List<String> order = new ArrayList<>();
        queue.add(root); depths.add(0); visited.add(root);
        while (!queue.isEmpty() && order.size() < maxPages) {
            String url = queue.poll();
            int depth = depths.poll();
            fetchAndStore(url, depth, result);
            order.add(url);
            if (depth >= maxDepth) continue;
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

    private List<String> crawlDfs(String root, int maxPages, int maxDepth, Set<String> visited, CrawlResult result) {
        Deque<String> stack = new ArrayDeque<>();
        Deque<Integer> depths = new ArrayDeque<>();
        List<String> order = new ArrayList<>();
        stack.push(root); depths.push(0); visited.add(root);
        while (!stack.isEmpty() && order.size() < maxPages) {
            String url = stack.pop();
            int depth = depths.pop();
            fetchAndStore(url, depth, result);
            order.add(url);
            if (depth >= maxDepth) continue;
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

    private List<String> discoverLinksFromPage(CrawlResult result, String url, String root) {
        CrawlResult.CrawledPage page = result.getPages().get(url);
        if (page == null || page.getDocument() == null) return List.of();
        List<String> links = new ArrayList<>();
        for (Element anchor : page.getDocument().select("a[href]")) {
            try {
                String resolved = UrlNormalizer.resolve(url, anchor.attr("href"));
                if (resolved != null && UrlNormalizer.isSameHost(root, resolved)) links.add(resolved);
            } catch (Exception ignored) { }
        }
        return links;
    }

    private void fetchAndStore(String url, int depth, CrawlResult result) {
        try {
            Connection.Response response = Jsoup.connect(url)
                    .userAgent(properties.getCrawl().getUserAgent())
                    .timeout(properties.getCrawl().getTimeoutMs())
                    .followRedirects(true).ignoreHttpErrors(true).execute();
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
        if (doc == null) return;
        for (Element anchor : doc.select("a[href]")) {
            try {
                String resolved = UrlNormalizer.resolve(page.getUrl(), anchor.attr("href"));
                if (resolved == null) continue;
                result.getAllLinks().add(new CrawlResult.LinkRef(page.getUrl(), resolved,
                        UrlNormalizer.isSameHost(root, resolved)));
            } catch (Exception ignored) { }
        }
    }
}
