const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources');
const html = fs.readFileSync(path.join(root, 'templates/index.html'), 'utf8');

assert.match(html, /data-shell-page="outline"/);
assert.match(html, /workbench-shell\.css/);
assert.match(html, /workbench-shell\.js/);
assert.match(html, /id="outlineForm"/);
assert.match(html, /id="submitBtn"/);
assert.match(html, /id="statusText"[^>]*aria-live="polite"/);
assert.match(html, /id="outlineResult"/);
const shellCss = fs.readFileSync(path.join(root, 'static/studio/css/workbench-shell.css'), 'utf8');
assert.match(shellCss, /\.workbench-page \.page[\s\S]*?width:\s*100%[\s\S]*?max-width:\s*none/, 'outline page should use the full workbench content width');

console.log('outline workspace checks passed');
