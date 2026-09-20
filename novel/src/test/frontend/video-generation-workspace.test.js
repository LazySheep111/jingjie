const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources/static');
const html = fs.readFileSync(path.join(root, 'video-generation.html'), 'utf8');
const shellCss = fs.readFileSync(path.join(root, 'studio/css/workbench-shell.css'), 'utf8');

assert.match(html, /data-shell-page="video"/);
assert.match(html, /workbench-shell\.css/);
assert.match(html, /workbench-shell\.js/);
assert.match(html, /id="pageStatus"[^>]*aria-live="polite"/);
assert.match(html, /id="sceneList"[^>]*video-output-console/);
assert.match(html, /id="pageSummary"/);
assert.match(html, /id="firstFramePreviewDialog"/);

// The control rail must use an explicit single-column structure. This prevents
// the legacy outline.css auto-placement rules from scattering the actions.
assert.match(shellCss, /video-generation-body[\s\S]*video-stage-operations[\s\S]*grid-template-columns:\s*minmax\(0,\s*1fr\)/);
assert.match(shellCss, /video-generation-body[\s\S]*video-frame-actions[\s\S]*display:\s*flex/);
assert.match(shellCss, /video-generation-body[\s\S]*video-primary-actions[\s\S]*display:\s*flex/);

console.log('video generation workspace checks passed');
