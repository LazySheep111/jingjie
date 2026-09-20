const historyList = document.getElementById('historyList');
const historyEmpty = document.getElementById('historyEmpty');
const pageInfo = document.getElementById('pageInfo');
const refreshBtn = document.getElementById('refreshBtn');
const prevPageBtn = document.getElementById('prevPageBtn');
const nextPageBtn = document.getElementById('nextPageBtn');

let page = 1;
const pageSize = 10;
let hasNext = false;

refreshBtn.addEventListener('click', () => loadHistory());
prevPageBtn.addEventListener('click', () => {
    if (page > 1) {
        page -= 1;
        loadHistory();
    }
});
nextPageBtn.addEventListener('click', () => {
    if (hasNext) {
        page += 1;
        loadHistory();
    }
});

loadHistory();

async function loadHistory() {
    historyList.innerHTML = '';
    historyEmpty.hidden = true;
    pageInfo.textContent = `第 ${page} 页`;
    prevPageBtn.disabled = page <= 1;
    nextPageBtn.disabled = true;

    try {
        const response = await fetch(`/api/novel/history?page=${page}&pageSize=${pageSize}`);
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || `请求失败：${response.status}`);
        }

        const data = unwrapResponse(await response.json());
        const records = normalizeRecords(data);
        const total = Number(data && data.total) || records.length;
        hasNext = page * pageSize < total || records.length === pageSize;

        pageInfo.textContent = `第 ${page} 页`;
        prevPageBtn.disabled = page <= 1;
        nextPageBtn.disabled = !hasNext;

        if (!records.length) {
            historyEmpty.textContent = '暂无历史作品';
            historyEmpty.hidden = false;
            return;
        }

        records.forEach(renderHistoryCard);
    } catch (error) {
        historyEmpty.hidden = false;
        historyEmpty.textContent = `加载失败：${error.message}`;
    }
}

function renderHistoryCard(item) {
    const card = document.createElement('article');
    card.className = 'history-card workspace-project-row';

    const novelId = pickNovelId(item);
    const title = item.novelTitle || item.title || '未命名小说';
    const chapterCount = item.totalChapter || item.chapterCount || getChapterCount(item);
    const overallPlot = item.overallPlot || item.summary || item.outlineSummary || '暂无整体梗概';
    const statusText = statusLabel(item.status);

    card.innerHTML = `
        <div class="history-card-head">
            <div>
                <p class="eyebrow">作品 ${escapeHtml(novelId || '-')}</p>
                <h2>${escapeHtml(title)}</h2>
            </div>
            <span class="badge">${escapeHtml(statusText)}</span>
        </div>
        <div class="card-meta">
            <span>章节数：${escapeHtml(chapterCount || '未知')}</span>
            <span>创建时间：${escapeHtml(formatTime(item.createTime || item.createdTime))}</span>
        </div>
        <p class="history-overview">${escapeHtml(overallPlot)}</p>
        <div class="card-actions">
            <button type="button" data-action="view">查看小说</button>
            <button type="button" data-action="copy" class="secondary-button">复制梗概</button>
            <button type="button" data-action="delete" class="secondary-button">删除作品</button>
        </div>
    `;

    card.querySelector('[data-action="view"]').addEventListener('click', () => viewNovel(item));
    card.querySelector('[data-action="copy"]').addEventListener('click', () => copyOverview(item));
    card.querySelector('[data-action="delete"]').addEventListener('click', () => deleteWork(item));
    historyList.appendChild(card);
}

function viewNovel(item) {
    const novelId = pickNovelId(item);
    if (!novelId) {
        alert('缺少 novelId，无法查看小说。');
        return;
    }
    const previousNovelId = readSelectedNovelId();
    rememberSelectedNovelId(novelId);
    if (String(previousNovelId || '') !== String(novelId)) {
        clearVideoContext();
    }
    window.location.href = `/novel-detail.html?novelId=${encodeURIComponent(novelId)}`;
}

async function copyOverview(item) {
    const text = [
        item.novelTitle || item.title || '未命名小说',
        item.overallPlot || item.summary || item.outlineSummary || ''
    ].filter(Boolean).join('\n\n');

    try {
        await navigator.clipboard.writeText(text);
        alert('梗概已复制。');
    } catch (error) {
        alert(`复制失败：${error.message}`);
    }
}

async function deleteWork(item) {
    const novelId = pickNovelId(item);
    if (!novelId) {
        alert('缺少 novelId，无法删除。');
        return;
    }
    if (!confirm('确定删除这个作品吗？')) {
        return;
    }

    const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}`, {
        method: 'DELETE'
    });

    if (!response.ok) {
        const text = await response.text();
        alert(text || '删除失败。');
        return;
    }
    loadHistory();
}

function normalizeRecords(data) {
    if (Array.isArray(data)) {
        return data;
    }
    if (!data || typeof data !== 'object') {
        return [];
    }
    if (Array.isArray(data.records)) {
        return data.records;
    }
    if (Array.isArray(data.list)) {
        return data.list;
    }
    if (Array.isArray(data.rows)) {
        return data.rows;
    }
    return [];
}

function unwrapResponse(data) {
    if (data && typeof data === 'object' && 'data' in data) {
        return data.data;
    }
    return data;
}

function pickNovelId(item) {
    return item && (item.novelId || item.id || item.workId || '');
}

function rememberSelectedNovelId(novelId) {
    try {
        localStorage.setItem('mirror:selectedNovelId', String(novelId));
    } catch (_) { }
}

function readSelectedNovelId() {
    try {
        return localStorage.getItem('mirror:selectedNovelId') || '';
    } catch (_) {
        return '';
    }
}

function clearVideoContext() {
    try {
        localStorage.removeItem('mirror:videoContext');
    } catch (_) { }
}

function getChapterCount(item) {
    const chapterList = item.chapterList || item.chapters || [];
    return Array.isArray(chapterList) ? chapterList.length : '';
}

function statusLabel(status) {
    const statusMap = {
        OUTLINE_ONLY: '仅保存大纲',
        GENERATING_FULL: '全文生成中',
        FULL_GENERATED: '全文已生成',
        FAILED: '生成失败'
    };
    return statusMap[status] || status || '未知状态';
}

function formatTime(value) {
    if (!value) {
        return '未知';
    }
    const date = new Date(value);
    if (Number.isNaN(date.getTime())) {
        return value;
    }
    return date.toLocaleString();
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
