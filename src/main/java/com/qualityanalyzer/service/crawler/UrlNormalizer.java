package com.qualityanalyzer.service.crawler;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Locale;

public final class UrlNormalizer {

    private UrlNormalizer() {}

    public static String normalize(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            URI uri = new URI(url.trim());
            if (uri.getScheme() == null || uri.getHost() == null) return null;
            String scheme = uri.getScheme().toLowerCase(Locale.ROOT);
            String host = uri.getHost().toLowerCase(Locale.ROOT);
            int port = uri.getPort();
            String path = uri.getPath();
            if (path == null || path.isEmpty()) path = "/";
            if (path.endsWith("/") && path.length() > 1) path = path.substring(0, path.length() - 1);
            StringBuilder normalized = new StringBuilder(scheme).append("://").append(host);
            if (port != -1) normalized.append(":").append(port);
            normalized.append(path);
            String query = uri.getQuery();
            if (query != null && !query.isBlank()) normalized.append("?").append(query);
            return normalized.toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    public static boolean isSameHost(String rootUrl, String candidateUrl) {
        try {
            URI root = new URI(rootUrl);
            URI candidate = new URI(candidateUrl);
            String rootHost = root.getHost() != null ? root.getHost().toLowerCase(Locale.ROOT) : "";
            String candidateHost = candidate.getHost() != null ? candidate.getHost().toLowerCase(Locale.ROOT) : "";
            return rootHost.equals(candidateHost);
        } catch (URISyntaxException e) {
            return false;
        }
    }

    public static String resolve(String baseUrl, String href) {
        if (href == null || href.isBlank()) return null;
        String trimmed = href.trim();
        if (trimmed.startsWith("#") || trimmed.startsWith("mailto:") || trimmed.startsWith("tel:")
                || trimmed.startsWith("javascript:") || trimmed.startsWith("data:")) return null;
        try {
            URI base = new URI(baseUrl);
            URI resolved = base.resolve(trimmed);
            return normalize(resolved.toString());
        } catch (URISyntaxException e) {
            return null;
        }
    }
}
