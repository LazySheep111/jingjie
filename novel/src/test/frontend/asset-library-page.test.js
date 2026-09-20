const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources/static');
const html = fs.readFileSync(path.join(root, 'asset-library.html'), 'utf8');
const script = fs.readFileSync(path.join(root, 'js/asset-library.js'), 'utf8');

assert(html.includes('class="studio-theme studio-asset-library-page"'));
assert(html.includes('class="toolbar workspace-header"'));
assert(html.includes('class="asset-library-layout workspace-shell"'));
assert(html.includes('id="novelFilterList"'));
assert(html.includes('id="assetGrid"'));
assert(html.includes('id="assetDetailPanel"'));
assert(html.includes('href="/asset-library.html"'));
assert(script.includes('function buildAssetQuery'));
assert(script.includes('function normalizeLibraryResponse'));
assert(script.includes('function buildVersionPayload'));
assert(script.includes('function assetPromptValue'));
assert(!script.includes('<label>正面提示词<textarea'));
assert(!script.includes('<label>侧面提示词<textarea'));
assert(!script.includes('<label>背面提示词<textarea'));
assert(script.includes('frontPrompt: values.prompt'));
assert(script.includes('sidePrompt: values.prompt'));
assert(script.includes('backPrompt: values.prompt'));
assert(script.includes('function buildReusePayload'));
assert(script.includes('targetAssetId'));
assert(script.includes('normalizeList(unwrapResponse(data)'));
assert(html.includes('id="reuseAssetSelect"'));
assert(script.includes('/api/novel/${encodeURIComponent(targetNovelId)}/assets?assetType='));
assert(script.includes("/api/visual-assets/library?"));
assert(script.includes("/api/visual-assets/${encodeURIComponent(assetId)}/versions"));
assert(script.includes("/api/visual-assets/${encodeURIComponent(assetId)}/reuse"));
assert(script.includes('asset-gallery-card'));

console.log('asset-library page checks passed');
