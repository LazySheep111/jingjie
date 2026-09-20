const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources/static');
const html = fs.readFileSync(path.join(root, 'history-list.html'), 'utf8');
const script = fs.readFileSync(path.join(root, 'js/history-list.js'), 'utf8');
const shellCss = fs.readFileSync(path.join(root, 'studio/css/workbench-shell.css'), 'utf8');

assert(html.includes('class="studio-theme studio-history-page"'));
assert(html.includes('class="toolbar workspace-header"'));
assert(html.includes('class="history-list workspace-shell"'));
assert(html.includes('id="historyList"'));
assert(html.includes('id="refreshBtn"'));
assert(script.includes('/api/novel/history'));
assert(script.includes('workspace-project-row'));
assert(script.includes("localStorage.setItem('mirror:selectedNovelId'"), 'history selection should persist the active novel');
assert(script.includes("localStorage.removeItem('mirror:videoContext')"), 'changing novels should clear the active video context');
assert.match(shellCss, /\.studio-history-page \.history-list[\s\S]*?minmax\(360px/, 'history cards should have enough width for their actions');
assert.match(shellCss, /\.studio-history-page \.history-card\.workspace-project-row[\s\S]*?grid-template-columns:\s*minmax\(0,\s*1fr\)/, 'history cards should override the legacy multi-column row layout');
assert.match(shellCss, /\.studio-history-page \.history-card\.workspace-project-row \.card-actions[\s\S]*?flex-direction:\s*row/, 'history actions should stay horizontal and wrap naturally');

console.log('history-list page checks passed');
