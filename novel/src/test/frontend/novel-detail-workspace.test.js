const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources/static');
const html = fs.readFileSync(path.join(root, 'novel-detail.html'), 'utf8');
const shellCss = fs.readFileSync(path.join(root, 'studio/css/workbench-shell.css'), 'utf8');

assert.match(html, /data-shell-page="detail"/);
assert.match(html, /workbench-shell\.css/);
assert.match(html, /workbench-shell\.js/);
assert.match(html, /id="generationStatus"[^>]*aria-live="polite"/);
assert.match(html, /id="storyboardStatus"[^>]*aria-live="polite"/);
assert.match(html, /id="assetStatus"[^>]*aria-live="polite"/);
assert.match(html, /id="chapterNav"/);
assert.match(html, /id="visualStyleStatus"/);
assert.match(html, /id="storyboardTabPanel"/);
assert.match(html, /id="assetTabPanel"/);
assert.match(shellCss, /\.studio-detail-page \.storyboard-panel[\s\S]*?display:\s*flex[\s\S]*?flex-direction:\s*column/, 'storyboard panel should use an ordered vertical work surface');
assert.match(shellCss, /\.studio-detail-page \.storyboard-panel\s*>\s*\.asset-tabs[\s\S]*?position:\s*static/, 'detail tabs should not float over storyboard content');
assert.match(shellCss, /\.studio-detail-page \.storyboard-panel\s*>\s*\.visual-style-section[\s\S]*?order:\s*3/, 'visual style should stay secondary to storyboard content');
assert.match(shellCss, /@media\s*\(min-width:\s*1600px\)[\s\S]*?\.studio-detail-page \.detail-layout[\s\S]*?grid-template-columns:\s*minmax\(300px,\s*\.42fr\)\s+minmax\(560px,\s*1\.12fr\)\s+minmax\(460px,\s*\.9fr\)/, 'wide detail workspace should give the directory and storyboard enough room');

console.log('novel detail workspace checks passed');
