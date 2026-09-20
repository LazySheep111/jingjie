const assert = require('assert');
const fs = require('fs');
const path = require('path');

const root = path.resolve(__dirname, '../../main/resources');
const html = fs.readFileSync(path.join(root, 'static/storyboard-workbench-new.html'), 'utf8');

assert(html.includes('class="studio-theme studio-workbench-page storyboard-workbench-new-page"'));
assert(html.includes('id="novelImportTitleInput"'));
assert(html.includes('id="novelImportFile"'));
assert(html.includes('id="novelImportBtn"'));
assert(html.includes('id="recentNovelList"'));
assert(html.includes('id="sceneTrack"'));
assert(html.includes('id="sceneEditor"'));
assert(html.includes('id="assetInspector"'));
assert(html.includes('id="videoStatus"'));
assert(html.includes('id="videoResolution"'));
assert(html.includes('data-action="generate-video"'));
assert(html.includes('id="studioInspector"'));
assert(html.includes('/studio/css/storyboard-workbench-new.css'));
assert(html.includes('/studio/js/api.js'));
assert(html.includes('/js/storyboard-workbench.js'));
assert(html.includes('/studio/js/workbench.js'));
assert(html.includes('/studio/js/workbench-shell.js'));
assert(html.includes('class="storyboard-cut-layout"'));
assert(html.includes('storyboard-current-shot'));
assert(html.includes('storyboard-output-rail'));

const css = fs.readFileSync(path.join(root, 'static/studio/css/storyboard-workbench-new.css'), 'utf8');
assert(css.includes('.storyboard-cut-layout'));

console.log('storyboard workbench preview checks passed');
