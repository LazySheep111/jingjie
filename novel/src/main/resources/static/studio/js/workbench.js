(function () {
    const params = new URLSearchParams(window.location.search);
    const novelId = params.get('novelId');
    const chapterNum = params.get('chapterNum');
    const sceneTrack = document.getElementById('sceneTrack');
    const sceneEditor = document.getElementById('sceneEditor');
    const assetInspector = document.getElementById('assetInspector');
    const title = document.getElementById('workbenchTitle');
    let scenes = [];
    let assets = [];
    let selectedSceneId = null;
    let firstFrameId = null;
    const generateVideoButton = document.querySelector('[data-action="generate-video"]');

    function escapeHtml(value) {
        return String(value == null ? '' : value).replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]));
    }

    function renderSceneTrack(nextScenes) {
        sceneTrack.innerHTML = nextScenes.length ? nextScenes.map(scene => `
            <button class="scene-chip${String(scene.id) === String(selectedSceneId) ? ' is-selected' : ''}" type="button" data-scene-id="${escapeHtml(scene.id)}">
                <strong>场景 ${escapeHtml(scene.sequence || '')}</strong><span>${escapeHtml(scene.durationSec || 0)} 秒 · ${escapeHtml(scene.shotType || '多镜头')}</span>
            </button>`).join('') : '<p class="empty-copy">本章还没有分镜。请先在旧编辑页生成分镜。</p>';
        sceneTrack.querySelectorAll('[data-scene-id]').forEach(button => button.addEventListener('click', () => {
            selectedSceneId = button.dataset.sceneId;
            renderSceneTrack(scenes);
            renderSelectedScene(scenes.find(scene => String(scene.id) === selectedSceneId));
        }));
    }

    function renderSelectedScene(scene) {
        if (!scene) { sceneEditor.innerHTML = '<p class="empty-copy">选择一个场景以开始编辑。</p>'; return; }
        const related = assets.filter(asset => (scene.characterAssetIds || []).concat(scene.locationAssetIds || []).map(String).includes(String(asset.assetId)));
        sceneEditor.innerHTML = `<div class="scene-title"><div><small>场景 ${escapeHtml(scene.sequence || '')}</small><h2>${escapeHtml(scene.location || '未设置场景')}</h2></div><span>${escapeHtml(scene.timeOfDay || '未设置时间')}</span></div><pre>${escapeHtml(scene.shotPlan || '暂无分镜脚本')}</pre><div class="frame-actions"><button type="button" data-action="generate-first-frame">生成首帧</button><button type="button" data-action="upload-first-frame">上传首帧</button><input type="file" data-role="first-frame-upload" accept="image/png,image/jpeg,image/webp" hidden><span data-role="frame-status"></span></div>`;
        assetInspector.innerHTML = related.length ? related.map(asset => `<article class="asset-row"><strong>${escapeHtml(asset.assetName)}</strong><span>${escapeHtml(asset.assetType || '资产')}</span></article>`).join('') : '<p class="empty-copy">该场景未关联资产。</p>';
        sceneEditor.querySelector('[data-action="generate-first-frame"]').addEventListener('click', () => generateFirstFrame(scene));
        sceneEditor.querySelector('[data-action="upload-first-frame"]').addEventListener('click', () => sceneEditor.querySelector('[data-role="first-frame-upload"]').click());
        sceneEditor.querySelector('[data-role="first-frame-upload"]').addEventListener('change', event => uploadFirstFrame(scene, event.target.files[0]));
        loadFirstFrames(scene);
    }

    async function loadFirstFrames(scene) {
        try {
            const frames = await studioApi.request(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/${encodeURIComponent(scene.id)}/first-frames`);
            const list = Array.isArray(frames) ? frames : [];
            const holder = document.createElement('div');
            holder.className = 'first-frame-list';
            holder.innerHTML = list.map(frame => `<button type="button" data-first-frame-id="${escapeHtml(frame.id || frame.firstFrameId)}">选择首帧</button>`).join('');
            sceneEditor.appendChild(holder);
            holder.querySelectorAll('[data-first-frame-id]').forEach(button => button.addEventListener('click', () => { firstFrameId = button.dataset.firstFrameId; generateVideoButton.disabled = false; document.getElementById('videoStatus').textContent = '已选择首帧，可以生成视频。'; }));
        } catch (_) { }
    }

    async function generateFirstFrame(scene) {
        const status = sceneEditor.querySelector('[data-role="frame-status"]');
        status.textContent = '正在创建首帧任务…';
        try {
            const task = await studioApi.request(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/${encodeURIComponent(scene.id)}/first-frame-tasks`, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({sceneId:scene.id})});
            await studioApi.poll(`/api/first-frame-tasks/${encodeURIComponent(task.taskId)}`, value => ['SUCCESS','FAILED'].includes(value.status), value => { status.textContent = value.status === 'SUCCESS' ? '首帧已生成，请刷新选择。' : `生成失败：${value.errorMsg || '请重试'}`; });
        } catch (error) { status.textContent = `生成失败：${error.message}`; }
    }

    async function uploadFirstFrame(scene, file) {
        if (!file) return;
        const status = sceneEditor.querySelector('[data-role="frame-status"]');
        const form = new FormData(); form.append('file', file);
        status.textContent = '正在上传首帧…';
        try { await studioApi.request(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/${encodeURIComponent(scene.id)}/first-frames/upload`, {method:'POST', body:form}); status.textContent = '首帧已上传，请刷新选择。'; }
        catch (error) { status.textContent = `上传失败：${error.message}`; }
    }

    async function generateVideo() {
        const scene = scenes.find(item => String(item.id) === selectedSceneId);
        if (!scene || !firstFrameId) return;
        const status = document.getElementById('videoStatus');
        generateVideoButton.disabled = true;
        status.textContent = '正在创建视频任务…';
        try {
            const task = await studioApi.request(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard/${encodeURIComponent(scene.id)}/video-tasks`, {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({sceneId:scene.id, firstFrameId, resolution:document.getElementById('videoResolution').value})});
            await studioApi.poll(`/api/video-tasks/${encodeURIComponent(task.taskId)}`, value => ['SUCCESS','FAILED'].includes(value.status), value => renderTaskState(value, scene));
        } catch (error) { renderTaskState({status:'FAILED', errorMsg:error.message}, scene); }
    }

    function renderTaskState(task, scene) {
        const status = document.getElementById('videoStatus');
        if (task.status === 'SUCCESS') { status.textContent = '视频已生成，可在任务列表查看版本。'; return; }
        if (task.status === 'FAILED') {
            status.innerHTML = `生成失败：${escapeHtml(task.errorMsg || '请重试')} <button type="button" data-action="retry-video">重试</button> <button type="button" data-action="safety-rewrite">安全改写后重试</button>`;
            status.querySelector('[data-action="retry-video"]').addEventListener('click', generateVideo);
            status.querySelector('[data-action="safety-rewrite"]').addEventListener('click', () => safetyRewrite(scene));
            generateVideoButton.disabled = false;
        } else status.textContent = `正在生成视频：${escapeHtml(task.status || '处理中')}`;
    }

    async function safetyRewrite(scene) {
        const status = document.getElementById('videoStatus');
        try { await studioApi.request('/api/video-prompts/safety-rewrite', {method:'POST', headers:{'Content-Type':'application/json'}, body:JSON.stringify({prompt:scene.shotPlan || ''})}); status.textContent = '已完成安全改写，请重新生成视频。'; }
        catch (error) { status.textContent = `安全改写失败：${error.message}`; }
    }
    generateVideoButton.addEventListener('click', generateVideo);

    async function loadWorkbench() {
        if (!novelId || !chapterNum) {
            const history = await studioApi.request('/api/novel/history?page=1&pageSize=10');
            const projects = Array.isArray(history) ? history : (history.records || []);
            sceneTrack.innerHTML = projects.length ? projects.map(project => `<a class="scene-chip" href="/novel-detail.html?novelId=${encodeURIComponent(project.novelId)}"><strong>${escapeHtml(project.novelTitle || '未命名作品')}</strong><span>打开作品并选择章节</span></a>`).join('') : '<p class="empty-copy">暂无项目。请使用旧页面导入小说。</p>';
            sceneEditor.innerHTML = '<p class="empty-copy">请选择一个项目以进入分镜编辑。</p>';
            return;
        }
        try {
            const [storyboard, chapterAssets] = await Promise.all([studioApi.getStoryboard(novelId, chapterNum), studioApi.getChapterAssets(novelId, chapterNum)]);
            scenes = Array.isArray(storyboard && storyboard.scenes) ? storyboard.scenes : [];
            assets = Array.isArray(chapterAssets) ? chapterAssets : [];
            title.textContent = `第 ${chapterNum} 章 · 分镜工作台`;
            selectedSceneId = scenes.length ? String(scenes[0].id) : null;
            renderSceneTrack(scenes);
            renderSelectedScene(scenes[0]);
        } catch (error) { sceneTrack.innerHTML = `<p class="empty-copy">加载失败：${escapeHtml(error.message)}。请返回后重试。</p>`; }
    }

    loadWorkbench();
}());
