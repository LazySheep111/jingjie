const form = document.getElementById('outlineForm');
const submitBtn = document.getElementById('submitBtn');
const statusText = document.getElementById('statusText');
const emptyResult = document.getElementById('emptyResult');
const outlineResult = document.getElementById('outlineResult');

form.addEventListener('submit', async (event) => {
    event.preventDefault();

    const payload = {
        novelTitle: valueOf('novelTitle') || '未命名小说',
        category: valueOf('category') || '综合类型',
        novelLength: valueOf('novelLength'),
        endingType: valueOf('endingType'),
        writingStyle: valueOf('writingStyle'),
        targetAudience: valueOf('targetAudience'),
        protagonist: valueOf('protagonist'),
        roleList: parseRoleList(valueOf('roleList')),
        background: valueOf('background'),
        worldRule: valueOf('worldRule'),
        theme: valueOf('theme'),
        triggerEvent: valueOf('triggerEvent'),
        foreshadowCount: valueOf('foreshadowCount') || '5',
        narrativeView: valueOf('narrativeView'),
        avoidContent: valueOf('avoidContent')
    };

    const missingField = validatePayload(payload);
    if (missingField) {
        setStatus(`请填写：${missingField}`, true);
        return;
    }

    submitBtn.disabled = true;
    setStatus('正在生成大纲，请稍等...');

    try {
        const response = await fetch('/api/novel/generateOutline', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json'
            },
            body: JSON.stringify(payload)
        });

        if (!response.ok) {
            const text = await response.text();
            throw new Error(text || `请求失败：${response.status}`);
        }

        const data = await response.json();
        renderOutline(data);
        setStatus('大纲生成完成。');
    } catch (error) {
        console.error(error);
        setStatus(`生成失败：${error.message}`, true);
    } finally {
        submitBtn.disabled = false;
    }
});

function valueOf(id) {
    const element = document.getElementById(id);
    return element ? element.value.trim() : '';
}

function parseRoleList(text) {
    if (!text) {
        return [];
    }
    return text
        .split(/[\n;；]+/)
        .map(item => item.trim())
        .filter(Boolean);
}

function validatePayload(payload) {
    const requiredFields = [
        ['novelTitle', '小说名称'],
        ['category', '小说类型'],
        ['protagonist', '主角设定'],
        ['background', '时代背景'],
        ['worldRule', '世界规则'],
        ['theme', '核心主题'],
        ['triggerEvent', '开篇事件']
    ];

    for (const [key, label] of requiredFields) {
        if (!payload[key]) {
            return label;
        }
    }
    return '';
}

function setStatus(message, isError = false) {
    statusText.textContent = message;
    statusText.classList.toggle('error', isError);
}

function renderOutline(data) {
    emptyResult.hidden = true;
    outlineResult.hidden = false;

    document.getElementById('resultTitle').textContent = data.novelTitle || '小说大纲';
    document.getElementById('novelIdBadge').textContent = data.novelId ? `ID ${data.novelId}` : '已生成';
    document.getElementById('overallPlot').textContent = data.overallPlot || 'AI 未返回整体梗概。';

    const foreshadowList = document.getElementById('foreshadowList');
    foreshadowList.innerHTML = '';
    (data.foreshadowList || []).forEach(item => {
        const li = document.createElement('li');
        li.textContent = item;
        foreshadowList.appendChild(li);
    });
    if (!foreshadowList.children.length) {
        const li = document.createElement('li');
        li.textContent = 'AI 未返回伏笔设计。';
        foreshadowList.appendChild(li);
    }

    const chapterList = document.getElementById('chapterList');
    chapterList.innerHTML = '';
    (data.chapterList || []).forEach((chapter, index) => {
        const chapterNum = chapter.chapterNum || index + 1;
        const item = document.createElement('article');
        item.className = 'chapter';
        item.innerHTML = `
            <div class="chapter-title">
                <span>第 ${escapeHtml(chapterNum)} 章：${escapeHtml(chapter.chapterTitle || '未命名章节')}</span>
                <span>${escapeHtml(chapter.wordCount || '')} 字</span>
            </div>
            <p>${escapeHtml(chapter.chapterSummary || 'AI 未返回章节梗概。')}</p>
        `;
        chapterList.appendChild(item);
    });
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}
