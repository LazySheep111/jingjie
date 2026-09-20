const form = document.getElementById('outlineForm');
const submitBtn = document.getElementById('submitBtn');
const statusText = document.getElementById('statusText');
const emptyResult = document.getElementById('emptyResult');
const outlineResult = document.getElementById('outlineResult');
const newCreateBtn = document.getElementById('newCreateBtn');
const saveOutlineBtn = document.getElementById('saveOutlineBtn');
const generateFullBtn = document.getElementById('generateFullBtn');

let currentPayload = null;
let currentOutline = null;
let restoredFullGenerated = false;

newCreateBtn.addEventListener('click', () => {
    form.reset();
    emptyResult.hidden = false;
    outlineResult.hidden = true;
    currentPayload = null;
    currentOutline = null
    restoredFullGenerated = false;
    generateFullBtn.textContent = '确认大纲，生成全文小说';
    document.getElementById('resultTitle').textContent = '等待生成';
    document.getElementById('novelIdBadge').textContent = '未生成';
    setStatus('');
});

saveOutlineBtn.addEventListener('click', async () => {
    if (!currentOutline || !currentPayload) {
        setStatus('请先生成大纲。', true);
        return;
    }
    await saveOutline(false);
});

generateFullBtn.addEventListener('click', async () => {
    if (!currentOutline || !currentPayload) {
        setStatus('请先生成大纲。', true);
        return;
    }

    const existingNovelId = currentOutline.novelId;

    if (restoredFullGenerated && existingNovelId) {
        window.location.href = `/novel-detail.html?novelId=${encodeURIComponent(existingNovelId)}`;
        return;
    }

    const novelId = await saveOutline(false);
    if (novelId) {
        sessionStorage.setItem(`creationSnapshot:${novelId}`, JSON.stringify({
            formData: currentPayload,
            outline: currentOutline,
            fullGenerated: false
        }));
        sessionStorage.setItem(`fullGenerationPayload:${novelId}`, JSON.stringify({
            novelId,
            outline: currentOutline
        }));

        window.location.href = `/novel-detail.html?novelId=${encodeURIComponent(novelId)}&autoGenerate=1`;
    }
});

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
        currentPayload = payload;
        currentOutline = data;
        renderOutline(data);
        setStatus('大纲生成完成。');
    } catch (error) {
        console.error(error);
        setStatus(`生成失败：${error.message}`, true);
    } finally {
        submitBtn.disabled = false;
    }
});

async function saveOutline(startFullGeneration) {
    setStatus(startFullGeneration ? '正在确认大纲并准备生成全文...' : '正在保存大纲...');

    const payload = {
        novelId: currentOutline.novelId,
        formData: currentPayload,
        outline: currentOutline,
        startFullGeneration
    };

    try {
        const response = await fetch('/api/novel/saveOutline', {
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
        const novelId = pickNovelId(data) || currentOutline.novelId;
        currentOutline.novelId = novelId;
        document.getElementById('novelIdBadge').textContent = novelId ? `ID ${novelId}` : '已保存';

        if (startFullGeneration) {
            await startFullNovelGeneration(novelId);
        } else {
            setStatus('大纲已保存至历史记录。');
        }

        return novelId;
    } catch (error) {
        console.error(error);
        setStatus(`保存失败：${error.message}`, true);
        return '';
    }
}

async function startFullNovelGeneration(novelId) {
    if (!novelId) {
        throw new Error('后端未返回 novelId');
    }

    const response = await fetch(`/api/novel/${encodeURIComponent(novelId)}/generateFull`, {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json'
        },
        body: JSON.stringify({
            novelId,
            outline: currentOutline
        })
    });

    if (!response.ok) {
        const text = await response.text();
        throw new Error(text || `请求失败：${response.status}`);
    }

    setStatus('全文生成任务已开始，正在跳转...');
}

function pickNovelId(data) {
    if (data == null) {
        return '';
    }
    if (typeof data === 'number' || typeof data === 'string') {
        return data;
    }
    if (data.novelId) {
        return data.novelId;
    }
    if (data.data && (typeof data.data === 'number' || typeof data.data === 'string')) {
        return data.data;
    }
    if (data.data && data.data.novelId) {
        return data.data.novelId;
    }
    return '';
}

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
        li.textContent = typeof item === 'string' ? item : item.description;
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



async function restoreCreationSnapshot() {
    const restoreNovelId = new URLSearchParams(window.location.search).get('restoreNovelId');
    if (!restoreNovelId) {
        return;
    }

    const raw = sessionStorage.getItem(`creationSnapshot:${restoreNovelId}`);
    let snapshot = null;

    if (raw) {
        snapshot = JSON.parse(raw);
    } else {
        try {
            setStatus('正在恢复历史大纲...');
            const response = await fetch(`/api/novel/${encodeURIComponent(restoreNovelId)}/outline`);
            if (!response.ok) {
                const text = await response.text();
                throw new Error(text || `请求失败：${response.status}`);
            }
            snapshot = normalizeRestoredOutline(await response.json(), restoreNovelId);
        } catch (error) {
            setStatus(`恢复历史大纲失败：${error.message}`, true);
            return;
        }
    }

    currentPayload = snapshot.formData || {};
    currentOutline = snapshot.outline || null;
    restoredFullGenerated = snapshot.fullGenerated === true || snapshot.status === 'FULL_GENERATED';
    if (currentOutline && !currentOutline.novelId) {
        currentOutline.novelId = restoreNovelId;
    }
    if (restoredFullGenerated) {
        generateFullBtn.textContent = '查看已生成全文小说';
    }

    Object.entries(currentPayload || {}).forEach(([key, value]) => {
        const element = document.getElementById(key);
        if (!element) {
            return;
        }

        element.value = Array.isArray(value) ? value.join('\n') : value;
    });

    if (currentOutline) {
        renderOutline(currentOutline);
        setStatus('已恢复历史作品的大纲内容。');
    }
}

function normalizeRestoredOutline(data, restoreNovelId) {
    const body = data && typeof data === 'object' && 'data' in data ? data.data : data;
    if (!body || typeof body !== 'object') {
        return {
            formData: {},
            outline: null,
            status: '',
            fullGenerated: false
        };
    }

    const outline = body.outline || body.novelOutline || body;
    const formData = body.formData || body.keys || body.request || {};

    if (!outline.novelId) {
        outline.novelId = body.novelId || restoreNovelId;
    }

    return {
        formData,
        outline,
        status: body.status || outline.status,
        fullGenerated: body.fullGenerated === true || body.status === 'FULL_GENERATED'
    };
}

restoreCreationSnapshot();
