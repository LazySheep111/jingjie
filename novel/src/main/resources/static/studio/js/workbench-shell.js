(function () {
    const body = document.body;
    if (!body || !body.dataset.shellPage) return;
    body.classList.add('workbench-page');

    const page = body.dataset.shellPage || '';
    const params = new URLSearchParams(window.location.search);
    const selectedNovelId = params.get('novelId') || readSelectedNovelId();
    if (params.get('novelId')) rememberSelectedNovelId(params.get('novelId'));
    const activeVideoContext = readVideoContext(params) || readStoredVideoContext();
    const contextNovelId = params.get('novelId') || selectedNovelId;
    const videoContextQuery = buildVideoContextQuery(activeVideoContext, selectedNovelId);
    const legacyNav = body.querySelector('.top-nav');
    if (legacyNav) legacyNav.remove();
    const navItems = [
        ['/outline-new.html', '小说大纲工作台', 'outline'],
        ['/novel-detail-new.html' + (selectedNovelId ? `?novelId=${encodeURIComponent(selectedNovelId)}` : ''), '小说全文工作台', 'detail'],
        ['/history-list-new.html', '我的历史作品', 'history'],
        ['/video-generation-new.html' + videoContextQuery, '视频创作工作台', 'video'],
        ['/asset-library-new.html', '资产库', 'assets'],
        ['/model-management-new.html', '模型管理', 'model']
    ];

    const currentMain = body.querySelector('main');
    if (!currentMain || body.querySelector('.workbench-shell')) return;

    const shell = document.createElement('div');
    shell.className = 'workbench-shell';
    const nav = document.createElement('aside');
    nav.className = 'workbench-nav';
    nav.setAttribute('aria-label', '工作区导航');
    nav.innerHTML = `<a class="workbench-brand" href="/outline-new.html"><strong>镜界</strong><small>AI 视频创作工作台</small></a><nav class="workbench-nav-links">${navItems.map(([href, label, key]) => `<a class="${key === 'model' ? 'workbench-nav-model-link ' : ''}${page === key ? 'active' : ''}" href="${href}">${label}</a>`).join('')}</nav><p class="workbench-nav-note">从故事到镜头，再到可生成的视频输出。</p>`;

    const mainWrap = document.createElement('div');
    mainWrap.className = 'workbench-main';
    const context = document.createElement('header');
    context.className = 'workbench-context';
    const heading = currentMain.querySelector('h1');
    const title = heading ? heading.textContent.trim() : '创作工作台';
    context.innerHTML = `<div><p class="eyebrow">当前工作区</p><h1>${escapeHtml(title)}</h1><p>${contextNovelId ? `作品 ${escapeHtml(contextNovelId)}${params.get('chapterNum') ? ` · 第 ${escapeHtml(params.get('chapterNum'))} 章` : ''}` : '选择一个作品开始创作'}</p></div>`;
    currentMain.classList.add('workbench-content');

    const inspector = body.querySelector('#studioInspector');
    if (inspector) {
        inspector.className = 'workbench-inspector';
        shell.classList.add('has-inspector');
    }

    shell.append(nav, mainWrap);
    if (inspector) shell.append(inspector);
    currentMain.replaceWith(shell);
    mainWrap.append(context, currentMain);
    body.appendChild(shell);
    const live = document.createElement('div');
    live.className = 'workbench-live';
    live.setAttribute('aria-live', 'polite');
    body.appendChild(live);

    function escapeHtml(value) {
        return String(value || '').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char]));
    }

    function readSelectedNovelId() {
        try {
            return localStorage.getItem('mirror:selectedNovelId') || '';
        } catch (_) {
            return '';
        }
    }

    function rememberSelectedNovelId(novelId) {
        try {
            localStorage.setItem('mirror:selectedNovelId', String(novelId));
        } catch (_) { }
    }

    function readVideoContext(searchParams) {
        const novelId = searchParams.get('novelId');
        const chapterNum = searchParams.get('chapterNum');
        return novelId && chapterNum ? {novelId, chapterNum} : null;
    }

    function readStoredVideoContext() {
        try {
            const stored = JSON.parse(localStorage.getItem('mirror:videoContext') || 'null');
            return stored && stored.novelId && stored.chapterNum ? stored : null;
        } catch (_) {
            return null;
        }
    }

    function buildVideoContextQuery(videoContext, novelId) {
        if (!videoContext || !novelId || String(videoContext.novelId) !== String(novelId)) {
            return '';
        }
        return `?novelId=${encodeURIComponent(videoContext.novelId)}&chapterNum=${encodeURIComponent(videoContext.chapterNum)}`;
    }
}());
