(function () {
    const grid = document.getElementById('modelConfigGrid');
    const pageStatus = document.getElementById('modelPageStatus');
    if (!grid) return;

    const meta = {
        TEXT: { label: '文本模型', description: '小说大纲、全文、分镜和资产提示词', provider: 'OPENAI_COMPATIBLE' },
        IMAGE: { label: '图像模型', description: '人物、场景三视图与分镜首帧', provider: 'OPENAI_COMPATIBLE' },
        VIDEO: { label: '视频模型', description: '根据首帧和镜头脚本生成视频', provider: 'ARK' }
    };

    load();

    async function load() {
        try {
            const configs = await window.studioApi.request('/api/model-configs');
            grid.innerHTML = configs.map(renderCard).join('');
        } catch (error) {
            grid.innerHTML = '<div class="model-empty">暂时无法读取模型配置，请确认后端已执行建表脚本。</div>';
            showPageStatus(error.message, false);
        }
    }

    function renderCard(config) {
        const type = config.capabilityType;
        const info = meta[type] || meta.TEXT;
        const active = config.enabled === true;
        const error = config.testStatus === 'ERROR';
        const provider = config.providerType || info.provider;
        const providerOptions = type === 'VIDEO'
            ? `<option value="ARK" ${provider === 'ARK' ? 'selected' : ''}>方舟 Ark</option><option value="MINIMAX" ${provider === 'MINIMAX' ? 'selected' : ''}>MiniMax</option>`
            : '<option value="OPENAI_COMPATIBLE" selected>OpenAI 兼容接口</option>';
        return `<article class="model-card" data-capability="${escapeHtml(type)}">
            <header class="model-card-head"><div><p class="model-card-kicker">${escapeHtml(type)}</p><h2>${escapeHtml(info.label)}</h2><p class="model-card-description">${escapeHtml(info.description)}</p></div><span class="model-state ${active ? 'is-active' : ''} ${error ? 'is-error' : ''}">${active ? '已启用' : (error ? '需检查' : '未启用')}</span></header>
            <form class="model-form">
                <label>服务商<select data-field="providerType">${providerOptions}</select></label>
                <label>模型名称<input data-field="modelName" type="text" value="${escapeAttr(config.modelName || '')}" placeholder="例如：deepseek-chat"></label>
                <label>API 地址<input data-field="apiUrl" type="url" value="${escapeAttr(config.apiUrl || '')}" placeholder="https://api.example.com/v1"></label>
                <label>API Key<input data-field="apiKey" type="password" value="" autocomplete="new-password" placeholder="${config.apiKeyMasked && config.apiKeyMasked !== '未配置' ? '已配置，重新填写可替换' : '输入 API Key'}"><span class="field-help">${escapeHtml(config.apiKeyMasked || '未配置')} · 页面不会回显原始密钥</span></label>
                <div class="advanced-fields" ${type === 'VIDEO' ? '' : 'hidden'}><label>任务查询地址<input data-field="queryUrl" type="url" value="${escapeAttr(config.queryUrl || '')}" placeholder="https://api.example.com/tasks/{taskId}"><span class="field-help">视频任务完成状态的轮询地址，必须包含 {taskId}</span></label></div>
                <div class="model-actions"><button class="model-test-button" type="button" data-action="test">测试连接</button><button class="model-save-button" type="button" data-action="save">测试并保存</button>${active ? '<button class="model-disable-button" type="button" data-action="disable">停用</button>' : ''}</div>
                <p class="model-test-result" data-role="result" aria-live="polite"></p>
            </form>
        </article>`;
    }

    grid.addEventListener('change', (event) => {
        if (event.target.matches('[data-field="providerType"]') && event.target.value === 'MINIMAX') {
            const card = event.target.closest('.model-card');
            const model = card.querySelector('[data-field="modelName"]');
            if (!model.value) model.value = 'MiniMax-H3';
        }
    });

    grid.addEventListener('click', async (event) => {
        const button = event.target.closest('button[data-action]');
        if (!button) return;
        const card = button.closest('.model-card');
        const type = card.dataset.capability;
        const action = button.dataset.action;
        button.disabled = true;
        setResult(card, '正在处理…');
        try {
            if (action === 'test') {
                const result = await window.studioApi.request(`/api/model-configs/${type}/test`, { method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(readForm(card)) });
                setResult(card, result.message || '连接测试通过', true);
            } else if (action === 'save') {
                await window.studioApi.request(`/api/model-configs/${type}`, { method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(readForm(card)) });
                setResult(card, '连接测试通过，配置已保存并启用', true);
                await load();
                showPageStatus(`${meta[type].label}配置已更新`, true);
            } else if (action === 'disable') {
                await window.studioApi.request(`/api/model-configs/${type}/disable`, { method: 'POST' });
                await load();
                showPageStatus(`${meta[type].label}已停用`, true);
            }
        } catch (error) {
            setResult(card, error.message, false);
            showPageStatus(error.message, false);
        } finally {
            button.disabled = false;
        }
    });

    function readForm(card) {
        const value = field => card.querySelector(`[data-field="${field}"]`)?.value.trim() || '';
        return { providerType: value('providerType'), apiUrl: value('apiUrl'), queryUrl: value('queryUrl'), apiKey: value('apiKey'), modelName: value('modelName') };
    }

    function setResult(card, message, success) {
        const element = card.querySelector('[data-role="result"]');
        element.textContent = message || '';
        element.className = `model-test-result ${success === true ? 'success' : (success === false ? 'error' : '')}`;
    }

    function showPageStatus(message, success) {
        pageStatus.hidden = !message;
        pageStatus.textContent = message || '';
        pageStatus.className = `model-page-status ${success ? 'success' : ''}`;
    }

    function escapeAttr(value) { return escapeHtml(value).replace(/`/g, '&#96;'); }
    function escapeHtml(value) { return String(value || '').replace(/[&<>'"]/g, char => ({'&':'&amp;','<':'&lt;','>':'&gt;',"'":'&#39;','"':'&quot;'}[char])); }
}());
