const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources/static');
const html = fs.readFileSync(path.join(root, 'model-management.html'), 'utf8');
const script = fs.readFileSync(path.join(root, 'studio/js/model-management.js'), 'utf8');
const css = fs.readFileSync(path.join(root, 'studio/css/model-management.css'), 'utf8');

for (const marker of ['data-shell-page="model"', '/studio/css/model-management.css', '/studio/js/model-management.js']) {
    if (!html.includes(marker)) throw new Error(`model management page missing ${marker}`);
}
for (const marker of ['/api/model-configs', 'data-action="test"', 'data-action="save"', 'data-action="disable"', 'apiKeyMasked', 'new-password']) {
    if (!script.includes(marker) && !html.includes(marker)) throw new Error(`model management behavior missing ${marker}`);
}
for (const marker of ['grid-template-columns', 'model-card', '@media']) {
    if (!css.includes(marker)) throw new Error(`model management layout missing ${marker}`);
}
if (script.includes('localStorage') || script.includes('sessionStorage')) {
    throw new Error('model management must not store API keys in browser storage');
}
console.log('model management page checks passed');
