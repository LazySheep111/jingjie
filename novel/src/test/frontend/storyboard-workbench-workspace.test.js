const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources/static');
const html = fs.readFileSync(path.join(root, 'storyboard-workbench.html'), 'utf8');
const script = fs.readFileSync(path.join(root, 'studio/js/workbench.js'), 'utf8');

assert.match(html, /class="scene-track scene-timeline"/);
assert.match(html, /id="sceneEditor"/);
assert.match(html, /id="assetInspector"/);
assert.match(html, /id="videoStatus"[^>]*aria-live="polite"/);
assert.match(html, /data-action="generate-video"/);
assert.strictEqual((html.match(/id="sceneEditor"/g) || []).length, 1);
assert.match(script, /renderSelectedScene/);
assert.match(script, /first-frame-tasks/);
assert.match(script, /video-tasks/);

console.log('storyboard workbench workspace checks passed');
