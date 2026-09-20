const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources');
const html = fs.readFileSync(path.join(root, 'static/storyboard-workbench.html'), 'utf8');
const script = fs.readFileSync(path.join(root, 'static/js/storyboard-workbench.js'), 'utf8');
const outline = fs.readFileSync(path.join(root, 'templates/index.html'), 'utf8');

assert(html.includes('分镜脚本工作台'));
assert(html.includes('class="studio-theme studio-workbench-page"'));
assert(html.includes('class="toolbar workspace-header"'));
assert(html.includes('class="workbench-layout workspace-shell"'));
assert(html.includes('id="novelImportFile"'));
assert(html.includes('accept=".txt,.docx'));
assert(html.includes('id="recentNovelList"'));
assert(script.includes('/api/novel/import'));
assert(script.includes('/api/novel/history?page=1&pageSize=10'));
assert(script.includes('novel-detail.html?novelId='));
assert(!outline.includes('id="novelImportFile"'));
assert(outline.includes('class="studio-theme studio-outline-page"'));
assert(outline.includes('class="page workspace-shell outline-workspace-shell"'));

console.log('storyboard workbench page checks passed');
