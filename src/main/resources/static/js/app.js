const API = '/api';
let selectedStrategy = 'BFS';

document.addEventListener('DOMContentLoaded', () => {
    initStrategyToggle();
    initForm();
    loadHistory();
});

function initStrategyToggle() {
    document.querySelectorAll('.toggle-btn, .strategy-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            document.querySelectorAll('.toggle-btn, .strategy-btn').forEach(b => b.classList.remove('active'));
            btn.classList.add('active');
            selectedStrategy = btn.dataset.strategy;
        });
    });
}

function initForm() {
    document.getElementById('analyze-form').addEventListener('submit', async (e) => {
        e.preventDefault();
        await runAnalysis();
    });
}

async function runAnalysis() {
    const url = document.getElementById('url-input').value.trim();
    const maxPages = parseInt(document.getElementById('max-pages').value, 10);
    const maxDepth = parseInt(document.getElementById('max-depth').value, 10);

    const loading = document.getElementById('loading');
    const results = document.getElementById('results');
    const scanBtn = document.getElementById('scan-btn');

    loading.classList.remove('hidden');
    results.classList.add('hidden');
    scanBtn.disabled = true;
    setStatus('Analyzing…');
    animateLoadingSteps();

    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), 180000);
    const elapsed = startElapsedTimer();

    try {
        const res = await fetch(`${API}/analyze`, {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ url, maxPages, maxDepth, crawlStrategy: selectedStrategy }),
            signal: controller.signal
        });

        const data = await res.json();
        if (!res.ok) throw new Error(data.error || 'Analysis failed');

        renderResults(data);
        results.classList.remove('hidden');
        results.scrollIntoView({ behavior: 'smooth', block: 'start' });
        loadHistory();
        setStatus('Ready');
    } catch (err) {
        const msg = err.name === 'AbortError'
            ? 'Analysis timed out. Try fewer pages (e.g. 10) or lower depth.'
            : err.message;
        showToast(msg);
        setStatus('Ready');
    } finally {
        clearTimeout(timeout);
        stopElapsedTimer(elapsed);
        loading.classList.add('hidden');
        scanBtn.disabled = false;
    }
}

function setStatus(text) {
    const el = document.getElementById('status-text');
    const dot = document.querySelector('.status-dot');
    if (el) el.textContent = text;
    if (dot) dot.style.background = text === 'Analyzing…' ? 'var(--gold)' : 'var(--mint)';
}

function showToast(msg) {
    const toast = document.getElementById('toast');
    toast.textContent = msg;
    toast.classList.remove('hidden');
    setTimeout(() => toast.classList.add('hidden'), 5000);
}

function startElapsedTimer() {
    const loading = document.getElementById('loading');
    const p = loading.querySelector('.loading-text');
    const base = p.textContent;
    const start = Date.now();
    const interval = setInterval(() => {
        const secs = Math.floor((Date.now() - start) / 1000);
        p.textContent = `${base} (${secs}s)`;
    }, 1000);
    return { interval, base, p };
}

function stopElapsedTimer(elapsed) {
    if (!elapsed) return;
    clearInterval(elapsed.interval);
    elapsed.p.textContent = elapsed.base;
}

function animateLoadingSteps() {
    const steps = document.querySelectorAll('.loading-steps .step');
    let i = 0;
    const interval = setInterval(() => {
        steps.forEach((s, idx) => s.classList.toggle('active', idx === i));
        i = (i + 1) % steps.length;
    }, 900);
    setTimeout(() => clearInterval(interval), 12000);
}

function renderResults(report) {
    renderWarnings(report.warnings);
    renderScore(report);
    renderMetrics(report.summary);
    renderSiteTree(report.siteTree);
    renderDepthChart(report.crawlDepthDistribution);
    renderCategories(report.contentCategories);
    renderUrlClassifications(report.urlClassifications);
    renderAiRecommendations(report.aiRecommendations);
    renderBrokenLinks(report.brokenLinks);
    renderDuplicates(report.duplicatePages);
    renderPages(report.pages);
}

function renderWarnings(warnings) {
    const el = document.getElementById('warnings-banner');
    if (!warnings || warnings.length === 0) {
        el.classList.add('hidden');
        el.innerHTML = '';
        return;
    }
    el.classList.remove('hidden');
    el.innerHTML = warnings.map(w => `<p>${escapeHtml(w)}</p>`).join('');
}

function renderScore(report) {
    const score = report.overallScore;
    document.getElementById('overall-score').textContent = score;
    document.getElementById('analyzed-url').textContent = report.rootUrl;
    document.getElementById('crawl-info').textContent =
        `${report.crawlStrategy} crawl · ${report.summary.totalPages} pages · depth ${report.summary.maxCrawlDepth}`;

    const circle = document.getElementById('score-circle');
    const circumference = 2 * Math.PI * 58;
    circle.style.strokeDashoffset = circumference - (score / 100) * circumference;
    circle.style.stroke = score >= 70 ? '#5eead4' : score >= 40 ? '#fbbf24' : '#fb7185';

    const chips = document.getElementById('score-chips');
    chips.innerHTML = '';
    [
        { label: `URL ${Math.round(report.summary.averageUrlScore)}`, cls: scoreClass(report.summary.averageUrlScore) },
        { label: `Content ${Math.round(report.summary.averageContentScore)}`, cls: scoreClass(report.summary.averageContentScore) },
        { label: `${report.summary.brokenLinkCount} broken`, cls: report.summary.brokenLinkCount > 0 ? 'warn' : 'good' },
        { label: `${report.summary.thinContentPages} thin`, cls: report.summary.thinContentPages > 0 ? 'warn' : 'good' }
    ].forEach(item => {
        const chip = document.createElement('span');
        chip.className = `chip ${item.cls}`;
        chip.textContent = item.label;
        chips.appendChild(chip);
    });
}

function renderMetrics(summary) {
    const icons = ['◆', '◇', '○', '△', '□', '▽', '◎', '◈'];
    const metrics = [
        { value: summary.totalPages, label: 'Pages Crawled' },
        { value: summary.internalLinks, label: 'Internal Links' },
        { value: summary.externalLinks, label: 'External Links' },
        { value: summary.brokenLinkCount, label: 'Broken Links' },
        { value: summary.duplicatePageCount, label: 'Duplicates' },
        { value: summary.thinContentPages, label: 'Thin Content' },
        { value: summary.missingSeoPages, label: 'SEO Issues' },
        { value: Math.round(summary.averageUrlScore), label: 'Avg URL Score' }
    ];

    document.getElementById('metrics-grid').innerHTML = metrics.map((m, i) => `
        <div class="metric-card" style="animation: fadeUp 0.5s ease ${i * 0.05}s both">
            <div class="value">${m.value}</div>
            <div class="label">${m.label}</div>
        </div>
    `).join('');
}

function renderSiteTree(node, container) {
    const el = container || document.getElementById('site-tree');
    if (!container) el.innerHTML = '';
    if (!node || !node.name) {
        el.innerHTML = '<p class="empty-state">No site structure available</p>';
        return;
    }
    el.appendChild(buildTreeNode(node, true));
}

function buildTreeNode(node, isRoot) {
    const div = document.createElement('div');
    div.className = `tree-node${isRoot ? ' root' : ''}`;
    const label = document.createElement('span');
    label.className = 'tree-label';
    label.title = node.url || '';
    label.innerHTML = `${escapeHtml(node.name)} <span class="tree-depth">d${node.depth}</span>`;
    div.appendChild(label);
    if (node.children?.length) {
        node.children.forEach(child => div.appendChild(buildTreeNode(child, false)));
    }
    return div;
}

function renderDepthChart(distribution) {
    const chart = document.getElementById('depth-chart');
    if (!distribution || !Object.keys(distribution).length) {
        chart.innerHTML = '<p class="empty-state">No depth data</p>';
        return;
    }
    const entries = Object.entries(distribution).sort((a, b) =>
        parseInt(a[0].replace('Depth ', '')) - parseInt(b[0].replace('Depth ', '')));
    const max = Math.max(...entries.map(e => e[1]));
    chart.innerHTML = entries.map(([label, count]) => {
        const pct = max > 0 ? (count / max) * 100 : 0;
        return `<div class="depth-bar-wrap">
            <div class="depth-count">${count}</div>
            <div class="depth-bar" style="height:${Math.max(pct, 4)}%"></div>
            <div class="depth-label">${label.replace('Depth ', 'L')}</div>
        </div>`;
    }).join('');
}

function renderCategories(categories) {
    const el = document.getElementById('categories');
    if (!categories || !Object.keys(categories).length) {
        el.innerHTML = '<p class="empty-state">No categories detected</p>';
        return;
    }
    el.innerHTML = Object.entries(categories).sort((a, b) => b[1] - a[1])
        .map(([cat, count]) => `<span class="cat-tag">${escapeHtml(cat)}<span>${count}</span></span>`).join('');
}

function renderUrlClassifications(classifications) {
    const el = document.getElementById('url-classifications');
    if (!classifications?.length) {
        el.innerHTML = '<p class="empty-state">No URLs classified</p>';
        return;
    }
    el.innerHTML = classifications.map(c => `
        <div class="list-item classification-${c.classification.toLowerCase()}">
            <div class="url">${escapeHtml(c.url)}</div>
            <div class="meta">${c.classification}${c.matchedRule ? ' · ' + escapeHtml(c.matchedRule) : ''}</div>
        </div>`).join('');
}

function renderAiRecommendations(recs) {
    const el = document.getElementById('ai-recommendations');
    if (!recs?.length) {
        el.innerHTML = '<p class="empty-state">No recommendations — site looks healthy</p>';
        return;
    }
    el.innerHTML = recs.map(r => `
        <div class="ai-card">
            <div class="priority ${r.priority}">${r.priority}</div>
            <div class="issue">${escapeHtml(r.issue)}</div>
            <div class="suggestion">${escapeHtml(r.suggestion)}</div>
            ${r.pageUrl ? `<div class="page-ref">${escapeHtml(r.pageUrl)}</div>` : ''}
        </div>`).join('');
}

function renderBrokenLinks(links) {
    document.getElementById('broken-count').textContent = links.length;
    const el = document.getElementById('broken-links');
    if (!links.length) {
        el.innerHTML = '<p class="empty-state">No broken links found</p>';
        return;
    }
    el.innerHTML = links.map(l => `
        <div class="list-item">
            <div class="url">${escapeHtml(l.brokenUrl)}</div>
            <div class="meta">From: ${escapeHtml(l.sourceUrl)} · ${l.statusCode || 'N/A'} · ${l.linkType}</div>
        </div>`).join('');
}

function renderDuplicates(dups) {
    document.getElementById('duplicate-count').textContent = dups.length;
    const el = document.getElementById('duplicate-pages');
    if (!dups.length) {
        el.innerHTML = '<p class="empty-state">No duplicate content detected</p>';
        return;
    }
    el.innerHTML = dups.map(d => `
        <div class="list-item">
            <div class="url">${escapeHtml(d.url1)}</div>
            <div class="url">${escapeHtml(d.url2)}</div>
            <div class="meta">${d.similarityPercent}% similar</div>
        </div>`).join('');
}

function renderPages(pages) {
    document.getElementById('page-count').textContent = `${pages.length} pages`;
    const el = document.getElementById('pages-table');
    el.innerHTML = `
        <div class="page-row header">
            <span>URL</span><span>Category</span><span>URL</span>
            <span>Content</span><span>SEO</span><span>Issues</span>
        </div>` + pages.map(p => `
        <div class="page-row">
            <span class="page-url" title="${escapeHtml(p.title || '')}">${escapeHtml(truncate(p.url, 48))}</span>
            <span>${escapeHtml(p.contentCategory)}</span>
            <span><span class="score-pill ${scoreClass(p.urlScore)}">${p.urlScore}</span></span>
            <span><span class="score-pill ${scoreClass(p.contentScore)}">${p.contentScore}</span></span>
            <span><span class="score-pill ${scoreClass(p.seo.seoScore)}">${p.seo.seoScore}</span></span>
            <span class="issues-list">${p.issues.slice(0, 2).map(escapeHtml).join(' · ') || '—'}</span>
        </div>`).join('');
}

async function loadHistory() {
    try {
        const res = await fetch(`${API}/reports`);
        const reports = await res.json();
        const el = document.getElementById('history-list');
        if (!reports.length) {
            el.innerHTML = '<p class="empty-state">No previous scans yet</p>';
            return;
        }
        el.innerHTML = reports.map(r => `
            <div class="history-item" data-id="${r.id}">
                <span class="h-score">${r.overallScore}</span>/100 ·
                ${escapeHtml(truncate(r.rootUrl, 36))} · ${r.pagesCrawled} pg
            </div>`).join('');
        el.querySelectorAll('.history-item').forEach(item => {
            item.addEventListener('click', () => loadReport(item.dataset.id));
        });
    } catch { /* non-critical */ }
}

async function loadReport(id) {
    try {
        const res = await fetch(`${API}/reports/${id}`);
        const data = await res.json();
        if (!res.ok) throw new Error(data.error);
        renderResults(data);
        document.getElementById('results').classList.remove('hidden');
        document.getElementById('results').scrollIntoView({ behavior: 'smooth' });
    } catch (err) {
        showToast('Could not load report: ' + err.message);
    }
}

function scoreClass(score) {
    if (score >= 70) return 'high';
    if (score >= 40) return 'mid';
    return 'low';
}

function truncate(str, len) {
    if (!str) return '';
    return str.length > len ? str.substring(0, len) + '…' : str;
}

function escapeHtml(str) {
    if (!str) return '';
    const div = document.createElement('div');
    div.textContent = str;
    return div.innerHTML;
}
