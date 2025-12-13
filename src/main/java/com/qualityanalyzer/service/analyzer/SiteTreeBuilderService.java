package com.qualityanalyzer.service.analyzer;

import com.qualityanalyzer.dto.QualityReportDto;
import com.qualityanalyzer.service.crawler.CrawlResult;

import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

@Service
public class SiteTreeBuilderService {

    public QualityReportDto.SiteTreeNodeDto buildTree(String rootUrl, CrawlResult crawlResult) {
        Map<String, QualityReportDto.SiteTreeNodeDto> nodes = new HashMap<>();

        for (CrawlResult.CrawledPage page : crawlResult.getPages().values()) {
            QualityReportDto.SiteTreeNodeDto node = new QualityReportDto.SiteTreeNodeDto();
            node.setUrl(page.getUrl());
            node.setName(extractName(page.getUrl(), page.getTitle()));
            node.setDepth(page.getDepth());
            nodes.put(page.getUrl(), node);
        }

        QualityReportDto.SiteTreeNodeDto root = nodes.get(rootUrl);
        if (root == null && !nodes.isEmpty()) {
            root = nodes.values().iterator().next();
        }

        for (Map.Entry<String, String> entry : crawlResult.getParentMap().entrySet()) {
            QualityReportDto.SiteTreeNodeDto child = nodes.get(entry.getKey());
            QualityReportDto.SiteTreeNodeDto parent = nodes.get(entry.getValue());
            if (child != null && parent != null && !parent.getChildren().contains(child)) {
                parent.getChildren().add(child);
            }
        }

        if (root != null && root.getChildren().isEmpty()) {
            attachByPath(rootUrl, new ArrayList<>(nodes.values()), root);
        }

        return root != null ? root : new QualityReportDto.SiteTreeNodeDto();
    }

    private void attachByPath(String rootUrl, List<QualityReportDto.SiteTreeNodeDto> allNodes,
                              QualityReportDto.SiteTreeNodeDto root) {
        try {
            URI rootUri = new URI(rootUrl);
            String rootPath = rootUri.getPath() != null ? rootUri.getPath() : "/";
            Map<String, QualityReportDto.SiteTreeNodeDto> pathMap = new HashMap<>();
            pathMap.put(rootPath.isEmpty() ? "/" : rootPath, root);

            allNodes.sort((a, b) -> Integer.compare(a.getDepth(), b.getDepth()));

            for (QualityReportDto.SiteTreeNodeDto node : allNodes) {
                if (node == root) {
                    continue;
                }
                URI uri = new URI(node.getUrl());
                String path = uri.getPath() != null ? uri.getPath() : "/";
                String parentPath = getParentPath(path);
                QualityReportDto.SiteTreeNodeDto parent = pathMap.get(parentPath);
                if (parent == null) {
                    parent = root;
                }
                if (!parent.getChildren().contains(node)) {
                    parent.getChildren().add(node);
                }
                pathMap.put(path, node);
            }
        } catch (Exception ignored) {
            for (QualityReportDto.SiteTreeNodeDto node : allNodes) {
                if (node != root && node.getDepth() == 1) {
                    root.getChildren().add(node);
                }
            }
        }
    }

    private String getParentPath(String path) {
        if (path == null || path.equals("/")) {
            return "/";
        }
        int lastSlash = path.lastIndexOf('/');
        if (lastSlash <= 0) {
            return "/";
        }
        return path.substring(0, lastSlash);
    }

    private String extractName(String url, String title) {
        if (title != null && !title.isBlank()) {
            return title.length() > 40 ? title.substring(0, 37) + "..." : title;
        }
        try {
            URI uri = new URI(url);
            String path = uri.getPath();
            if (path == null || path.equals("/")) {
                return "Home";
            }
            String segment = path.substring(path.lastIndexOf('/') + 1);
            return segment.isBlank() ? "Page" : segment.replace("-", " ").replace("_", " ");
        } catch (Exception e) {
            return "Page";
        }
    }
}
