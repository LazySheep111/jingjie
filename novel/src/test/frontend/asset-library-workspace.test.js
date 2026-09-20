const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources/static');
const html = fs.readFileSync(path.join(root, 'asset-library.html'), 'utf8');

assert.match(html, /data-shell-page="assets"/);
assert.match(html, /workbench-shell\.css/);
assert.match(html, /workbench-shell\.js/);
assert.match(html, /class="asset-library-layout workspace-shell"/);
assert.match(html, /id="novelFilterList"/);
assert.match(html, /id="assetGrid"/);
assert.match(html, /id="assetDetailPanel"/);
assert.match(html, /id="assetStatus"[^>]*aria-live="polite"/);
assert.match(html, /id="assetReuseModal"/);

console.log('asset library workspace checks passed');
