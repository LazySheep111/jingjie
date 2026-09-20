const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources/static');
const shellCss = fs.readFileSync(path.join(root, 'studio/css/workbench-shell.css'), 'utf8');
const shellJs = fs.readFileSync(path.join(root, 'studio/js/workbench-shell.js'), 'utf8');
assert.match(shellCss, /--wb-canvas:/, 'shared shell should expose the production-console canvas token');
assert.match(shellCss, /\.workbench-status-strip/, 'shared shell should style local production status strips');
assert.match(shellCss, /body\.workbench-page\s*\{[\s\S]*?width:\s*100%[\s\S]*?max-width:\s*none[\s\S]*?padding:\s*0/, 'shared shell should release the legacy body width constraint');
assert.match(shellCss, /@media \(min-width: ?721px\)[\s\S]*?\.workbench-nav\s*\{[\s\S]*?position:\s*sticky[\s\S]*?top:\s*0[\s\S]*?height:\s*100dvh[\s\S]*?overflow-y:\s*auto/, 'desktop workbench navigation should remain fixed in the viewport and scroll internally');
assert.match(shellCss, /@media \(max-width: ?720px\)[\s\S]*?\.workbench-nav\s*\{[\s\S]*?position:\s*static[\s\S]*?height:\s*auto[\s\S]*?overflow:\s*visible/, 'narrow screens should return navigation to document flow');
const modelLinkRule = shellCss.match(/\.workbench-nav-model-link\s*\{([\s\S]*?)\}/);
assert.ok(modelLinkRule, 'model management link should have a dedicated rule');
assert.ok(!/margin-top\s*:\s*auto/.test(modelLinkRule[1]), 'model management link should stay directly below the asset link');
assert.ok(!shellJs.includes("filter(([, , key]) => key !== 'model')"), 'model management should remain in the same navigation group');
assert.ok(!shellJs.includes('当前任务'), 'shared shell should not render the removed current-task panel');
assert.ok(!shellJs.includes('工作提示'), 'shared shell should not render the removed work-hint panel');
assert.ok(!shellJs.includes('toggle-task-drawer'), 'shared shell should not keep the removed task drawer entry point');
assert.ok(shellJs.includes("localStorage.getItem('mirror:selectedNovelId')"), 'shared shell should restore the last selected novel');
assert.ok(shellJs.includes("localStorage.getItem('mirror:videoContext')"), 'shared shell should restore the active video context');
assert.ok(shellJs.includes('chapterNum'), 'shared shell should carry the active chapter into video workspaces');
assert.ok(!shellJs.includes("['/storyboard-workbench-new.html'"), 'new navigation should not include the redundant storyboard workspace');
assert.ok(shellJs.includes("['/video-generation-new.html' + videoContextQuery, '视频创作工作台', 'video']"));
const navOrder = [
    "['/outline-new.html', '小说大纲工作台', 'outline']",
    "['/novel-detail-new.html'",
    "'小说全文工作台', 'detail']",
    "['/history-list-new.html', '我的历史作品', 'history']",
    "['/video-generation-new.html'",
    "'视频创作工作台', 'video'",
    "['/asset-library-new.html', '资产库', 'assets']",
    "['/model-management-new.html', '模型管理', 'model']"
];
let previousIndex = -1;
for (const marker of navOrder) {
    const index = shellJs.indexOf(marker);
    assert.ok(index > previousIndex, `navigation marker should keep the requested order: ${marker}`);
    previousIndex = index;
}
assert.match(shellJs, /href="\/outline-new\.html"/, 'brand should return to the new outline workbench');
const pages = [
    'asset-library.html',
    'history-list.html',
    'novel-detail.html',
    'storyboard-workbench.html',
    'video-generation.html'
];

for (const page of pages) {
    const html = fs.readFileSync(path.join(root, page), 'utf8');
    assert.match(html, /workbench-shell\.css/, `${page} should load shared shell styles`);
    assert.match(html, /workbench-shell\.js/, `${page} should load shared shell behavior`);
    assert.match(html, /data-shell-page=/, `${page} should identify its shell page`);
    assert.ok(!html.includes('data-action="toggle-task-drawer"'), `${page} should not expose the removed task drawer toggle`);
}

const previewPages = [
    'asset-library-new.html',
    'history-list-new.html',
    'model-management-new.html',
    'novel-detail-new.html',
    'outline-new.html',
    'storyboard-workbench-new.html',
    'video-generation-new.html'
];
for (const page of previewPages) {
    const html = fs.readFileSync(path.join(root, page), 'utf8');
    assert.match(html, /workbench-shell\.js\?v=20260918c/, `${page} should request the current navigation script version`);
}

const legacyRoutes = [
    'asset-library.html',
    'history-list.html',
    'model-management.html',
    'novel-detail.html',
    'storyboard-workbench.html',
    'video-generation.html'
];
for (const page of legacyRoutes) {
    const html = fs.readFileSync(path.join(root, page), 'utf8');
    assert.match(html, /\/studio\/js\/legacy-page-redirect\.js/, `${page} should redirect to its new workbench by default`);
}
const legacyRedirect = fs.readFileSync(path.join(root, 'studio/js/legacy-page-redirect.js'), 'utf8');
assert.match(legacyRedirect, /params\.get\('legacy'\) === '1'/, 'legacy pages should remain reachable with the legacy escape hatch');
assert.match(legacyRedirect, /window\.location\.replace/, 'legacy pages should replace browser history with the new workbench route');

console.log('workbench shell page checks passed');
