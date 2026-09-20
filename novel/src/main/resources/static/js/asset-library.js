const state = {
    novels: [],
    records: [],
    selectedNovelId: '',
    selectedAsset: null,
    page: 1,
    pageSize: 20,
    total: 0,
    search: '',
    assetType: '',
    detailRequestId: 0
};

const novelFilterList = document.getElementById('novelFilterList');
const novelSearchInput = document.getElementById('novelSearchInput');
const assetGrid = document.getElementById('assetGrid');
const assetEmpty = document.getElementById('assetEmpty');
const assetResultSummary = document.getElementById('assetResultSummary');
const assetSearchInput = document.getElementById('assetSearchInput');
const assetTypeFilter = document.getElementById('assetTypeFilter');
const assetSearchBtn = document.getElementById('assetSearchBtn');
const assetPagination = document.getElementById('assetPagination');
const assetDetailPanel = document.getElementById('assetDetailPanel');
const assetStatus = document.getElementById('assetStatus');
const reuseModal = document.getElementById('assetReuseModal');
const reuseAssetName = document.getElementById('reuseAssetName');
const reuseNovelSelect = document.getElementById('reuseNovelSelect');
const reuseChapterSelect = document.getElementById('reuseChapterSelect');
const reuseRoleSelect = document.getElementById('reuseRoleSelect');
const reuseAssetSelect = document.getElementById('reuseAssetSelect');
const reuseStatus = document.getElementById('reuseStatus');
const submitReuseBtn = document.getElementById('submitReuseBtn');
const closeReuseBtn = document.getElementById('closeReuseBtn');
const cancelReuseBtn = document.getElementById('cancelReuseBtn');
const imagePreviewModal = document.getElementById('assetImagePreviewModal');
const imagePreview = document.getElementById('assetImagePreview');
const closeImageBtn = document.getElementById('closeAssetImageBtn');

function unwrapResponse(data) {
    if (data && typeof data === 'object' && 'data' in data) {
        return data.data;
    }
    return data;
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function normalizeList(data, keys) {
    if (Array.isArray(data)) return data;
    for (const key of keys) {
        if (data && Array.isArray(data[key])) return data[key];
    }
    return [];
}

function buildAssetQuery(filters) {
    const params = new URLSearchParams();
    if (filters.novelId) params.set('novelId', filters.novelId);
    if (filters.assetType) params.set('assetType', filters.assetType);
    if (filters.keyword) params.set('keyword', filters.keyword);
    params.set('page', String(filters.page || 1));
    params.set('pageSize', String(filters.pageSize || 20));
    return params.toString();
}

function normalizeLibraryResponse(data) {
    const source = unwrapResponse(data) || {};
    const records = Array.isArray(source) ? source : normalizeList(source, ['records', 'list', 'items']);
    return {
        records,
        total: Number(source.total ?? records.length),
        page: Number(source.page ?? state.page),
        pageSize: Number(source.pageSize ?? state.pageSize)
    };
}

function setStatus(message, isError = false) {
    assetStatus.textContent = message || '';
    assetStatus.classList.toggle('error', Boolean(isError));
}

async function requestJson(url, options) {
    const response = await fetch(url, options);
    const payload = await response.json().catch(() => null);
    if (!response.ok) {
        throw new Error(payload?.message || payload?.error || `请求失败：${response.status}`);
    }
    return unwrapResponse(payload);
}

async function loadNovels() {
    try {
        const data = await requestJson('/api/novels');
        state.novels = normalizeList(data, ['records', 'list', 'novels']);
        renderNovelFilters();
        populateReuseNovels();
    } catch (error) {
        state.novels = [];
        renderNovelFilters();
        setStatus(`小说列表加载失败：${error.message}`, true);
    }
}

function novelIdOf(novel) {
    return novel?.novelId ?? novel?.id ?? novel?.workId ?? '';
}

function novelTitleOf(novel) {
    return novel?.novelTitle || novel?.title || `作品 ${novelIdOf(novel) || '-'}`;
}

function renderNovelFilters() {
    const keyword = novelSearchInput.value.trim().toLowerCase();
    const novels = state.novels.filter(novel => novelTitleOf(novel).toLowerCase().includes(keyword));
    const allActive = !state.selectedNovelId;
    novelFilterList.innerHTML = [
        `<button type="button" class="novel-filter ${allActive ? 'active' : ''}" data-novel-id="">全部小说</button>`,
        ...novels.map(novel => {
            const id = novelIdOf(novel);
            return `<button type="button" class="novel-filter ${String(id) === String(state.selectedNovelId) ? 'active' : ''}" data-novel-id="${escapeHtml(id)}">${escapeHtml(novelTitleOf(novel))}</button>`;
        })
    ].join('');
    novelFilterList.querySelectorAll('.novel-filter').forEach(button => {
        button.addEventListener('click', () => {
            state.selectedNovelId = button.dataset.novelId || '';
            state.page = 1;
            renderNovelFilters();
            loadAssets();
        });
    });
}

async function loadAssets() {
    assetGrid.innerHTML = '<div class="asset-library-empty">正在加载资产...</div>';
    const query = buildAssetQuery({
        novelId: state.selectedNovelId,
        assetType: state.assetType,
        keyword: state.search,
        page: state.page,
        pageSize: state.pageSize
    });
    try {
        const result = normalizeLibraryResponse(await requestJson(`/api/visual-assets/library?${query}`));
        state.records = result.records;
        state.total = result.total;
        state.page = result.page;
        state.pageSize = result.pageSize;
        renderAssetCards();
        renderPagination();
    } catch (error) {
        state.records = [];
        state.total = 0;
        assetGrid.innerHTML = '';
        assetEmpty.hidden = false;
        assetEmpty.textContent = `资产加载失败：${error.message}`;
        assetResultSummary.textContent = '加载失败';
    }
}

function assetTypeLabel(type) {
    return type === 'LOCATION' ? '场景' : '人物';
}

function imagePath(asset) {
    return asset?.compositeImagePath || asset?.imagePath || '';
}

function renderAssetCards() {
    assetEmpty.hidden = state.records.length > 0;
    assetResultSummary.textContent = `共 ${state.total} 个资产`;
    assetGrid.innerHTML = state.records.map(asset => {
        const image = imagePath(asset);
        const active = state.selectedAsset && String(state.selectedAsset.assetId) === String(asset.assetId);
        return `<article class="asset-library-card asset-gallery-card ${active ? 'active' : ''}" data-asset-id="${escapeHtml(asset.assetId)}">
            ${image ? `<img class="asset-library-image" src="${escapeHtml(image)}" alt="${escapeHtml(asset.assetName || '资产')}三视图">` : '<div class="asset-library-image-placeholder">图片待生成</div>'}
            <div class="asset-library-card-foot"><h3>${escapeHtml(asset.assetName || '未命名资产')}</h3><span class="asset-type-chip">${assetTypeLabel(asset.assetType)}</span></div>
            <p>${escapeHtml(asset.novelTitle || '未命名小说')} · v${escapeHtml(asset.version || 1)}</p>
            <p>已复用 ${escapeHtml(asset.reuseCount || 0)} 次</p>
        </article>`;
    }).join('');
    assetGrid.querySelectorAll('.asset-library-card').forEach(card => {
        card.addEventListener('click', event => {
            if (event.target.matches('img')) {
                openImagePreview(event.target);
                return;
            }
            loadAssetDetail(card.dataset.assetId);
        });
    });
}

function renderPagination() {
    const totalPages = Math.max(1, Math.ceil(state.total / state.pageSize));
    assetPagination.innerHTML = `<button type="button" class="secondary-button" data-page="prev" ${state.page <= 1 ? 'disabled' : ''}>上一页</button>
        <span>第 ${state.page} / ${totalPages} 页</span>
        <button type="button" class="secondary-button" data-page="next" ${state.page >= totalPages ? 'disabled' : ''}>下一页</button>`;
    assetPagination.querySelector('[data-page="prev"]').addEventListener('click', () => changePage(state.page - 1));
    assetPagination.querySelector('[data-page="next"]').addEventListener('click', () => changePage(state.page + 1));
}

function changePage(page) {
    const totalPages = Math.max(1, Math.ceil(state.total / state.pageSize));
    if (page < 1 || page > totalPages) return;
    state.page = page;
    loadAssets();
}

async function loadAssetDetail(assetId) {
    const requestId = ++state.detailRequestId;
    assetDetailPanel.innerHTML = '<div class="asset-detail-empty">正在加载资产详情...</div>';
    try {
        const asset = await requestJson(`/api/visual-assets/${encodeURIComponent(assetId)}`);
        if (requestId !== state.detailRequestId) return;
        state.selectedAsset = asset;
        renderAssetCards();
        renderAssetDetail(asset);
    } catch (error) {
        assetDetailPanel.innerHTML = `<div class="asset-detail-empty">详情加载失败：${escapeHtml(error.message)}</div>`;
    }
}

function renderAssetDetail(asset) {
    const image = imagePath(asset);
    assetDetailPanel.innerHTML = `<div class="asset-detail-head"><div><h2>${escapeHtml(asset.assetName || '未命名资产')}</h2><p class="asset-meta">${escapeHtml(asset.novelTitle || '未命名小说')} · ${assetTypeLabel(asset.assetType)} · v${escapeHtml(asset.version || 1)}</p></div><span class="asset-type-chip">复用 ${escapeHtml(asset.reuseCount || 0)} 次</span></div>
        ${image ? `<img id="assetDetailImage" class="asset-detail-image" src="${escapeHtml(image)}" alt="${escapeHtml(asset.assetName || '资产')}三视图">` : '<div class="asset-library-image-placeholder">图片待生成</div>'}
        <form id="assetDetailForm">
            <label>核心特征<textarea name="coreFeatures">${escapeHtml(asset.coreFeatures || '')}</textarea></label>
            <label>提示词<textarea name="prompt">${escapeHtml(assetPromptValue(asset))}</textarea></label>
            <p id="detailStatus" class="status"></p>
            <div class="asset-detail-actions"><button type="submit">保存为新版本</button><button id="reuseAssetBtn" type="button" class="secondary-button">复用资产</button></div>
        </form>`;
    const detailImage = document.getElementById('assetDetailImage');
    if (detailImage) detailImage.addEventListener('click', () => openImagePreview(detailImage));
    document.getElementById('assetDetailForm').addEventListener('submit', event => {
        event.preventDefault();
        saveAssetVersion(asset.assetId);
    });
    document.getElementById('reuseAssetBtn').addEventListener('click', () => openReuseDialog(asset));
}

function assetPromptValue(asset) {
    return asset.frontPrompt || asset.sidePrompt || asset.backPrompt || '';
}

function buildVersionPayload(values) {
    return {
        coreFeatures: values.coreFeatures || '',
        frontPrompt: values.prompt || '',
        sidePrompt: values.prompt || '',
        backPrompt: values.prompt || ''
    };
}

async function saveAssetVersion(assetId) {
    const form = document.getElementById('assetDetailForm');
    const button = form.querySelector('button[type="submit"]');
    const values = Object.fromEntries(new FormData(form).entries());
    button.disabled = true;
    try {
        const updated = await requestJson(`/api/visual-assets/${encodeURIComponent(assetId)}/versions`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(buildVersionPayload(values))
        });
        state.selectedAsset = updated;
        renderAssetDetail(updated);
        await loadAssets();
        setStatus('资产新版本保存成功。');
    } catch (error) {
        document.getElementById('detailStatus').textContent = `保存失败：${error.message}`;
        document.getElementById('detailStatus').classList.add('error');
    } finally {
        if (button) button.disabled = false;
    }
}

function populateReuseNovels() {
    reuseNovelSelect.innerHTML = state.novels.map(novel => `<option value="${escapeHtml(novelIdOf(novel))}">${escapeHtml(novelTitleOf(novel))}</option>`).join('');
    reuseNovelSelect.addEventListener('change', () => loadTargetChapters(reuseNovelSelect.value));
}

async function loadTargetChapters(targetNovelId) {
    reuseChapterSelect.innerHTML = '<option value="">正在加载章节...</option>';
    try {
        const data = await requestJson(`/api/novel/${encodeURIComponent(targetNovelId)}/chapters`);
        const chapters = normalizeList(data, ['records', 'list', 'chapters', 'chapterList']);
        reuseChapterSelect.innerHTML = chapters.map(chapter => {
            const num = chapter.chapterNum ?? chapter.chapterId ?? chapter.id;
            return `<option value="${escapeHtml(num)}">第 ${escapeHtml(num)} 章：${escapeHtml(chapter.chapterTitle || '未命名章节')}</option>`;
        }).join('') || '<option value="">暂无章节</option>';
        await loadTargetAssets();
    } catch (error) {
        reuseChapterSelect.innerHTML = '<option value="">章节加载失败</option>';
        reuseStatus.textContent = error.message;
        reuseStatus.classList.add('error');
    }
}

async function loadTargetAssets() {
    const targetNovelId = reuseNovelSelect.value;
    const targetChapterNum = reuseChapterSelect.value;
    const assetRole = reuseRoleSelect.value;
    if (!targetNovelId || !targetChapterNum) {
        reuseAssetSelect.innerHTML = '<option value="">请先选择目标章节</option>';
        return;
    }
    reuseAssetSelect.innerHTML = '<option value="">正在加载目标资产...</option>';
    try {
        const data = await requestJson(`/api/novel/${encodeURIComponent(targetNovelId)}/assets?assetType=${encodeURIComponent(assetRole)}`);
        const assets = normalizeList(unwrapResponse(data), ['records', 'list', 'assets'])
            .filter(asset => asset.assetType === assetRole);
        reuseAssetSelect.innerHTML = assets.map(asset =>
            `<option value="${escapeHtml(asset.assetId)}">${escapeHtml(asset.assetName || '未命名资产')} · v${escapeHtml(asset.version || 1)}</option>`
        ).join('') || '<option value="">该章节暂无对应类型资产</option>';
    } catch (error) {
        reuseAssetSelect.innerHTML = '<option value="">目标资产加载失败</option>';
        reuseStatus.textContent = error.message;
        reuseStatus.classList.add('error');
    }
}

function openReuseDialog(asset) {
    state.selectedAsset = asset;
    reuseAssetName.textContent = `${asset.assetName || '未命名资产'} · v${asset.version || 1}`;
    reuseRoleSelect.value = asset.assetType || 'CHARACTER';
    reuseStatus.textContent = '';
    reuseStatus.classList.remove('error');
    reuseModal.hidden = false;
    populateReuseNovels();
    if (reuseNovelSelect.value) loadTargetChapters(reuseNovelSelect.value);
}

function closeReuseDialog() {
    reuseModal.hidden = true;
}

function buildReusePayload(values) {
    if (!values.targetNovelId || !values.targetChapterNum || !values.targetAssetId) {
        throw new Error('请选择目标小说、章节和具体资产');
    }
    return {
        targetNovelId: Number(values.targetNovelId),
        targetChapterNum: Number(values.targetChapterNum),
        targetAssetId: Number(values.targetAssetId),
        assetRole: values.assetRole || 'CHARACTER'
    };
}

async function submitReuse(assetId) {
    try {
        const payload = buildReusePayload({
            targetNovelId: reuseNovelSelect.value,
            targetChapterNum: reuseChapterSelect.value,
            assetRole: reuseRoleSelect.value,
            targetAssetId: reuseAssetSelect.value
        });
        submitReuseBtn.disabled = true;
        await requestJson(`/api/visual-assets/${encodeURIComponent(assetId)}/reuse`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        closeReuseDialog();
        setStatus('目标资产已覆盖成功，新的提示词和三视图已生效。');
        await loadAssets();
    } catch (error) {
        reuseStatus.textContent = `覆盖失败：${error.message}`;
        reuseStatus.classList.add('error');
    } finally {
        submitReuseBtn.disabled = false;
    }
}

function openImagePreview(image) {
    imagePreview.src = image.src;
    imagePreview.alt = image.alt || '资产三视图预览';
    imagePreviewModal.hidden = false;
    document.body.classList.add('image-preview-open');
    closeImageBtn.focus();
}

function closeImagePreview() {
    imagePreviewModal.hidden = true;
    imagePreview.removeAttribute('src');
    document.body.classList.remove('image-preview-open');
}

assetSearchBtn.addEventListener('click', () => {
    state.search = assetSearchInput.value.trim();
    state.assetType = assetTypeFilter.value;
    state.page = 1;
    loadAssets();
});
assetSearchInput.addEventListener('keydown', event => {
    if (event.key === 'Enter') assetSearchBtn.click();
});
assetTypeFilter.addEventListener('change', () => assetSearchBtn.click());
novelSearchInput.addEventListener('input', renderNovelFilters);
reuseRoleSelect.addEventListener('change', loadTargetAssets);
reuseChapterSelect.addEventListener('change', loadTargetAssets);
closeReuseBtn.addEventListener('click', closeReuseDialog);
cancelReuseBtn.addEventListener('click', closeReuseDialog);
submitReuseBtn.addEventListener('click', () => state.selectedAsset && submitReuse(state.selectedAsset.assetId));
reuseModal.addEventListener('click', event => {
    if (event.target === reuseModal) closeReuseDialog();
});
closeImageBtn.addEventListener('click', closeImagePreview);
imagePreviewModal.addEventListener('click', event => {
    if (event.target === imagePreviewModal) closeImagePreview();
});
document.addEventListener('keydown', event => {
    if (event.key === 'Escape') {
        if (!reuseModal.hidden) closeReuseDialog();
        if (!imagePreviewModal.hidden) closeImagePreview();
    }
});

loadNovels().then(loadAssets);

if (typeof module !== 'undefined') {
    module.exports = {buildAssetQuery, normalizeLibraryResponse, buildVersionPayload, buildReusePayload};
}
