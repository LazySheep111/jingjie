const novelImportBtn = document.getElementById('novelImportBtn');
const novelImportFile = document.getElementById('novelImportFile');
const novelImportTitleInput = document.getElementById('novelImportTitleInput');
const novelImportStatus = document.getElementById('novelImportStatus');
const recentNovelList = document.getElementById('recentNovelList');
const recentNovelEmpty = document.getElementById('recentNovelEmpty');
const refreshRecentBtn = document.getElementById('refreshRecentBtn');

novelImportBtn.addEventListener('click', importNovel);
refreshRecentBtn.addEventListener('click', loadRecentNovels);
loadRecentNovels();

async function importNovel() {
    const file = novelImportFile.files && novelImportFile.files[0];
    if (!file) {
        setImportStatus('请选择 TXT 或 DOCX 文件。', true);
        return;
    }
    if (!/\.(txt|docx)$/i.test(file.name)) {
        setImportStatus('暂只支持 TXT 和 DOCX 文件。', true);
        return;
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('novelTitle', novelImportTitleInput.value.trim());
    novelImportBtn.disabled = true;
    setImportStatus('正在读取小说并识别章节，请稍候...');
    try {
        const response = await fetch('/api/novel/import', {method: 'POST', body: formData});
        const payload = await response.json().catch(() => ({}));
        if (!response.ok || payload.success === false) {
            throw new Error(payload.errorMsg || payload.message || `导入失败：${response.status}`);
        }
        const result = payload.data || payload;
        if (!result.novelId) {
            throw new Error('导入成功但未返回小说 ID');
        }
        window.location.href = `/novel-detail.html?novelId=${encodeURIComponent(result.novelId)}`;
    } catch (error) {
        setImportStatus(error.message, true);
    } finally {
        novelImportBtn.disabled = false;
    }
}

async function loadRecentNovels() {
    recentNovelList.innerHTML = '';
    recentNovelEmpty.hidden = true;
    try {
        const response = await fetch('/api/novel/history?page=1&pageSize=10');
        if (!response.ok) {
            throw new Error(`请求失败：${response.status}`);
        }
        const payload = await response.json();
        const data = payload && payload.data ? payload.data : payload;
        const records = data && (data.records || data.list || data.rows || data);
        const novels = Array.isArray(records) ? records : [];
        if (!novels.length) {
            recentNovelEmpty.hidden = false;
            return;
        }
        novels.forEach(renderRecentNovel);
    } catch (error) {
        recentNovelEmpty.textContent = `加载失败：${error.message}`;
        recentNovelEmpty.hidden = false;
    }
}

function renderRecentNovel(novel) {
    const novelId = novel.novelId || novel.id || novel.workId;
    const card = document.createElement('article');
    card.className = 'workbench-recent-item';
    card.innerHTML = `
        <div>
            <p class="eyebrow">作品 ${escapeHtml(novelId || '-')}</p>
            <h3>${escapeHtml(novel.novelTitle || novel.title || '未命名小说')}</h3>
            <p class="asset-meta">${escapeHtml(novel.totalChapter || novel.chapterCount || '章节数未知')} 个章节</p>
        </div>
        <button type="button" class="secondary-button">进入分镜</button>
    `;
    card.querySelector('button').addEventListener('click', () => {
        if (novelId) {
            window.location.href = `/novel-detail.html?novelId=${encodeURIComponent(novelId)}`;
        }
    });
    recentNovelList.appendChild(card);
}

function setImportStatus(message, isError = false) {
    novelImportStatus.textContent = message || '';
    novelImportStatus.classList.toggle('error', isError);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
