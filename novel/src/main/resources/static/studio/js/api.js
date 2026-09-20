(function () {
    function unwrapResponse(payload) {
        if (payload && payload.success === false) throw new Error(payload.errorMsg || '请求失败');
        return payload && Object.prototype.hasOwnProperty.call(payload, 'data') ? payload.data : payload;
    }

    async function request(path, options) {
        const response = await fetch(path, options);
        let payload;
        try { payload = await response.json(); } catch (_) { payload = null; }
        if (!response.ok) throw new Error((payload && payload.errorMsg) || `请求失败：${response.status}`);
        return unwrapResponse(payload);
    }

    async function poll(path, isTerminal, onUpdate, interval) {
        const delay = interval || 1500;
        let cancelled = false;
        const cancel = () => { cancelled = true; };
        while (!cancelled) {
            const task = await request(path);
            if (onUpdate) onUpdate(task);
            if (isTerminal(task)) return {task, cancel};
            await new Promise(resolve => setTimeout(resolve, delay));
        }
        return {task: null, cancel};
    }

    window.studioApi = {
        request,
        poll,
        getStoryboard: (novelId, chapterNum) => request(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/storyboard`),
        getChapterAssets: (novelId, chapterNum) => request(`/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}/assets`)
    };
}());
