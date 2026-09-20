const params = new URLSearchParams(window.location.search);
const novelId = params.get('novelId');
const chapterNum = params.get('chapterNum');
const pageSummary = document.getElementById('pageSummary');
const pageStatus = document.getElementById('pageStatus');
const sceneList = document.getElementById('sceneList');
const pageEmpty = document.getElementById('pageEmpty');
const backDetailBtn = document.getElementById('backDetailBtn');
const chapterPickerWrap = document.getElementById('chapterPickerWrap');
const chapterSelect = document.getElementById('chapterSelect');
const previewDialog = document.getElementById('firstFramePreviewDialog');
const previewImage = document.getElementById('firstFramePreviewImage');
const closePreviewButton = document.getElementById('closeFirstFramePreview');
const videoPollers = new Map();
const firstFramePollers = new Map();
const selectedFirstFrames = new Map();
const firstFrameClickTimers = new Map();

backDetailBtn.addEventListener('click', () => {
    window.location.href = `/novel-detail.html?novelId=${encodeURIComponent(novelId || '')}`;
});

chapterSelect?.addEventListener('change', () => {
    const selected = chapterSelect.value;
    if (!selected || selected === String(chapterNum)) return;
    const nextUrl = new URL(window.location.href);
    nextUrl.searchParams.set('novelId', novelId);
    nextUrl.searchParams.set('chapterNum', selected);
    window.location.href = nextUrl.toString();
});

closePreviewButton.addEventListener('click', closeFirstFramePreview);
previewDialog.addEventListener('click', event => {
    if (event.target === previewDialog) closeFirstFramePreview();
});

if (!novelId || !chapterNum) {
    setStatus('请先选择小说', true);
} else {
    loadPage();
}

async function loadPage() {
    loadChapterOptions();
    try {
        const [storyboardResponse, assetsResponse] = await Promise.all([
            fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard`),
            fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/assets`)
        ]);
        if (!storyboardResponse.ok) throw new Error(`分镜请求失败：${storyboardResponse.status}`);
        const storyboard = unwrap(await storyboardResponse.json());
        const assets = assetsResponse.ok ? (unwrap(await assetsResponse.json()) || []) : [];
        const assetMap = new Map(assets.map(asset => [String(asset.assetId), asset]));
        const scenes = Array.isArray(storyboard && storyboard.scenes) ? storyboard.scenes : [];
        pageSummary.textContent = `${scenes.length} 个分镜 · ${storyboard.totalDurationSec || 0} 秒`;
        renderScenes(scenes, assetMap);
    } catch (error) {
        setStatus(error.message, true);
        pageEmpty.textContent = '加载失败，请返回正文页重试。';
    }
}

async function loadChapterOptions() {
    if (!chapterSelect || !chapterPickerWrap || !novelId) return;
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters`);
        if (!response.ok) throw new Error(`章节列表请求失败：${response.status}`);
        const chapters = unwrap(await response.json()) || [];
        if (!Array.isArray(chapters) || !chapters.length) return;
        chapterSelect.replaceChildren(...chapters.map(chapter => {
            const option = document.createElement('option');
            option.value = String(chapter.chapterNum);
            option.textContent = `第 ${chapter.chapterNum} 章：${chapter.chapterTitle || '未命名章节'}`;
            return option;
        }));
        chapterSelect.value = String(chapterNum);
        chapterPickerWrap.hidden = false;
    } catch (error) {
        // Chapter navigation is additive; an unavailable chapter list must not hide the current chapter content.
        chapterPickerWrap.hidden = true;
    }
}

function renderScenes(scenes, assetMap) {
    sceneList.innerHTML = '';
    pageEmpty.hidden = scenes.length > 0;
    const quickNav = document.createElement('nav');
    quickNav.className = 'video-scene-quick-nav';
    quickNav.setAttribute('aria-label', '分镜快速切换');
    const stageHost = document.createElement('section');
    stageHost.className = 'video-stage-host';
    stageHost.id = 'video-stage-host';
    sceneList.appendChild(quickNav);
    sceneList.appendChild(stageHost);
    const sceneCards = [];
    scenes.forEach((scene, index) => {
        const card = document.createElement('details');
        card.className = 'video-scene-card video-production-stage';
        card.open = index === 0;
        card.dataset.sceneId = String(scene.id);
        card.id = `video-scene-${scene.id}`;
        card.innerHTML = `
            <summary class="storyboard-card-head video-scene-summary">
                <span class="video-scene-index"><small>分镜</small><strong>${escapeHtml(scene.sequence || '')}</strong></span>
                <span class="video-scene-summary-meta"><span>${escapeHtml(scene.durationSec || 0)} 秒</span><span>${escapeHtml(scene.shotType || '多镜头')}</span></span>
                <span class="storyboard-card-toggle" aria-hidden="true">收起</span>
            </summary>
            <div class="video-scene-body video-stage-layout">
                <aside class="video-stage-rail" aria-label="分镜摘要">
                    <span>SCENE</span>
                    <strong>${escapeHtml(String(scene.sequence || index + 1).padStart(2, '0'))}</strong>
                    <small>${escapeHtml(scene.durationSec || 0)} 秒</small>
                    <small>${escapeHtml(scene.shotType || '多镜头')}</small>
                </aside>
                <div class="video-stage-canvas">
                    <div class="video-primary-canvas">
                    <div class="video-scene-workspace">
                        <section class="video-script-panel">
                        <h3>分镜脚本</h3>
                        <p class="storyboard-meta">${escapeHtml(scene.location || '未设置场景')} · ${escapeHtml(scene.timeOfDay || '未设置时间')}</p>
                        <pre class="storyboard-shot-plan">${escapeHtml(scene.shotPlan || '暂无分镜脚本')}</pre>
                        </section>
                        <section class="video-assets-panel">
                            <h3>参考资产</h3>
                            <div class="video-reference-list">${renderReferences(scene, assetMap)}</div>
                        </section>
                    </div>
                    <section class="video-output-dock">
                        <div class="video-output-console">
                        <div class="first-frame-panel">
                            <div class="first-frame-panel-head">
                                <h3>分镜首帧</h3>
                                <span class="asset-meta">单击缩略图选择，双击查看大图</span>
                            </div>
                            <div class="first-frame-list" data-role="first-frame-list"></div>
                        </div>
                        <div class="video-result" data-role="result"></div>
                        <div class="video-history" data-role="history" hidden></div>
                        </div>
                    </section>
                    </div>
                </div>
                <section class="video-control-panel video-stage-operations" aria-label="视频生成控制">
                    <div class="video-control-heading">
                        <div><p>生成控制</p><span class="video-scene-status" data-role="status">尚未生成</span></div>
                        <label class="video-resolution-field">视频分辨率
                            <select data-field="resolution">
                                <option value="720P">720P</option>
                                <option value="768P" selected>768P</option>
                                <option value="1080P">1080P</option>
                                <option value="2K">2K</option>
                            </select>
                        </label>
                    </div>
                    <div class="video-action-groups">
                        <div class="video-frame-actions">
                            <span>01 / 准备首帧</span>
                            <button type="button" class="secondary-button" data-action="generate-first-frame">生成首帧</button>
                            <button type="button" class="secondary-button" data-action="upload-first-frame">上传首帧</button>
                            <input type="file" data-role="first-frame-upload" accept="image/png,image/jpeg,image/webp,.png,.jpg,.jpeg,.webp" hidden>
                        </div>
                        <div class="video-primary-actions">
                            <span>02 / 生成视频</span>
                            <button type="button" data-action="generate-video">生成视频</button>
                            <button type="button" class="secondary-button" data-action="upload-video">上传视频</button>
                            <input type="file" data-role="video-upload" accept="video/mp4,video/webm,video/quicktime,.mp4,.webm,.mov" hidden>
                            <button type="button" class="secondary-button" data-action="safety-rewrite" hidden>安全改写后重试</button>
                            <button type="button" class="secondary-button" data-action="history">查看历史版本</button>
                        </div>
                    </div>
                </section>
            </div>`;
        const generate = card.querySelector('[data-action="generate-video"]');
        const firstFrameButton = card.querySelector('[data-action="generate-first-frame"]');
        const uploadFirstFrameButton = card.querySelector('[data-action="upload-first-frame"]');
        const uploadVideoButton = card.querySelector('[data-action="upload-video"]');
        const firstFrameUpload = card.querySelector('[data-role="first-frame-upload"]');
        const videoUpload = card.querySelector('[data-role="video-upload"]');
        const rewrite = card.querySelector('[data-action="safety-rewrite"]');
        const history = card.querySelector('[data-action="history"]');
        generate.addEventListener('click', () => generateVideo(card, scene));
        firstFrameButton.addEventListener('click', () => generateFirstFrame(card, scene));
        uploadFirstFrameButton.addEventListener('click', () => firstFrameUpload.click());
        uploadVideoButton.addEventListener('click', () => videoUpload.click());
        firstFrameUpload.addEventListener('change', () => uploadFirstFrame(card, scene, firstFrameUpload));
        videoUpload.addEventListener('change', () => uploadVideo(card, scene, videoUpload));
        rewrite.addEventListener('click', () => safetyRewrite(card, scene));
        history.addEventListener('click', () => loadHistory(card, scene));
        card.querySelectorAll('[data-action="remove-reference-asset"]').forEach(button => {
            button.addEventListener('click', () => removeReferenceAsset(
                card,
                scene,
                button.dataset.assetId,
                button.dataset.assetRole
            ));
        });
        sceneCards.push({ card, scene });
        card.addEventListener('toggle', () => {
            if (card.open) setActiveSceneQuickNav(quickNav, String(scene.id));
        });
        loadFirstFrames(card, scene, true);
        loadHistory(card, scene, true);
    });
    renderSceneQuickNav(quickNav, stageHost, sceneCards);
    if (sceneCards.length) renderActiveScene(stageHost, quickNav, sceneCards, String(sceneCards[0].scene.id));
}

function renderSceneQuickNav(quickNav, stageHost, sceneCards) {
    quickNav.innerHTML = sceneCards.map(({ scene }, index) => {
        const sceneNumber = scene.sequence || index + 1;
        const shotType = scene.shotType || '多镜头';
        return `
            <button type="button" class="${index === 0 ? 'active' : ''}" data-scene-id="${escapeHtml(scene.id)}" aria-current="${index === 0 ? 'step' : 'false'}">
                <span>
                    <small>分镜 ${escapeHtml(sceneNumber)}</small>
                    <strong>${escapeHtml(shotType)}</strong>
                </span>
                <em>${escapeHtml(scene.durationSec || 0)} 秒</em>
            </button>
        `;
    }).join('');

    quickNav.querySelectorAll('button[data-scene-id]').forEach((button) => {
        button.addEventListener('click', () => {
            renderActiveScene(stageHost, quickNav, sceneCards, String(button.dataset.sceneId));
        });
    });
}

function renderActiveScene(stageHost, quickNav, sceneCards, activeSceneId) {
    const entry = sceneCards.find(({ scene }) => String(scene.id) === String(activeSceneId));
    if (!entry) return;
    const scrollTop = window.scrollY;
    entry.card.open = true;
    stageHost.replaceChildren(entry.card);
    setActiveSceneQuickNav(quickNav, String(entry.scene.id));
    window.setTimeout(() => window.scrollTo(0, scrollTop), 0);
}

function setActiveSceneQuickNav(quickNav, activeSceneId) {
    quickNav.querySelectorAll('button[data-scene-id]').forEach((button) => {
        const active = button.dataset.sceneId === String(activeSceneId);
        button.classList.toggle('active', active);
        button.setAttribute('aria-current', active ? 'step' : 'false');
    });
}

function renderReferences(scene, assetMap) {
    const refs = [
        ...(scene.characterAssetIds || []).map(id => ({id, label: '人物', assetRole: 'CHARACTER'})),
        ...(scene.locationAssetIds || []).map(id => ({id, label: '场景', assetRole: 'LOCATION'}))
    ];
    if (!refs.length) return '<p class="asset-meta">未关联人物或场景资产</p>';
    return refs.map(ref => {
        const asset = assetMap.get(String(ref.id));
        const image = asset && asset.compositeImagePath
            ? `<img src="${escapeHtml(asset.compositeImagePath)}" alt="${escapeHtml(asset.assetName || '参考三视图')}">`
            : '<div class="video-reference-placeholder">缺少三视图</div>';
        return `<div class="video-reference-card">
            <div class="video-reference-media">
                ${image}
                <button type="button" class="remove-reference-asset" data-action="remove-reference-asset" data-asset-id="${escapeHtml(ref.id)}" data-asset-role="${escapeHtml(ref.assetRole)}" aria-label="移除${escapeHtml(ref.label)}参考资产" title="移除当前分镜引用">×</button>
            </div>
            <strong>${ref.label}：${escapeHtml(asset ? asset.assetName : `资产 ${ref.id}`)}</strong>
        </div>`;
    }).join('');
}

async function removeReferenceAsset(card, scene, assetId, assetRole) {
    const confirmed = window.confirm('仅移除该资产在当前分镜中的引用，不会删除资产库内容或影响已生成视频。');
    if (!confirmed) return;
    const status = card.querySelector('[data-role="status"]');
    status.textContent = '正在移除参考资产...';
    try {
        const response = await fetch(
            `/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}`
            + `/storyboard/${encodeURIComponent(scene.id)}/assets/${encodeURIComponent(assetId)}`
            + `?assetRole=${encodeURIComponent(assetRole)}`,
            {method: 'DELETE'}
        );
        if (!response.ok) throw new Error(`移除参考资产失败：${response.status}`);
        await loadPage();
    } catch (error) {
        status.textContent = error.message;
    }
}

async function generateVideo(card, scene, promptOverride = null) {
    const button = card.querySelector('[data-action="generate-video"]');
    const status = card.querySelector('[data-role="status"]');
    const rewrite = card.querySelector('[data-action="safety-rewrite"]');
    const firstFrameId = selectedFirstFrames.get(String(scene.id));
    const resolution = card.querySelector('[data-field="resolution"]').value;
    const failedTaskId = card.dataset.failedTaskId;
    if (!firstFrameId && !failedTaskId) {
        status.textContent = '请先选择首帧';
        return;
    }
    button.disabled = true;
    rewrite.hidden = true;
    status.textContent = '正在排队...';
    try {
        const url = failedTaskId
            ? `/api/video-tasks/${encodeURIComponent(failedTaskId)}/retry`
            : `/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/${encodeURIComponent(scene.id)}/video-tasks`;
        const response = await fetch(url, {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({sceneId: scene.id, firstFrameId, promptOverride, resolution})
        });
        const data = await response.json();
        if (!response.ok || data.success === false) throw new Error(data.errorMsg || `请求失败：${response.status}`);
        const task = unwrap(data);
        delete card.dataset.failedTaskId;
        pollTask(card, scene, task.taskId);
    } catch (error) {
        button.disabled = false;
        rewrite.disabled = false;
        rewrite.hidden = !isSafetyReviewError(error.message);
        status.textContent = `生成失败：${error.message}`;
        setStatus(error.message, true);
    }
}

async function generateFirstFrame(card, scene) {
    const button = card.querySelector('[data-action="generate-first-frame"]');
    const status = card.querySelector('[data-role="status"]');
    button.disabled = true;
    status.textContent = '正在生成分镜首帧...';
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/${encodeURIComponent(scene.id)}/first-frame-tasks`, {
            method: 'POST'
        });
        const data = await response.json();
        if (!response.ok || data.success === false) throw new Error(data.errorMsg || `请求失败：${response.status}`);
        const task = unwrap(data);
        pollFirstFrameTask(card, scene, task.id);
    } catch (error) {
        button.disabled = false;
        status.textContent = `首帧生成失败：${error.message}`;
        setStatus(error.message, true);
    }
}

async function uploadFirstFrame(card, scene, input) {
    const file = input.files && input.files[0];
    if (!file) return;
    const button = card.querySelector('[data-action="upload-first-frame"]');
    const status = card.querySelector('[data-role="status"]');
    button.disabled = true;
    status.textContent = '正在上传首帧...';
    try {
        const formData = new FormData();
        formData.append('file', file);
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/${encodeURIComponent(scene.id)}/first-frames/upload`, {
            method: 'POST',
            body: formData
        });
        const data = await response.json();
        if (!response.ok || data.success === false) throw new Error(data.errorMsg || `上传失败：${response.status}`);
        const frame = unwrap(data);
        selectedFirstFrames.set(String(scene.id), String(frame.id));
        await loadFirstFrames(card, scene);
        status.textContent = '首帧上传完成，已选中，可生成视频';
    } catch (error) {
        status.textContent = `首帧上传失败：${error.message}`;
        setStatus(error.message, true);
    } finally {
        button.disabled = false;
        input.value = '';
    }
}

function pollFirstFrameTask(card, scene, taskId) {
    stopFirstFramePolling(scene.id);
    const check = async () => {
        try {
            const response = await fetch(`/api/first-frame-tasks/${encodeURIComponent(taskId)}`);
            const data = await response.json();
            if (!response.ok || data.success === false) throw new Error(data.errorMsg || `状态请求失败：${response.status}`);
            const task = unwrap(data);
            const status = card.querySelector('[data-role="status"]');
            const button = card.querySelector('[data-action="generate-first-frame"]');
            status.textContent = firstFrameTaskStatusLabel(task.status);
            if (task.status === 'SUCCESS') {
                stopFirstFramePolling(scene.id);
                button.disabled = false;
                status.textContent = '首帧生成完成，请点击缩略图选择';
                await loadFirstFrames(card, scene);
                return;
            }
            if (task.status === 'FAILED') {
                stopFirstFramePolling(scene.id);
                button.disabled = false;
                status.textContent = `首帧生成失败：${task.errorMessage || '图片模型处理失败'}`;
                return;
            }
            firstFramePollers.set(scene.id, window.setTimeout(check, 2000));
        } catch (error) {
            stopFirstFramePolling(scene.id);
            card.querySelector('[data-action="generate-first-frame"]').disabled = false;
            card.querySelector('[data-role="status"]').textContent = `首帧状态读取失败：${error.message}`;
        }
    };
    check();
}

async function loadFirstFrames(card, scene, quiet = false) {
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/${encodeURIComponent(scene.id)}/first-frames`);
        if (!response.ok) throw new Error(`首帧列表请求失败：${response.status}`);
        renderFirstFrames(card, scene, unwrap(await response.json()) || []);
    } catch (error) {
        if (!quiet) setStatus(error.message, true);
    }
}

function renderFirstFrames(card, scene, frames) {
    const list = card.querySelector('[data-role="first-frame-list"]');
    const sceneKey = String(scene.id);
    const selectedId = selectedFirstFrames.get(sceneKey);
    if (selectedId && !frames.some(frame => String(frame.id) === String(selectedId))) {
        selectedFirstFrames.delete(sceneKey);
    }
    if (!frames.length) {
        list.innerHTML = '<p class="asset-meta">暂无首帧。请先生成一张首帧，再选择它生成视频。</p>';
        return;
    }
    list.innerHTML = frames.map(frame => `
        <article class="first-frame-candidate${String(frame.id) === String(selectedFirstFrames.get(sceneKey)) ? ' selected' : ''}" data-first-frame-id="${escapeHtml(frame.id)}">
            <button type="button" class="first-frame-select" data-action="select-first-frame" data-first-frame-id="${escapeHtml(frame.id)}" aria-label="选择首帧 v${escapeHtml(frame.version)}">
                <img src="${escapeHtml(frame.imagePath)}" alt="分镜首帧 v${escapeHtml(frame.version)}">
            </button>
            <div class="first-frame-candidate-foot"><span>v${escapeHtml(frame.version)} · <span class="media-source-badge media-source-${escapeHtml(String(frame.source || 'AI').toLowerCase())}">${mediaSourceLabel(frame.source)}</span></span><button type="button" class="text-button" data-action="delete-first-frame" data-first-frame-id="${escapeHtml(frame.id)}">删除</button></div>
        </article>`).join('');
    list.querySelectorAll('[data-action="select-first-frame"]').forEach(button => {
        button.addEventListener('click', () => {
            const frameId = String(button.dataset.firstFrameId);
            const clickKey = `${sceneKey}:${frameId}`;
            window.clearTimeout(firstFrameClickTimers.get(clickKey));
            firstFrameClickTimers.set(clickKey, window.setTimeout(() => {
                firstFrameClickTimers.delete(clickKey);
                selectedFirstFrames.set(sceneKey, frameId);
                renderFirstFrames(card, scene, frames);
                card.querySelector('[data-role="status"]').textContent = '已选择首帧，可生成视频';
            }, 300));
        });
        button.addEventListener('dblclick', event => {
            event.preventDefault();
            const frameId = String(button.dataset.firstFrameId);
            const clickKey = `${sceneKey}:${frameId}`;
            window.clearTimeout(firstFrameClickTimers.get(clickKey));
            firstFrameClickTimers.delete(clickKey);
            const frame = frames.find(item => String(item.id) === frameId);
            if (frame) openFirstFramePreview(frame);
        });
    });
    list.querySelectorAll('[data-action="delete-first-frame"]').forEach(button => {
        button.addEventListener('click', event => {
            event.stopPropagation();
            deleteFirstFrame(card, scene, button.dataset.firstFrameId);
        });
    });
}

function openFirstFramePreview(frame) {
    previewImage.src = frame.imagePath;
    previewImage.alt = `分镜首帧 v${frame.version} 大图预览`;
    if (!previewDialog.open) previewDialog.showModal();
}

function closeFirstFramePreview() {
    if (previewDialog.open) previewDialog.close();
}

async function deleteFirstFrame(card, scene, firstFrameId) {
    try {
        const response = await fetch(`/api/storyboard-first-frames/${encodeURIComponent(firstFrameId)}`, {method: 'DELETE'});
        if (!response.ok) throw new Error(`删除首帧失败：${response.status}`);
        if (String(selectedFirstFrames.get(String(scene.id))) === String(firstFrameId)) {
            selectedFirstFrames.delete(String(scene.id));
        }
        await loadFirstFrames(card, scene);
    } catch (error) {
        setStatus(error.message, true);
    }
}

async function uploadVideo(card, scene, input) {
    const file = input.files && input.files[0];
    if (!file) return;
    const button = card.querySelector('[data-action="upload-video"]');
    const status = card.querySelector('[data-role="status"]');
    button.disabled = true;
    status.textContent = '正在上传视频...';
    try {
        const formData = new FormData();
        formData.append('file', file);
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/${encodeURIComponent(scene.id)}/videos/upload`, {
            method: 'POST',
            body: formData
        });
        const data = await response.json();
        if (!response.ok || data.success === false) throw new Error(data.errorMsg || `上传失败：${response.status}`);
        delete card.dataset.failedTaskId;
        card.querySelector('[data-action="generate-video"]').textContent = '生成视频';
        await loadHistory(card, scene);
        status.textContent = '视频上传完成，已设为当前成片';
    } catch (error) {
        status.textContent = `视频上传失败：${error.message}`;
        setStatus(error.message, true);
    } finally {
        button.disabled = false;
        input.value = '';
    }
}

function pollTask(card, scene, taskId) {
    stopVideoPolling(scene.id);
    const check = async () => {
        try {
            const response = await fetch(`/api/video-tasks/${encodeURIComponent(taskId)}`);
            const task = unwrap(await response.json());
            const status = card.querySelector('[data-role="status"]');
            const button = card.querySelector('[data-action="generate-video"]');
            const rewrite = card.querySelector('[data-action="safety-rewrite"]');
            status.textContent = taskStatusLabel(task.status);
            if (task.status === 'SUCCESS') {
                stopVideoPolling(scene.id);
                delete card.dataset.failedTaskId;
                button.disabled = false;
                renderVideo(card, task.videoUrl, task.videoId);
                loadHistory(card, scene, true);
                return;
            }
            if (task.status === 'FAILED') {
                stopVideoPolling(scene.id);
                card.dataset.failedTaskId = String(taskId);
                button.disabled = false;
                button.textContent = '重试生成视频';
                status.textContent = `生成失败：${task.errorMessage || '视频模型处理失败'}`;
                rewrite.hidden = !isSafetyReviewError(task.errorMessage);
                rewrite.disabled = false;
                return;
            }
            videoPollers.set(scene.id, window.setTimeout(check, 2000));
        } catch (error) {
            stopVideoPolling(scene.id);
            card.querySelector('[data-action="generate-video"]').disabled = false;
            card.querySelector('[data-role="status"]').textContent = `状态读取失败：${error.message}`;
        }
    };
    check();
}

async function safetyRewrite(card, scene) {
    const rewrite = card.querySelector('[data-action="safety-rewrite"]');
    const generate = card.querySelector('[data-action="generate-video"]');
    const status = card.querySelector('[data-role="status"]');
    rewrite.disabled = true;
    generate.disabled = true;
    status.textContent = '正在进行安全改写...';
    try {
        const response = await fetch('/api/video-prompts/safety-rewrite', {
            method: 'POST',
            headers: {'Content-Type': 'application/json'},
            body: JSON.stringify({prompt: buildScenePrompt(scene)})
        });
        const data = await response.json();
        if (!response.ok || data.success === false) throw new Error(data.errorMsg || `安全改写失败：${response.status}`);
        const rewrittenPrompt = unwrap(data);
        if (!rewrittenPrompt) throw new Error('AI 未返回改写后的提示词');
        status.textContent = '安全改写完成，正在重新提交...';
        await generateVideo(card, scene, rewrittenPrompt);
    } catch (error) {
        rewrite.disabled = false;
        generate.disabled = false;
        status.textContent = `安全改写失败：${error.message}`;
        setStatus(error.message, true);
    }
}

function buildScenePrompt(scene) {
    return `镜头脚本：\n${scene.shotPlan || ''}`;
}

function isSafetyReviewError(message) {
    const value = String(message || '');
    return value.includes('InputTextSensitiveContentDetected') || value.includes('敏感') || value.includes('内容安全');
}

async function loadHistory(card, scene, quiet = false) {
    try {
        const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/${encodeURIComponent(scene.id)}/videos`);
        if (!response.ok) throw new Error(`历史版本请求失败：${response.status}`);
        const videos = unwrap(await response.json()) || [];
        const current = videos.find(video => video.current) || videos[0];
        if (current) {
            renderVideo(card, current.videoPath, current.id);
        } else {
            renderVideo(card, null, null);
        }
        const history = card.querySelector('[data-role="history"]');
        history.innerHTML = videos.length ? videos.map(video => `
            <div class="video-history-row"><span>v${escapeHtml(video.version)} · ${escapeHtml(video.resolution || '未知分辨率')} · ${escapeHtml(video.durationSec || 0)} 秒 · <span class="media-source-badge media-source-${escapeHtml(String(video.source || 'AI').toLowerCase())}">${mediaSourceLabel(video.source)}</span>${video.current ? ' · 当前' : ''}</span>
            <span class="video-history-actions"><button type="button" class="secondary-button" data-action="use-video" data-video-id="${video.id}">使用此版本</button><button type="button" class="text-button" data-action="delete-video" data-video-id="${video.id}">删除</button></span></div>`).join('') : '<p class="asset-meta">暂无历史视频</p>';
        history.querySelectorAll('[data-action="use-video"]').forEach(button => button.addEventListener('click', () => useVersion(button.dataset.videoId, card, scene)));
        history.querySelectorAll('[data-action="delete-video"]').forEach(button => button.addEventListener('click', () => deleteVideo(button.dataset.videoId, card, scene)));
    } catch (error) {
        if (!quiet) setStatus(error.message, true);
    }
}

function renderVideo(card, url, videoId) {
    card.querySelector('[data-role="result"]').innerHTML = url
        ? `<video class="generated-video" controls preload="metadata" src="${escapeHtml(url)}"></video>`
        : '<p class="asset-meta">暂无成片</p>';
}

function taskStatusLabel(status) {
    const labels = {
        QUEUED: '正在排队...',
        GENERATING_VIDEO: '正在生成视频...',
        RUNNING: '正在生成视频...',
        SUCCESS: '生成完成'
    };
    return labels[status] || `${status || '处理中'}...`;
}

function firstFrameTaskStatusLabel(status) {
    const labels = {
        QUEUED: '首帧正在排队...',
        GENERATING: '正在生成分镜首帧...',
        SUCCESS: '首帧生成完成',
        FAILED: '首帧生成失败'
    };
    return labels[status] || `${status || '首帧处理中'}...`;
}

async function useVersion(videoId, card, scene) {
    const response = await fetch(`/api/storyboard-videos/${encodeURIComponent(videoId)}/current`, {method: 'PUT'});
    if (!response.ok) {
        setStatus('切换视频版本失败。', true);
        return;
    }
    await loadHistory(card, scene);
}

async function deleteVideo(videoId, card, scene) {
    try {
        const response = await fetch(`/api/storyboard-videos/${encodeURIComponent(videoId)}`, {method: 'DELETE'});
        const data = await response.json();
        if (!response.ok || data.success === false) throw new Error(data.errorMsg || `删除视频失败：${response.status}`);
        await loadHistory(card, scene);
        card.querySelector('[data-role="status"]').textContent = '视频版本已删除';
    } catch (error) {
        setStatus(error.message, true);
    }
}

function mediaSourceLabel(source) {
    return source === 'UPLOAD' ? '手动上传' : 'AI 生成';
}

function stopVideoPolling(sceneId) {
    const timer = videoPollers.get(sceneId);
    if (timer) window.clearTimeout(timer);
    videoPollers.delete(sceneId);
}

function stopFirstFramePolling(sceneId) {
    const timer = firstFramePollers.get(sceneId);
    if (timer) window.clearTimeout(timer);
    firstFramePollers.delete(sceneId);
}

function setStatus(message, error) {
    pageStatus.textContent = message || '';
    pageStatus.classList.toggle('error', Boolean(error));
}

function unwrap(data) {
    return data && Object.prototype.hasOwnProperty.call(data, 'data') ? data.data : data;
}

function escapeHtml(value) {
    return String(value == null ? '' : value).replaceAll('&', '&amp;').replaceAll('<', '&lt;').replaceAll('>', '&gt;').replaceAll('"', '&quot;').replaceAll("'", '&#039;');
}
