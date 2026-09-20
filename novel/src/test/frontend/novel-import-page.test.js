const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources');
const html = fs.readFileSync(path.join(root, 'static/storyboard-workbench.html'), 'utf8');
const script = fs.readFileSync(path.join(root, 'static/js/storyboard-workbench.js'), 'utf8');

assert(html.includes('id="novelImportFile"'));
assert(html.includes('accept=".txt,.docx'));
assert(html.includes('id="novelImportBtn"'));
assert(script.includes('/api/novel/import'));
assert(script.includes("formData.append('file', file)"));
assert(script.includes('novel-detail.html?novelId='));

console.log('novel import page checks passed');
