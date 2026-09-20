const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const test = require('node:test');

const staticRoot = path.resolve(__dirname, '../../main/resources/static');

const pages = [
  { file: 'outline-new.html', script: '/js/outline.js', ids: ['outlineForm', 'novelTitle', 'submitBtn', 'statusText'] },
  { file: 'novel-detail-new.html', script: '/js/novel-detail.js', ids: ['chapterNav', 'chapterContent', 'storyboardTabBtn', 'visualStyleEra'] },
  { file: 'history-list-new.html', script: '/js/history-list.js', ids: ['refreshBtn', 'historyList', 'historyEmpty', 'prevPageBtn', 'nextPageBtn'] },
  { file: 'asset-library-new.html', script: '/js/asset-library.js', ids: ['assetGrid', 'assetDetailPanel', 'assetReuseModal', 'assetImagePreviewModal'] },
  { file: 'video-generation-new.html', script: '/js/video-generation.js', ids: ['sceneList', 'pageStatus', 'pageEmpty', 'firstFramePreviewDialog'] },
  { file: 'model-management-new.html', script: '/studio/js/model-management.js', ids: ['modelPageStatus', 'modelConfigGrid'] }
];

test('all remaining workbenches have isolated preview pages with original integration points', () => {
  for (const page of pages) {
    const html = fs.readFileSync(path.join(staticRoot, page.file), 'utf8');
    assert.match(html, /\/studio\/css\/workbenches-new\.css/);
    assert.match(html, new RegExp(page.script.replace(/[.*+?^${}()|[\]\\]/g, '\\$&')));
    for (const id of page.ids) {
      assert.match(html, new RegExp(`id="${id}"`));
    }
  }
});

test('shared preview stylesheet defines desktop and narrow-screen layout safeguards', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');
  assert.match(css, /\.new-workbench-page/);
  assert.match(css, /@media \(max-width: 960px\)/);
  assert.match(css, /@media \(max-width: 760px\)/);
  assert.match(css, /prefers-reduced-motion/);
});

test('creative workbench pages expose the production-console layout regions without replacing integration ids', () => {
  const read = file => fs.readFileSync(path.join(staticRoot, file), 'utf8');
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(read('outline-new.html'), /class="outline-stage"/);
  assert.match(read('history-list-new.html'), /class="project-list-stage"/);
  assert.match(read('novel-detail-new.html'), /class="reader-workspace"/);
  assert.match(read('novel-detail-new.html'), /class="chapter-rail"/);
  assert.match(read('video-generation-new.html'), /class="video-production-list"/);
  assert.match(read('asset-library-new.html'), /class="asset-search-workspace"/);
  assert.match(read('asset-library-new.html'), /class="[^"]*asset-filter-rail/);
  assert.match(read('model-management-new.html'), /class="model-config-console"/);
  assert.match(css, /\.outline-stage/);
  assert.match(css, /\.reader-workspace/);
  assert.match(css, /\.video-scene-unit/);
  assert.match(css, /\.asset-detail-drawer/);
  assert.match(css, /\.model-test-feedback/);
});

test('creative layouts prevent script content and fixed navigation from forcing horizontal overflow', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');
  const storyboardCss = fs.readFileSync(path.join(staticRoot, 'studio/css/storyboard-workbench-new.css'), 'utf8');

  assert.match(css, /@media \(max-width: ?1480px\)[\s\S]*?\.reader-workspace \.new-reader-grid/);
  assert.match(css, /\.video-production-list \.storyboard-shot-plan\s*\{[^}]*white-space:\s*pre-wrap/);
  assert.match(css, /\.video-production-list \.video-production-stage\s*\{[^}]*min-width:\s*0/);
  assert.match(css, /\.video-production-list img, \.video-production-list video\s*\{[^}]*max-width:\s*100%/);
  assert.match(storyboardCss, /\.storyboard-file-input\s*\{[^}]*width:\s*1px !important/);
});

test('video production line keeps dynamic actions while exposing dedicated canvas and output regions', () => {
  const script = fs.readFileSync(path.join(staticRoot, 'js/video-generation.js'), 'utf8');
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(script, /class="video-scene-status"/);
  assert.match(script, /class="video-primary-canvas"/);
  assert.match(script, /class="video-output-console"/);
  assert.match(script, /data-action="generate-first-frame"/);
  assert.match(script, /data-action="generate-video"/);
  assert.match(script, /card\.open = index === 0/);
  assert.match(css, /\.video-production-list \.video-primary-canvas/);
  assert.match(css, /\.video-production-list \.video-output-console/);
  assert.match(css, /\.new-workbench-page \.new-workbench-empty\[hidden\]\s*\{\s*display:\s*none/);
  assert.match(css, /@media \(max-width: ?760px\)[\s\S]*?\.video-stage-layout \{ grid-template-columns:1fr/);
});

test('video production console groups primary actions on a light operational surface', () => {
  const script = fs.readFileSync(path.join(staticRoot, 'js/video-generation.js'), 'utf8');
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(script, /class="video-frame-actions"/);
  assert.match(script, /class="video-primary-actions"/);
  assert.match(css, /\.video-production-list \.video-control-panel\s*\{[^}]*background:\s*#fffefa/);
  assert.match(css, /\.video-production-list \.video-action-groups\s*\{[^}]*gap:\s*14px/);
});

test('video production console gives the content canvas priority and stacks at narrow widths', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.video-production-list \.video-stage-layout\s*\{[^}]*grid-template-columns:\s*minmax\(0,1fr\) minmax\(280px,\.34fr\)/);
  assert.match(css, /@media \(max-width: ?960px\)[\s\S]*?\.video-production-list \.video-stage-layout\s*\{\s*grid-template-columns:1fr/);
});

test('video production console exposes a quick scene navigator matching the production workflow', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.video-production-list \.video-scene-quick-nav\s*\{[^}]*grid-template-columns/);
  assert.match(css, /\.video-production-list \.video-scene-quick-nav button\.active\s*\{[^}]*background:\s*var\(--new-teal\)/);
});

test('video production console exposes a chapter selector that switches the query-driven workbench', () => {
  const html = fs.readFileSync(path.join(staticRoot, 'video-generation-new.html'), 'utf8');
  const script = fs.readFileSync(path.join(staticRoot, 'js/video-generation.js'), 'utf8');

  assert.match(html, /id="chapterPickerWrap"/);
  assert.match(html, /id="chapterSelect"/);
  assert.match(script, /fetch\(`\/api\/novel\/\$\{encodeURIComponent\(novelId\)\}\/chapters`\)/);
  assert.match(script, /chapterSelect\?\.addEventListener\('change'/);
  assert.match(script, /searchParams\.set\('chapterNum', selected\)/);
});

test('video workbench header keeps chapter selection and return action on one compact desktop row', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.new-video-stage \.new-workbench-header \.card-actions\s*\{[^}]*flex-wrap:\s*nowrap/);
  assert.match(css, /\.new-workbench-page \.video-chapter-picker\s*\{[^}]*display:flex/);
  assert.match(css, /\.new-workbench-page \.video-chapter-picker span\s*\{[^}]*white-space:\s*nowrap/);
  assert.match(css, /@media \(max-width: ?760px\)[\s\S]*?\.new-video-stage \.new-workbench-header \.card-actions/);
});

test('video workbench uses the chapter selector as the primary page heading', () => {
  const html = fs.readFileSync(path.join(staticRoot, 'video-generation-new.html'), 'utf8');
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');
  const script = fs.readFileSync(path.join(staticRoot, 'js/video-generation.js'), 'utf8');

  assert.match(html, /class="video-title-block"[\s\S]*id="chapterPickerWrap"/);
  assert.doesNotMatch(html, /<h1 id="pageTitle">/);
  assert.match(css, /\.video-title-block \.video-chapter-picker select\s*\{[^}]*font-size:\s*clamp\(26px, 3vw, 42px\)/);
  assert.doesNotMatch(script, /pageTitle\.textContent/);
});

test('history workbench keeps project cards compact when overview text is long', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.project-list-stage \.history-list\s*\{[^}]*grid-template-columns:\s*repeat\(auto-fit,? ?minmax\((?:240|280)px/);
  assert.match(css, /\.project-list-stage \.history-card\s*\{[^}]*min-height:\s*0/);
  assert.match(css, /\.project-list-stage \.history-overview\s*\{[^}]*-webkit-line-clamp:\s*4/);
  assert.match(css, /\.project-list-stage \.history-card \.card-actions\s*\{[^}]*margin-top:\s*auto/);
});

test('history workbench uses a portrait project-card proportion like the asset cards', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.project-list-stage \.history-list\s*\{[^}]*grid-template-columns:\s*repeat\(auto-fit,minmax\(240px,280px\)/);
  assert.match(css, /\.project-list-stage \.history-card\s*\{[^}]*aspect-ratio:\s*\.9/);
  assert.match(css, /\.project-list-stage \.history-card\s*\{[^}]*min-height:\s*0/);
  assert.match(css, /\.project-list-stage \.history-list\s*\{[^}]*justify-content:\s*start/);
});

test('video production console keeps one fixed stage host below the quick scene navigator', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.video-production-list \.video-stage-host\s*\{[^}]*min-width:\s*0/);
  assert.match(css, /\.video-production-list \.video-stage-host > \.video-production-stage\s*\{[^}]*margin:\s*0/);
});

test('video reference assets use a compact corner remove affordance', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.video-production-list \.video-reference-card\s*\{[^}]*position:\s*relative/);
  assert.match(css, /\.video-production-list \.video-reference-media\s*\{[^}]*position:\s*relative/);
  assert.match(css, /\.video-production-list \.remove-reference-asset\s*\{[^}]*position:\s*absolute/);
      assert.match(css, /\.video-production-list \.video-reference-card:hover \.remove-reference-asset/);
});

test('asset-library preview keeps its initially hidden dialogs out of view', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.new-workbench-page \.asset-library-modal\[hidden\], \.new-workbench-page \.image-preview-modal\[hidden\]\s*\{\s*display:\s*none/);
});

test('asset-library preview gives the active novel filter a distinct visual state', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.asset-search-workspace \.asset-filter-rail \.novel-filter\.active\s*\{[^}]*background:\s*var\(--new-teal\)/);
  assert.match(css, /\.asset-search-workspace \.asset-filter-rail \.novel-filter\.active\s*\{[^}]*box-shadow:\s*none/);
  assert.match(css, /\.asset-search-workspace \.asset-filter-rail \.novel-filter:not\(\.active\)\s*\{[^}]*background:\s*#fffdf9/);
});

test('novel detail asset images stay contained within narrow cards', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.new-reader-grid \.asset-list\s*\{[^}]*min-width:\s*0/);
  assert.match(css, /\.new-reader-grid \.asset-card\s*\{[^}]*min-width:\s*0/);
  assert.match(css, /\.new-reader-grid \.asset-composite-image\s*\{[^}]*display:\s*block/);
  assert.match(css, /\.new-reader-grid \.asset-composite-image\s*\{[^}]*max-width:\s*100%/);
  assert.match(css, /\.new-reader-grid \.asset-composite-image\s*\{[^}]*height:\s*auto/);
});

test('novel detail asset panel uses a compact responsive card layout', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.new-reader-grid \.asset-section \.new-workbench-toolbar\s*\{[^}]*margin-bottom/);
  assert.match(css, /\.new-reader-grid \.asset-section \.storyboard-actions\s*\{[^}]*display:\s*grid/);
  assert.match(css, /\.new-reader-grid \.asset-section \.asset-filters\s*\{[^}]*margin-top/);
  assert.match(css, /\.new-reader-grid \.asset-card-head\s*\{[^}]*display:\s*grid/);
  assert.match(css, /\.new-reader-grid \.asset-card-actions\s*\{[^}]*display:\s*grid/);
  assert.match(css, /\.new-reader-grid \.storyboard-empty\[hidden\]\s*\{\s*display:\s*none/);
  assert.match(css, /@media \(max-width: ?760px\)[\s\S]*?\.new-reader-grid \.asset-card-actions/);
});

test('novel detail asset prompts have breathing room before composite images', () => {
  const css = fs.readFileSync(path.join(staticRoot, 'studio/css/workbenches-new.css'), 'utf8');

  assert.match(css, /\.new-reader-grid \.asset-card-body textarea\s*\{[^}]*min-height:\s*112px/);
  assert.match(css, /\.new-reader-grid \.asset-composite-image\s*\{[^}]*margin-top:\s*12px/);
});
