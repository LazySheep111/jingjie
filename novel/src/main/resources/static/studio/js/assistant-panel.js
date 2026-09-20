(function () {
    'use strict';
    if (window.__mirrorAssistantMounted) return;
    window.__mirrorAssistantMounted = true;

    const params = new URLSearchParams(window.location.search);
    const context = {
        novelId: params.get('novelId') || readStorage('mirror:selectedNovelId'),
        chapterNum: params.get('chapterNum') || '',
        sceneId: params.get('sceneId') || '',
        taskId: params.get('taskId') || '',
        page: document.body.dataset.shellPage || document.title || location.pathname
    };
    let conversationId = '';

    const trigger = document.createElement('button');
    trigger.type = 'button';
    trigger.className = 'assistant-trigger assistant-trigger-float';
    trigger.setAttribute('aria-controls', 'mirrorAssistantDrawer');
    trigger.setAttribute('aria-expanded', 'false');
    trigger.innerHTML = '<span class="assistant-trigger-dot" aria-hidden="true"></span><span>打开 AI 助手</span>';

    const backdrop = document.createElement('div');
    backdrop.className = 'assistant-backdrop';
    backdrop.setAttribute('aria-hidden', 'true');

    const drawer = document.createElement('aside');
    drawer.id = 'mirrorAssistantDrawer';
    drawer.className = 'assistant-drawer';
    drawer.setAttribute('aria-label', 'AI 只读助手');
    drawer.innerHTML = `<header class="assistant-drawer-header"><div><p class="assistant-eyebrow">READ-ONLY COPILOT</p><h2>AI 助手</h2><p class="assistant-subtitle">只读查看当前作品、章节、分镜与资产状态</p></div><div class="assistant-header-actions"><button class="assistant-history-toggle" type="button">历史会话</button><button class="assistant-close" type="button" aria-label="关闭 AI 助手">×</button></div></header><div class="assistant-context" aria-label="当前上下文">${contextChip('页面', context.page)}${contextChip('作品', context.novelId || '通用')}${context.chapterNum ? contextChip('章节', '第 ' + context.chapterNum + ' 章') : ''}</div><section class="assistant-history-panel" hidden><div class="assistant-history-head"><strong>历史会话</strong><button type="button" class="assistant-history-close">关闭</button></div><div class="assistant-history-list"></div><button type="button" class="assistant-history-more">加载更多</button></section><div class="assistant-messages" aria-live="polite"></div><div class="assistant-suggestions"><button type="button" class="assistant-suggestion">当前作品有多少章？</button><button type="button" class="assistant-suggestion">当前章节有没有分镜脚本？</button><button type="button" class="assistant-suggestion">当前作品有哪些人物和场景资产？</button></div><p class="assistant-readonly-note">助手不会生成、修改、删除或上传内容。</p><form class="assistant-compose"><textarea name="message" rows="1" placeholder="问问当前作品…" aria-label="向 AI 助手提问"></textarea><button class="assistant-send" type="submit">发送</button></form>`;
    document.body.append(trigger, backdrop, drawer);

    const messages = drawer.querySelector('.assistant-messages');
    const form = drawer.querySelector('.assistant-compose');
    const textarea = form.querySelector('textarea');
    const send = form.querySelector('.assistant-send');
    const historyPanel = drawer.querySelector('.assistant-history-panel');
    const historyList = drawer.querySelector('.assistant-history-list');
    let historyPage = 0;

    loadCurrentConversation();

    function open() { drawer.classList.add('is-open'); backdrop.classList.add('is-open'); trigger.setAttribute('aria-expanded', 'true'); textarea.focus(); }
    function close() { drawer.classList.remove('is-open'); backdrop.classList.remove('is-open'); trigger.setAttribute('aria-expanded', 'false'); }
    trigger.addEventListener('click', open);
    backdrop.addEventListener('click', close);
    drawer.querySelector('.assistant-close').addEventListener('click', close);
    drawer.querySelector('.assistant-history-toggle').addEventListener('click', () => { historyPanel.hidden = false; loadHistory(true); });
    drawer.querySelector('.assistant-history-close').addEventListener('click', () => { historyPanel.hidden = true; });
    drawer.querySelector('.assistant-history-more').addEventListener('click', () => loadHistory(false));
    document.addEventListener('keydown', event => { if (event.key === 'Escape') close(); });
    drawer.querySelectorAll('.assistant-suggestion').forEach(button => button.addEventListener('click', () => { textarea.value = button.textContent; form.requestSubmit(); }));
    form.addEventListener('submit', async event => {
        event.preventDefault();
        const message = textarea.value.trim();
        if (!message || send.disabled) return;
        addMessage('user', message);
        textarea.value = '';
        send.disabled = true;
        const loading = addMessage('loading', '正在查询只读信息…');
        try {
            const response = await fetch('/api/assistant/chat', { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify({ ...context, conversationId, message }) });
            const payload = await response.json();
            if (!response.ok || !payload.success) throw new Error(payload.errorMsg || '助手请求失败');
            loading.remove();
            if (payload.data && payload.data.conversationId) conversationId = payload.data.conversationId;
            addMessage('assistant', payload.data && payload.data.answer ? payload.data.answer : '暂时没有可用回答。');
        } catch (error) {
            loading.className = 'assistant-message error';
            loading.textContent = error.message || '助手暂时不可用，请稍后重试。';
        } finally { send.disabled = false; }
    });

    async function loadCurrentConversation() {
        try {
            const query = context.novelId ? '?novelId=' + encodeURIComponent(context.novelId) : '';
            const response = await fetch('/api/assistant/conversations/current' + query);
            const payload = await response.json();
            if (!response.ok || !payload.success) throw new Error(payload.errorMsg || '会话加载失败');
            conversationId = payload.data.conversationId;
            const messagesResponse = await fetch('/api/assistant/conversations/' + encodeURIComponent(conversationId) + '/messages?rounds=20');
            const messagesPayload = await messagesResponse.json();
            messages.innerHTML = '';
            const history = messagesPayload.success && Array.isArray(messagesPayload.data) ? messagesPayload.data : [];
            history.sort((a, b) => Number(a.sequenceNo || 0) - Number(b.sequenceNo || 0));
            history.forEach(item => addMessage(item.role === 'user' ? 'user' : item.status === 'FAILED' ? 'error' : 'assistant', item.content || ''));
            if (!history.length) addMessage('assistant', context.novelId ? '我可以帮你查看当前作品的章节、正文、分镜、资产和任务状态。' : '你可以直接咨询小说和视频创作问题。');
        } catch (error) {
            messages.innerHTML = '';
            addMessage('error', error.message || '会话加载失败，请稍后重试。');
        }
    }

    async function loadHistory(reset) {
        if (reset) { historyPage = 0; historyList.innerHTML = ''; }
        const response = await fetch('/api/assistant/conversations?page=' + historyPage + '&size=20');
        const payload = await response.json();
        if (!response.ok || !payload.success) return;
        (payload.data || []).forEach(conversation => {
            const row = document.createElement('div');
            row.className = 'assistant-history-row';
            row.innerHTML = '<button type="button" class="assistant-history-open"><strong></strong><small></small></button><button type="button" class="assistant-history-delete">删除</button>';
            row.querySelector('strong').textContent = conversation.title || '通用助手';
            row.querySelector('small').textContent = (conversation.lastMessagePreview || '暂无消息') + ' · ' + formatTime(conversation.updatedAt);
            row.querySelector('.assistant-history-open').addEventListener('click', () => {
                conversationId = conversation.conversationId;
                historyPanel.hidden = true;
                loadConversationMessages(conversationId);
            });
            row.querySelector('.assistant-history-delete').addEventListener('click', async () => {
                if (!window.confirm('确定删除这段聊天记录吗？删除后不可恢复。')) return;
                await fetch('/api/assistant/conversations/' + encodeURIComponent(conversation.conversationId), { method: 'DELETE' });
                row.remove();
            });
            historyList.appendChild(row);
        });
        historyPage += 1;
        const total = Number(payload.total || 0);
        drawer.querySelector('.assistant-history-more').hidden = historyPage * 20 >= total;
    }

    async function loadConversationMessages(id) {
        const response = await fetch('/api/assistant/conversations/' + encodeURIComponent(id) + '/messages?rounds=20');
        const payload = await response.json();
        messages.innerHTML = '';
        if (payload.success && Array.isArray(payload.data)) {
            payload.data.sort((a, b) => Number(a.sequenceNo || 0) - Number(b.sequenceNo || 0));
            payload.data.forEach(item => addMessage(item.role === 'user' ? 'user' : item.status === 'FAILED' ? 'error' : 'assistant', item.content || ''));
        }
    }

    function formatTime(value) {
        if (!value) return '刚刚';
        return String(value).replace('T', ' ').slice(0, 16);
    }

    function addMessage(type, text) {
        const node = document.createElement('div');
        node.className = 'assistant-message ' + (type === 'assistant' ? '' : type);
        node.textContent = text;
        messages.appendChild(node);
        messages.scrollTop = messages.scrollHeight;
        return node;
    }

    function contextChip(label, value) {
        return '<span class="assistant-context-chip"><strong>' + escapeHtml(label) + '</strong> ' + escapeHtml(value) + '</span>';
    }
    function escapeHtml(value) { return String(value || '').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char])); }
    function readStorage(key) { try { return localStorage.getItem(key) || ''; } catch (_) { return ''; } }
}());
