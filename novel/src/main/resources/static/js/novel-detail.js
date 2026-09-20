const searchParams = new URLSearchParams(window.location.search);
const novelId = searchParams.get('novelId');
const autoGenerate = searchParams.get('autoGenerate') === '1';
const novelTitle = document.getElementById('novelTitle');
const generationStatus = document.getElementById('generationStatus');
const readerLayout = document.getElementById('readerLayout');
const chapterNav = document.getElementById('chapterNav');
const chapterTitle = document.getElementById('chapterTitle');
const chapterContent = document.getElementById('chapterContent');
const backCreateBtn = document.getElementById('backCreateBtn');
const backHistoryBtn = document.getElementById('backHistoryBtn');
const downloadBtn = document.getElementById('downloadBtn');
const importNovelBtn = document.getElementById('importNovelBtn');
const readerEntryState = document.getElementById('readerEntryState');
const readerEntryImportBtn = document.getElementById('readerEntryImportBtn');
const readerHistoryBtn = document.getElementById('readerHistoryBtn');
const readerImportDialog = document.getElementById('readerImportDialog');
const readerImportFile = document.getElementById('readerImportFile');
const readerImportTitleInput = document.getElementById('readerImportTitleInput');
const readerImportStatus = document.getElementById('readerImportStatus');
const readerImportSubmitBtn = document.getElementById('readerImportSubmitBtn');
const closeReaderImportBtn = document.getElementById('closeReaderImportBtn');
const cancelReaderImportBtn = document.getElementById('cancelReaderImportBtn');
const saveChapterBtn = document.getElementById('saveChapterBtn');
const regenerateChapterBtn = document.getElementById('regenerateChapterBtn');
const generateStoryboardBtn = document.getElementById('generateStoryboardBtn');
const saveStoryboardBtn = document.getElementById('saveStoryboardBtn');
const goVideoGenerationBtn = document.getElementById('goVideoGenerationBtn');
const exportStoryboardBtn = document.getElementById('exportStoryboardBtn');
const storyboardStatus = document.getElementById('storyboardStatus');
const storyboardSummary = document.getElementById('storyboardSummary');
const storyboardEmpty = document.getElementById('storyboardEmpty');
const storyboardList = document.getElementById('storyboardList');
const storyboardTabBtn = document.getElementById('storyboardTabBtn');
const assetTabBtn = document.getElementById('assetTabBtn');
const storyboardTabPanel = document.getElementById('storyboardTabPanel');
const assetTabPanel = document.getElementById('assetTabPanel');
const extractAssetsBtn = document.getElementById('extractAssetsBtn');
const refreshAssetsBtn = document.getElementById('refreshAssetsBtn');
const assetTaskSummary = document.getElementById('assetTaskSummary');
const assetStatus = document.getElementById('assetStatus');
const assetEmpty = document.getElementById('assetEmpty');
const assetListElement = document.getElementById('assetList');
const assetFilters = Array.from(document.querySelectorAll('.asset-filter'));
const imagePreviewModal = document.getElementById('imagePreviewModal');
const imagePreview = document.getElementById('imagePreview');
const closeImagePreviewBtn = document.getElementById('closeImagePreviewBtn');
const visualStyleStatus = document.getElementById('visualStyleStatus');
const generateVisualStyleBtn = document.getElementById('generateVisualStyleBtn');
const saveVisualStyleBtn = document.getElementById('saveVisualStyleBtn');
const visualStyleFields = {
    era: document.getElementById('visualStyleEra'),
    region: document.getElementById('visualStyleRegion'),
    architecture: document.getElementById('visualStyleArchitecture'),
    material: document.getElementById('visualStyleMaterial'),
    colorStyle: document.getElementById('visualStyleColorStyle'),
    lightingStyle: document.getElementById('visualStyleLightingStyle'),
    artStyle: document.getElementById('visualStyleArtStyle'),
    cameraStyle: document.getElementById('visualStyleCameraStyle'),
    positivePrompt: document.getElementById('visualStylePositivePrompt'),
    negativePrompt: document.getElementById('visualStyleNegativePrompt')
};
const videoNavigationModal = document.getElementById('videoNavigationModal');
const saveAndGoVideoBtn = document.getElementById('saveAndGoVideoBtn');
const discardAndGoVideoBtn = document.getElementById('discardAndGoVideoBtn');
const cancelVideoNavigationBtn = document.getElementById('cancelVideoNavigationBtn');

let chapters = [];
let activeChapter = null;
let timer = null;
let autoJumpToLatestChapter = autoGenerate;
let activeStoryboard = null;
let loadedStoryboardChapterNum = null;
let assetTaskTimer = null;
let activeAssetTaskId = null;
let chapterAssets = [];
let allNovelAssets = [];
let activeAssetType = '';
const compositeImageTimers = new Map();

function openImagePreview(image) {
    imagePreview.src = image.src;
    imagePreview.alt = image.alt || '三视图大图预览';
    imagePreviewModal.hidden = false;
    document.body.classList.add('image-preview-open');
    closeImagePreviewBtn.focus();
}

function closeImagePreview() {
    imagePreviewModal.hidden = true;
    imagePreview.removeAttribute('src');
    document.body.classList.remove('image-preview-open');
}

closeImagePreviewBtn.addEventListener('click', closeImagePreview);
imagePreviewModal.addEventListener('click', event => {
    if (event.target === imagePreviewModal) {
        closeImagePreview();
    }
});
document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && !imagePreviewModal.hidden) {
        closeImagePreview();
    }
});

backCreateBtn.addEventListener('click', () => {
    window.location.href = `/outline?restoreNovelId=${encodeURIComponent(novelId)}`;
});
backHistoryBtn.addEventListener('click', () => window.location.href = '/history-list-new.html');
downloadBtn.addEventListener('click', () => {
    if (novelId) {
        window.location.href = `/api/novel/${encodeURIComponent(novelId)}/download`;
    }
});
saveChapterBtn.addEventListener('click', saveActiveChapter);
regenerateChapterBtn.addEventListener('click', regenerateActiveChapter);
generateStoryboardBtn.addEventListener('click', generateStoryboard);
saveStoryboardBtn.addEventListener('click', saveStoryboard);
goVideoGenerationBtn.addEventListener('click', goToVideoGeneration);
saveAndGoVideoBtn.addEventListener('click', async () => {
    await saveStoryboard();
    if (!hasUnsavedStoryboardChanges()) {
        goToVideoGenerationPage();
    }
});
discardAndGoVideoBtn.addEventListener('click', goToVideoGenerationPage);
cancelVideoNavigationBtn.addEventListener('click', closeVideoNavigationModal);
saveVisualStyleBtn.addEventListener('click', saveVisualStyle);
generateVisualStyleBtn.addEventListener('click', generateVisualStyle);
exportStoryboardBtn.addEventListener('click', exportStoryboard);
storyboardList.addEventListener('input', event => {
    if (event.target.matches('[data-field="shotPlan"]')) {
        refreshStoryboardTimeline();
    }
});
storyboardTabBtn.addEventListener('click', () => switchAssetTab(false));
assetTabBtn.addEventListener('click', () => switchAssetTab(true));
extractAssetsBtn.addEventListener('click', extractAssets);
refreshAssetsBtn.addEventListener('click', () => Promise.all([loadChapterAssets(true), loadAllNovelAssets(true)]));
assetFilters.forEach(button => button.addEventListener('click', () => {
    activeAssetType = button.dataset.assetType || '';
    assetFilters.forEach(item => item.classList.toggle('active', item === button));
    renderAssets();
}));

function openReaderImportDialog() {
    if (!readerImportDialog) return;
    readerImportStatus.textContent = '';
    readerImportStatus.classList.remove('error');
    readerImportDialog.hidden = false;
    readerImportFile.focus();
}

function closeReaderImportDialog() {
    if (!readerImportDialog) return;
    readerImportDialog.hidden = true;
}

function setReaderImportStatus(message, isError = false) {
    if (!readerImportStatus) return;
    readerImportStatus.textContent = message || '';
    readerImportStatus.classList.toggle('error', isError);
}

async function importNovelFromReader() {
    const file = readerImportFile && readerImportFile.files && readerImportFile.files[0];
    if (!file) {
        setReaderImportStatus('请选择 TXT 或 DOCX 文件。', true);
        return;
    }
    if (!/\.(txt|docx)$/i.test(file.name)) {
        setReaderImportStatus('暂只支持 TXT 和 DOCX 文件。', true);
        return;
    }

    const formData = new FormData();
    formData.append('file', file);
    formData.append('novelTitle', readerImportTitleInput.value.trim());
    readerImportSubmitBtn.disabled = true;
    setReaderImportStatus('正在读取小说并识别章节，请稍候...');
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
        try {
            localStorage.setItem('mirror:selectedNovelId', String(result.novelId));
        } catch (_) { }
        window.location.href = `/novel-detail-new.html?novelId=${encodeURIComponent(result.novelId)}`;
    } catch (error) {
        setReaderImportStatus(error.message, true);
    } finally {
        readerImportSubmitBtn.disabled = false;
    }
}

if (importNovelBtn) importNovelBtn.addEventListener('click', openReaderImportDialog);
if (readerEntryImportBtn) readerEntryImportBtn.addEventListener('click', openReaderImportDialog);
if (readerHistoryBtn) readerHistoryBtn.addEventListener('click', () => window.location.href = '/history-list-new.html');
if (readerImportSubmitBtn) readerImportSubmitBtn.addEventListener('click', importNovelFromReader);
if (closeReaderImportBtn) closeReaderImportBtn.addEventListener('click', closeReaderImportDialog);
if (cancelReaderImportBtn) cancelReaderImportBtn.addEventListener('click', closeReaderImportDialog);
if (readerImportDialog) readerImportDialog.addEventListener('click', event => {
    if (event.target === readerImportDialog) closeReaderImportDialog();
});
document.addEventListener('keydown', event => {
    if (event.key === 'Escape' && readerImportDialog && !readerImportDialog.hidden) {
        closeReaderImportDialog();
    }
});

if (!novelId) {
    novelTitle.textContent = '加载完成';
    generationStatus.textContent = '请选择或导入一部小说后开始创作。';
    if (readerEntryState) readerEntryState.hidden = false;
    readerLayout.hidden = true;
    if (importNovelBtn) importNovelBtn.hidden = true;
    if (downloadBtn) downloadBtn.hidden = true;
} else {
    if (readerEntryState) readerEntryState.hidden = true;
    if (importNovelBtn) importNovelBtn.hidden = false;
    if (downloadBtn) downloadBtn.hidden = false;
    if (autoGenerate) {
        startFullGenerationFromDetailPage();
    }
    loadVisualStyle();
    pollStatus();
}

async function loadVisualStyle() {
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/visual-style`);
        if (!response.ok) throw new Error(`请求失败：${response.status}`);
        const style = unwrapResponse(await response.json()) || {};
        Object.keys(visualStyleFields).forEach(key => {
            visualStyleFields[key].value = style[key] || '';
        });
        visualStyleStatus.textContent = style.version ? `已保存 v${style.version}` : '尚未配置';
    } catch (error) {
        visualStyleStatus.textContent = `加载失败：${error.message}`;
    }
}

async function saveVisualStyle() {
    saveVisualStyleBtn.disabled = true;
    visualStyleStatus.textContent = '正在保存...';
    try {
        const payload = {};
        Object.keys(visualStyleFields).forEach(key => {
            payload[key] = visualStyleFields[key].value.trim();
        });
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/visual-style`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || `请求失败：${response.status}`);
        const style = unwrapResponse(data) || {};
        visualStyleStatus.textContent = style.version ? `已保存 v${style.version}` : '已保存';
    } catch (error) {
        visualStyleStatus.textContent = `保存失败：${error.message}`;
    } finally {
        saveVisualStyleBtn.disabled = false;
    }
}

async function generateVisualStyle() {
    generateVisualStyleBtn.disabled = true;
    saveVisualStyleBtn.disabled = true;
    visualStyleStatus.textContent = 'AI 正在分析全书内容...';
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/visual-style/generate`, {
            method: 'POST'
        });
        const data = await response.json();
        if (!response.ok) throw new Error(data.message || `请求失败：${response.status}`);
        const style = unwrapResponse(data) || {};
        Object.keys(visualStyleFields).forEach(key => {
            visualStyleFields[key].value = style[key] || '';
        });
        visualStyleStatus.textContent = 'AI 已生成，请确认后点击保存';
    } catch (error) {
        visualStyleStatus.textContent = `生成失败：${error.message}`;
    } finally {
        generateVisualStyleBtn.disabled = false;
        saveVisualStyleBtn.disabled = false;
    }
}

async function startFullGenerationFromDetailPage() {
    const payloadKey = `fullGenerationPayload:${novelId}`;
    const startedKey = `fullGenerationStarted:${novelId}`;
    const raw = sessionStorage.getItem(payloadKey);

    if (!raw || sessionStorage.getItem(startedKey) === '1') {
        return;
    }

    sessionStorage.setItem(startedKey, '1');
    try {
        const payload = JSON.parse(raw);
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/generateFull`, {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify({
                novelId,
                outline: payload.outline
            })
        });
        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || `请求失败：${response.status}`);
        }
    } catch (error) {
        sessionStorage.removeItem(startedKey);
        generationStatus.hidden = false;
        generationStatus.textContent = `启动全文生成失败：${error.message}`;
    }
}

async function pollStatus() {
    await loadStatus();
    timer = setInterval(loadStatus, 2000);
}

async function loadStatus() {
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/generateStatus`);
        if (!response.ok) {
            throw new Error(`请求失败：${response.status}`);
        }

        const status = unwrapResponse(await response.json()) || {};
        novelTitle.textContent = status.novelTitle || `作品 ${novelId}`;
        novelTitle.title = novelTitle.textContent;
        const workspaceTitle = document.querySelector('.workbench-context h1');
        if (workspaceTitle) workspaceTitle.textContent = '加载完成';

        if (Array.isArray(status.chapterList) || Array.isArray(status.chapters)) {
            setChapters(status.chapterList || status.chapters);
        }

        if (status.status === 'FULL_GENERATED') {
            clearInterval(timer);
            generationStatus.hidden = true;
            await loadChapters();
            return;
        }

        if (status.status === 'FAILED') {
            clearInterval(timer);
            generationStatus.hidden = false;
            generationStatus.textContent = status.errorMessage || status.message || '全文生成失败。';
            return;
        }

        const currentChapter = status.currentChapter || chapters.length || 1;
        const totalChapter = status.totalChapter ? ` / ${status.totalChapter}` : '';
        generationStatus.hidden = false;
        generationStatus.textContent = `正在生成第 ${currentChapter}${totalChapter} 章内容，请稍候...`;

        await loadChapters(true);
    } catch (error) {
        generationStatus.hidden = false;
        generationStatus.textContent = `加载失败：${error.message}`;
    }
}

async function loadChapters(quiet = false) {
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters`);
        if (!response.ok) {
            if (!quiet) {
                generationStatus.hidden = false;
                generationStatus.textContent = `章节加载失败：${response.status}`;
            }
            return;
        }

        const data = unwrapResponse(await response.json());
        const nextChapters = normalizeChapterList(data);
        if (!nextChapters.length && quiet) {
            return;
        }

        setChapters(nextChapters);
    } catch (error) {
        if (!quiet) {
            generationStatus.hidden = false;
            generationStatus.textContent = `章节加载失败：${error.message}`;
        }
    }
}

function setChapters(nextChapters) {
    const previousLength = chapters.length;
    chapters = normalizeChapterList(nextChapters);
    renderChapterNav();
    readerLayout.hidden = !chapters.length;

    if (!chapters.length) {
        chapterTitle.textContent = '暂无章节';
        chapterContent.value = '';
        return;
    }

    const activeId = activeChapter && getChapterId(activeChapter);
    const nextActive = autoJumpToLatestChapter && chapters.length > previousLength
        ? chapters[chapters.length - 1]
        : chapters.find(chapter => getChapterId(chapter) === activeId) || chapters[0];

    // Status polling refreshes the chapter list while a full generation is running.
    // Do not reload the same active chapter, otherwise storyboard scroll position is lost.
    const currentActiveId = activeChapter && getChapterId(activeChapter);
    if (currentActiveId && currentActiveId === getChapterId(nextActive)
        || getChapterNumber(activeChapter) === getChapterNumber(nextActive)) {
        return;
    }
    selectChapter(nextActive);
}

function renderChapterNav() {
    chapterNav.innerHTML = '';
    chapters.forEach((chapter, index) => {
        const button = document.createElement('button');
        button.type = 'button';
        button.textContent = `第 ${chapter.chapterNum || index + 1} 章：${chapter.chapterTitle || '未命名章节'}`;
        if (activeChapter && getChapterId(chapter) === getChapterId(activeChapter)) {
            button.classList.add('active');
        }
        button.addEventListener('click', () => selectChapter(chapter));
        chapterNav.appendChild(button);
    });
}

async function selectChapter(chapter) {
    activeChapter = chapter;
    renderChapterNav();

    const chapterId = getChapterId(chapter);
    let detail = chapter;

    if (chapterId && !chapter.content && !chapter.chapterText) {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterId)}`);
        if (response.ok) {
            detail = unwrapResponse(await response.json()) || chapter;
        }
    }

    activeChapter = detail;
    chapterTitle.textContent = detail.chapterTitle || chapter.chapterTitle || '未命名章节';
    chapterContent.value = detail.content || detail.chapterText || '';
    stopAssetPolling();
    chapterAssets = [];
    allNovelAssets = [];
    assetTaskSummary.textContent = '当前章节未提取';
    assetListElement.innerHTML = '';
    assetEmpty.hidden = false;
    assetEmpty.textContent = '选择章节后，可以提取本章人物和场景资产。';
    await loadStoryboard();
    if (!assetTabPanel.hidden) {
        await Promise.all([loadChapterAssets(), loadAllNovelAssets()]);
    }
}

async function loadStoryboard() {
    clearStoryboardStatus();
    const chapterNum = getChapterNumber(activeChapter);
    if (!novelId || !chapterNum) {
        activeStoryboard = null;
        loadedStoryboardChapterNum = null;
        storyboardList.innerHTML = '';
        storyboardEmpty.hidden = false;
        storyboardEmpty.textContent = '当前章节缺少编号，无法加载分镜脚本。';
        storyboardSummary.textContent = '不可用';
        return;
    }

    const sameStoryboardChapter = loadedStoryboardChapterNum === String(chapterNum);
    if (!sameStoryboardChapter || !activeStoryboard) {
        activeStoryboard = null;
        storyboardList.innerHTML = '';
        storyboardEmpty.hidden = false;
        storyboardEmpty.textContent = '正在读取本章已保存的分镜脚本...';
        storyboardSummary.textContent = '读取中';
        saveStoryboardBtn.hidden = true;
        goVideoGenerationBtn.hidden = true;
        exportStoryboardBtn.hidden = true;
    }

    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard`);
        if (!response.ok) {
            throw new Error(`请求失败：${response.status}`);
        }
        const storyboard = unwrapResponse(await response.json());
        if (!storyboard) {
            activeStoryboard = null;
            loadedStoryboardChapterNum = String(chapterNum);
            storyboardList.innerHTML = '';
            storyboardEmpty.hidden = false;
            storyboardEmpty.textContent = '本章还没有分镜脚本，点击上方按钮生成。';
            storyboardSummary.textContent = '尚未生成';
            return;
        }
        activeStoryboard = storyboard;
        loadedStoryboardChapterNum = String(chapterNum);
        renderStoryboard(storyboard);
    } catch (error) {
        storyboardEmpty.textContent = `读取分镜失败：${error.message}`;
        storyboardSummary.textContent = '读取失败';
    }
}

function switchAssetTab(showAssets) {
    storyboardTabPanel.hidden = showAssets;
    assetTabPanel.hidden = !showAssets;
    storyboardTabBtn.classList.toggle('active', !showAssets);
    assetTabBtn.classList.toggle('active', showAssets);
    assetTabBtn.classList.toggle('secondary-button', !showAssets);
    storyboardTabBtn.classList.toggle('secondary-button', showAssets);
    storyboardTabBtn.setAttribute('aria-selected', String(!showAssets));
    assetTabBtn.setAttribute('aria-selected', String(showAssets));
    if (showAssets) {
        Promise.all([loadChapterAssets(), loadAllNovelAssets()]);
    }
}

async function loadChapterAssets(showError = false) {
    const chapterNum = getChapterNumber(activeChapter);
    if (!novelId || !chapterNum) {
        return;
    }
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/assets`);
        if (!response.ok) {
            throw new Error(`请求失败：${response.status}`);
        }
        chapterAssets = unwrapResponse(await response.json()) || [];
        assetEmpty.hidden = chapterAssets.length > 0;
        assetEmpty.textContent = chapterAssets.length ? '' : '本章还没有资产，点击“提取人物/场景”开始。';
        assetTaskSummary.textContent = chapterAssets.length
            ? `${chapterAssets.length} 个本章资产`
            : '当前章节未提取';
        renderAssets();
        if (activeStoryboard && !assetTabPanel.hidden) {
            renderStoryboard(activeStoryboard);
        }
    } catch (error) {
        if (showError || !assetTabPanel.hidden) {
            setAssetStatus(`读取资产失败：${error.message}`, true);
        }
    }
}

async function loadAllNovelAssets(showError = false) {
    if (!novelId) {
        return;
    }
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/assets`);
        if (!response.ok) {
            throw new Error(`请求失败：${response.status}`);
        }
        allNovelAssets = unwrapResponse(await response.json()) || [];
        renderAssets();
    } catch (error) {
        if (showError || !assetTabPanel.hidden) {
            setAssetStatus(`读取资产库失败：${error.message}`, true);
        }
    }
}

async function extractAssets() {
    const chapterNum = getChapterNumber(activeChapter);
    if (!activeChapter || !chapterNum || !chapterContent.value.trim()) {
        setAssetStatus('当前章节没有正文，无法提取资产。', true);
        return;
    }
    if (!activeStoryboard || !Array.isArray(activeStoryboard.scenes) || !activeStoryboard.scenes.length) {
        setAssetStatus('请先生成本章分镜脚本。', true);
        return;
    }

    extractAssetsBtn.disabled = true;
    setAssetStatus('正在提交资产提取任务...', false);
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/assets/extract`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({chapterNum, forceRetry: false})
        });
        if (!response.ok) {
            throw new Error(`请求失败：${response.status}`);
        }
        const task = unwrapResponse(await response.json());
        activeAssetTaskId = task.taskId;
        await pollAssetTask();
    } catch (error) {
        setAssetStatus(`提取失败：${error.message}`, true);
    } finally {
        extractAssetsBtn.disabled = false;
    }
}

async function pollAssetTask() {
    stopAssetPolling();
    if (!activeAssetTaskId) {
        return;
    }
    const check = async () => {
        try {
            const response = await fetch(`/api/asset-tasks/${encodeURIComponent(activeAssetTaskId)}`);
            if (!response.ok) {
                throw new Error(`请求失败：${response.status}`);
            }
            const task = unwrapResponse(await response.json());
            assetTaskSummary.textContent = `${task.status} · 新建 ${task.created || 0} · 复用 ${task.reused || 0} · 失败 ${task.failed || 0}`;
            if (['COMPLETED', 'PARTIAL_FAILED', 'FAILED'].includes(task.status)) {
                stopAssetPolling();
                setAssetStatus(task.message || (task.status === 'COMPLETED' ? '资产提取完成。' : '资产提取结束，请检查失败项。'), task.status === 'FAILED');
                await Promise.all([loadChapterAssets(true), loadAllNovelAssets(true)]);
                await loadStoryboard();
                return;
            }
            setAssetStatus(task.message || '正在提取人物和场景，请稍候...', false);
        } catch (error) {
            stopAssetPolling();
            setAssetStatus(`任务状态读取失败：${error.message}`, true);
        }
    };
    await check();
    if (activeAssetTaskId) {
        assetTaskTimer = setInterval(check, 1800);
    }
}

function stopAssetPolling() {
    if (assetTaskTimer) {
        clearInterval(assetTaskTimer);
        assetTaskTimer = null;
    }
}

function renderAssets() {
    const sourceAssets = allNovelAssets.length ? allNovelAssets : chapterAssets;
    const visibleAssets = sourceAssets.filter(asset => !activeAssetType || asset.assetType === activeAssetType);
    assetListElement.innerHTML = '';
    visibleAssets.forEach(asset => {
        const card = document.createElement('details');
        card.className = 'asset-card';
        card.open = true;
        card.dataset.assetId = String(asset.assetId);
        card.innerHTML = `
            <summary class="asset-card-head">
                <strong>${escapeHtml(asset.assetName || '未命名资产')} · v${escapeHtml(asset.version || 1)}</strong>
                <span class="asset-type">${asset.assetType === 'LOCATION' ? '场景' : '人物'}</span>
                <span class="asset-card-toggle" aria-hidden="true">收起</span>
            </summary>
            <div class="asset-card-body">
                <p class="asset-meta">${escapeHtml(asset.coreFeatures || '暂无核心特征')} · 已复用 ${escapeHtml(asset.reuseCount || 0)} 次</p>
                <label>提示词<textarea data-asset-field="prompt">${escapeHtml(assetPromptValue(asset))}</textarea></label>
                ${asset.compositeImagePath
                    ? `<img class="asset-composite-image" src="${escapeHtml(asset.compositeImagePath)}" alt="${escapeHtml(asset.assetName || '资产')}三视图">`
                    : '<div class="asset-view-placeholder asset-view-placeholder-wide">三视图合成图<br><small>图片待生成</small></div>'}
                <div class="asset-card-actions">
                    <button type="button" data-action="save-asset">保存修改</button>
                    <button type="button" data-action="generate-composite-image">生成三视图</button>
                    <button type="button" class="secondary-button" data-action="merge-asset">合并到已有资产</button>
                </div>
            </div>
        `;
        card.querySelector('[data-action="save-asset"]').addEventListener('click', () => saveAsset(card, asset));
        card.querySelector('[data-action="generate-composite-image"]').addEventListener('click', () => generateCompositeImage(card, asset));
        card.querySelector('[data-action="merge-asset"]').addEventListener('click', () => mergeAsset(asset));
        const compositeImage = card.querySelector('.asset-composite-image');
        if (compositeImage) {
            compositeImage.addEventListener('click', () => openImagePreview(compositeImage));
        }
        assetListElement.appendChild(card);
    });
}

function assetPromptValue(asset) {
    return asset.frontPrompt || asset.sidePrompt || asset.backPrompt || '';
}

function assetPromptPayload(card, asset) {
    const prompt = card.querySelector('[data-asset-field="prompt"]').value;
    return {
        ...asset,
        frontPrompt: prompt,
        sidePrompt: prompt,
        backPrompt: prompt
    };
}

async function generateCompositeImage(card, asset) {
    const button = card.querySelector('[data-action="generate-composite-image"]');
    const payload = assetPromptPayload(card, asset);
    button.disabled = true;
    button.textContent = '提交中...';
    setAssetStatus('正在提交三视图生成任务...', false);
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/assets/${encodeURIComponent(asset.assetId)}/composite-image`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            throw new Error(`请求失败：${response.status}`);
        }
        const task = unwrapResponse(await response.json());
        pollCompositeImage(task.taskId, asset.assetId, button);
    } catch (error) {
        button.disabled = false;
        button.textContent = '生成三视图';
        setAssetStatus(`三视图生成失败：${error.message}`, true);
    }
}

function pollCompositeImage(taskId, assetId, button) {
    stopCompositeImagePolling(assetId);
    const check = async () => {
        try {
            const response = await fetch(`/api/composite-image-tasks/${encodeURIComponent(taskId)}`);
            if (!response.ok) {
                throw new Error(`请求失败：${response.status}`);
            }
            const task = unwrapResponse(await response.json());
            if (task.status === 'COMPLETED') {
                stopCompositeImagePolling(assetId);
                setAssetStatus('三视图生成完成。', false);
                await Promise.all([loadChapterAssets(true), loadAllNovelAssets(true)]);
                return;
            }
            if (task.status === 'FAILED') {
                stopCompositeImagePolling(assetId);
                button.disabled = false;
                button.textContent = '生成三视图';
                setAssetStatus(`三视图生成失败：${task.errorMessage || '图片接口处理失败'}`, true);
                return;
            }
            button.textContent = '生成中...';
            setAssetStatus('正在生成三视图，请稍候...', false);
            compositeImageTimers.set(assetId, window.setTimeout(check, 1800));
        } catch (error) {
            stopCompositeImagePolling(assetId);
            button.disabled = false;
            button.textContent = '生成三视图';
            setAssetStatus(`读取三视图任务失败：${error.message}`, true);
        }
    };
    check();
}

function stopCompositeImagePolling(assetId) {
    const timer = compositeImageTimers.get(assetId);
    if (timer) {
        window.clearTimeout(timer);
        compositeImageTimers.delete(assetId);
    }
}

async function saveAsset(card, asset) {
    const payload = assetPromptPayload(card, asset);
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/assets/${encodeURIComponent(asset.assetId)}`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify(payload)
        });
        if (!response.ok) {
            throw new Error(`请求失败：${response.status}`);
        }
        const updated = unwrapResponse(await response.json());
        chapterAssets = chapterAssets.map(item => item.assetId === updated.assetId ? updated : item);
        allNovelAssets = allNovelAssets.map(item => item.assetId === updated.assetId ? updated : item);
        renderAssets();
        setAssetStatus('资产修改已保存。', false);
    } catch (error) {
        setAssetStatus(`保存资产失败：${error.message}`, true);
    }
}

async function mergeAsset(asset) {
    const candidates = chapterAssets.filter(item => item.assetId !== asset.assetId);
    if (!candidates.length) {
        setAssetStatus('没有可合并的目标资产。', true);
        return;
    }
    const targetId = window.prompt(`请输入目标资产 ID：\n${candidates.map(item => `${item.assetId}：${item.assetName}`).join('\n')}`);
    if (!targetId || !window.confirm(`确认将“${asset.assetName}”合并到资产 ${targetId} 吗？`)) {
        return;
    }
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/assets/${encodeURIComponent(asset.assetId)}/merge`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({targetAssetId: Number(targetId), targetVersion: 1})
        });
        if (!response.ok) {
            throw new Error(`请求失败：${response.status}`);
        }
        await Promise.all([loadChapterAssets(true), loadAllNovelAssets(true)]);
        await loadStoryboard();
        setAssetStatus('资产合并完成。', false);
    } catch (error) {
        setAssetStatus(`合并资产失败：${error.message}`, true);
    }
}

function setAssetStatus(message, error) {
    assetStatus.textContent = message || '';
    assetStatus.classList.toggle('error', Boolean(error));
}

async function generateStoryboard() {
    if (!activeChapter) {
        alert('请先选择章节。');
        return;
    }
    const chapterNum = getChapterNumber(activeChapter);
    const chapterText = chapterContent.value.trim();
    if (!chapterNum || !chapterText) {
        alert('当前章节没有正文，无法生成分镜脚本。');
        return;
    }

    setStoryboardBusy(true, '正在根据本章正文生成分镜脚本，请稍候...');
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/generate`, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({
                novelId: Number(novelId),
                chapterNum,
                chapterTitle: chapterTitle.textContent,
                chapterText,
                targetDurationSec: 10,
                visualStyle: '电影感写实',
                aspectRatio: '16:9'
            })
        });
        if (!response.ok) {
            const message = await response.text();
            throw new Error(message || `请求失败：${response.status}`);
        }
        activeStoryboard = unwrapResponse(await response.json());
        renderStoryboard(activeStoryboard);
        setStoryboardStatus('分镜脚本生成成功。', false);
    } catch (error) {
        setStoryboardStatus(`生成失败：${error.message}`, true);
        storyboardEmpty.hidden = false;
        storyboardEmpty.textContent = '生成失败，请检查后端日志后重试。';
    } finally {
        setStoryboardBusy(false);
    }
}

async function saveStoryboard() {
    if (!activeStoryboard || !activeChapter) {
        return;
    }
    const chapterNum = getChapterNumber(activeChapter);
    const scenes = Array.from(storyboardList.querySelectorAll('.storyboard-card')).map(card => {
        const scene = activeStoryboard.scenes.find(item => String(item.id) === card.dataset.sceneId);
        return {...scene,
            shotPlan: card.querySelector('[data-field="shotPlan"]').value
        };
    });

    if (scenes.some(scene => !isValidShotPlan(scene.shotPlan))) {
        setStoryboardStatus('镜头脚本格式不正确：请使用“时长 0:00-0:05”，并保证镜头时间连续、无重叠。', true);
        return;
    }

    saveStoryboardBtn.disabled = true;
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard`, {
            method: 'PUT',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({...activeStoryboard, scenes})
        });
        if (!response.ok) {
            throw new Error(`请求失败：${response.status}`);
        }
        activeStoryboard = unwrapResponse(await response.json());
        renderStoryboard(activeStoryboard);
        setStoryboardStatus('分镜修改已保存。', false);
    } catch (error) {
        setStoryboardStatus(`保存失败：${error.message}`, true);
    } finally {
        saveStoryboardBtn.disabled = false;
    }
}

function exportStoryboard() {
    if (!activeChapter) {
        return;
    }
    const chapterNum = getChapterNumber(activeChapter);
    window.location.href = `/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/export`;
}

function renderStoryboard(storyboard) {
    const scenes = Array.isArray(storyboard && storyboard.scenes) ? storyboard.scenes : [];
    storyboardList.innerHTML = '';
    storyboardEmpty.hidden = scenes.length > 0;
    storyboardEmpty.textContent = scenes.length ? '' : '本章还没有有效的分镜脚本。';
    storyboardSummary.textContent = scenes.length
        ? `${scenes.length} 个分镜 · ${storyboard.totalDurationSec || scenes.reduce((sum, scene) => sum + (scene.durationSec || 0), 0)} 秒`
        : '尚未生成';
    saveStoryboardBtn.hidden = scenes.length === 0;
    goVideoGenerationBtn.hidden = scenes.length === 0;
    exportStoryboardBtn.hidden = scenes.length === 0;

    scenes.forEach((scene, index) => {
        const card = document.createElement('details');
        card.className = 'storyboard-card';
        card.open = index === 0;
        card.dataset.sceneId = String(scene.id || '');
        card.innerHTML = `
            <summary class="storyboard-card-head">
                <strong>分镜 ${escapeHtml(scene.sequence || '')}</strong>
                <span>${escapeHtml(scene.durationSec || 0)} 秒 · ${escapeHtml(scene.shotType || '多镜头')}</span>
                <span class="storyboard-card-toggle" aria-hidden="true">收起</span>
            </summary>
            <div class="storyboard-card-body">
                <p class="storyboard-meta">${escapeHtml(scene.location || '未设置场景')} · ${escapeHtml(scene.timeOfDay || '未设置时间')} · ${escapeHtml(scene.cameraMovement || '未设置镜头运动')}</p>
                <div class="asset-chip-list">${renderAssetChips(scene)}</div>
                <label class="storyboard-shot-plan-label">镜头脚本<textarea data-field="shotPlan" rows="12">${escapeHtml(scene.shotPlan || '')}</textarea></label>
            </div>
        `;
        storyboardList.appendChild(card);
    });
}

function refreshStoryboardTimeline() {
    const cards = Array.from(storyboardList.querySelectorAll('.storyboard-card'));
    cards.forEach(card => {
        const plan = card.querySelector('[data-field="shotPlan"]');
        if (plan) {
            plan.classList.toggle('invalid', Boolean(plan.value.trim()) && !isValidShotPlan(plan.value));
        }
    });
}

function isValidShotPlan(plan) {
    const total = plan.match(/^\s*时长\s+(\d+):(\d{2})\s*-\s*(\d+):(\d{2})\s*$/m);
    if (!total) return false;
    const totalStart = Number(total[1]) * 60 + Number(total[2]);
    const totalEnd = Number(total[3]) * 60 + Number(total[4]);
    if (totalStart !== 0 || totalEnd - totalStart < 3 || totalEnd - totalStart > 10) return false;
    const shots = [...plan.matchAll(/\[(\d+):(\d{2})\s*-\s*(\d+):(\d{2})\s*\|[^\]]+\]/g)];
    if (shots.length < 1 || shots.length > 6) return false;
    let expected = 0;
    for (const shot of shots) {
        const start = Number(shot[1]) * 60 + Number(shot[2]);
        const end = Number(shot[3]) * 60 + Number(shot[4]);
        if (start !== expected || end <= start) return false;
        expected = end;
    }
    return expected === totalEnd;
}

function goToVideoGeneration() {
    if (!activeChapter || !activeStoryboard || !activeStoryboard.scenes || !activeStoryboard.scenes.length) {
        setStoryboardStatus('当前章节还没有可用的分镜脚本。', true);
        return;
    }
    if (hasUnsavedStoryboardChanges()) {
        videoNavigationModal.hidden = false;
        return;
    }
    goToVideoGenerationPage();
}

function goToVideoGenerationPage() {
    closeVideoNavigationModal();
    const chapterNum = getChapterNumber(activeChapter);
    rememberVideoContext(novelId, chapterNum);
    window.location.href = `/video-generation.html?novelId=${encodeURIComponent(novelId)}&chapterNum=${encodeURIComponent(chapterNum)}`;
}

function rememberVideoContext(currentNovelId, chapterNum) {
    try {
        localStorage.setItem('mirror:videoContext', JSON.stringify({
            novelId: String(currentNovelId),
            chapterNum: String(chapterNum)
        }));
    } catch (_) { }
}

function closeVideoNavigationModal() {
    videoNavigationModal.hidden = true;
}

function hasUnsavedStoryboardChanges() {
    if (!activeStoryboard || !Array.isArray(activeStoryboard.scenes)) {
        return false;
    }
    return activeStoryboard.scenes.some(scene => {
        const card = storyboardList.querySelector(`[data-scene-id="${CSS.escape(String(scene.id))}"]`);
        if (!card) {
            return false;
        }
        return ['shotPlan'].some(field => {
            const input = card.querySelector(`[data-field="${field}"]`);
            return input && String(input.value || '') !== String(scene[field] || '');
        });
    });
}

function renderAssetChips(scene) {
    const byId = new Map(chapterAssets.map(asset => [String(asset.assetId), asset]));
    const ids = [
        ...(scene.characterAssetIds || []).map(id => ({id, role: '人物'})),
        ...(scene.locationAssetIds || []).map(id => ({id, role: '场景'}))
    ];
    if (!ids.length) {
        return '<span class="asset-chip muted">未关联资产</span>';
    }
    return ids.map(item => {
        const asset = byId.get(String(item.id));
        return `<span class="asset-chip">${item.role}：${escapeHtml(asset ? `${asset.assetName} v${asset.version || 1}` : `资产 ${item.id}`)}</span>`;
    }).join('');
}

function setStoryboardBusy(busy, message) {
    generateStoryboardBtn.disabled = busy;
    generateStoryboardBtn.textContent = busy ? '正在生成分镜...' : '生成分镜脚本';
    if (message) {
        setStoryboardStatus(message, false);
    }
}

function setStoryboardStatus(message, error) {
    storyboardStatus.textContent = message || '';
    storyboardStatus.classList.toggle('error', Boolean(error));
}

function clearStoryboardStatus() {
    setStoryboardStatus('', false);
}

function getChapterNumber(chapter) {
    return chapter && (chapter.chapterNum || chapter.chapterNumber || chapter.id);
}

function escapeHtml(value) {
    return String(value)
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

async function saveActiveChapter() {
    if (!activeChapter) {
        return;
    }

    const chapterId = getChapterId(activeChapter);
    if (!chapterId) {
        alert('缺少章节 ID，无法保存。');
        return;
    }

    const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterId)}`, {
        method: 'PUT',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            content: chapterContent.value,
            chapterText: chapterContent.value
        })
    });

    alert(response.ok ? '保存成功。' : '保存失败。');
}

async function regenerateActiveChapter() {
    if (!activeChapter) {
        return;
    }

    const chapterId = getChapterId(activeChapter);
    if (!chapterId) {
        alert('缺少章节 ID，无法重新生成。');
        return;
    }

    const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterId)}/regenerate`, {
        method: 'POST'
    });

    if (!response.ok) {
        alert('重新生成失败。');
        return;
    }

    const detail = unwrapResponse(await response.json()) || {};
    activeChapter = detail;
    chapterTitle.textContent = detail.chapterTitle || activeChapter.chapterTitle || '未命名章节';
    chapterContent.value = detail.content || detail.chapterText || '';
    alert('本章已重新生成。');
}

function normalizeChapterList(data) {
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
    if (Array.isArray(data.chapters)) {
        return data.chapters;
    }
    if (Array.isArray(data.chapterList)) {
        return data.chapterList;
    }
    return [];
}

function unwrapResponse(data) {
    if (!data || typeof data !== 'object') {
        return data;
    }
    if ('data' in data) {
        return data.data;
    }
    return data;
}

function getChapterId(chapter) {
    return chapter.chapterId || chapter.id || chapter.chapterNum || '';
}
