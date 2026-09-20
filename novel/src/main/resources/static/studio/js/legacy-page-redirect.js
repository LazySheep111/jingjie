(function () {
    const params = new URLSearchParams(window.location.search);
    if (params.get('legacy') === '1') return;

    const targets = {
        '/outline': '/outline-new.html',
        '/asset-library.html': '/asset-library-new.html',
        '/history-list.html': '/history-list-new.html',
        '/model-management.html': '/model-management-new.html',
        '/novel-detail.html': '/novel-detail-new.html',
        '/storyboard-workbench.html': '/storyboard-workbench-new.html',
        '/video-generation.html': '/video-generation-new.html'
    };
    const target = targets[window.location.pathname];
    if (!target) return;

    window.location.replace(target + window.location.search + window.location.hash);
}());
