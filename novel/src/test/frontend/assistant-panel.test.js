const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources/static');
const js = fs.readFileSync(path.join(root, 'studio/js/assistant-panel.js'), 'utf8');
const css = fs.readFileSync(path.join(root, 'studio/css/assistant-panel.css'), 'utf8');

assert.match(js, /\/api\/assistant\/chat/, 'assistant should call the read-only chat endpoint');
assert.match(js, /\/api\/assistant\/conversations\/current/, 'assistant should restore the current conversation');
assert.match(js, /\/api\/assistant\/conversations\?page=/, 'assistant should load paginated conversation history');
assert.match(js, /method: 'DELETE'/, 'assistant should delete a conversation from history');
assert.match(js, /novelId: params\.get\('novelId'\)/, 'assistant should capture the current novel context');
assert.match(js, /chapterNum: params\.get\('chapterNum'\)/, 'assistant should capture the current chapter context');
assert.match(js, /assistant-drawer/, 'assistant should render a drawer');
assert.match(js, /assistant-trigger-float/, 'assistant should use a dedicated floating trigger');
assert.match(js, /不会生成、修改、删除或上传/, 'assistant should declare its read-only scope');
assert.match(css, /@media \(max-width: 720px\)[\s\S]*?\.assistant-drawer \{ width: 100vw/, 'assistant should become full-screen on narrow screens');
assert.match(css, /\.assistant-trigger-float\s*\{[\s\S]*?position:\s*fixed[\s\S]*?top:\s*96px/, 'assistant trigger should float below the workbench context bar');

for (const page of ['outline-new.html', 'history-list-new.html', 'storyboard-workbench-new.html', 'novel-detail-new.html', 'asset-library-new.html', 'video-generation-new.html']) {
    const html = fs.readFileSync(path.join(root, page), 'utf8');
    assert.match(html, /assistant-panel\.css\?v=20260919g/, `${page} should load assistant styles`);
    assert.match(html, /assistant-panel\.js\?v=20260919g/, `${page} should load assistant behavior`);
}

console.log('assistant panel checks passed');
