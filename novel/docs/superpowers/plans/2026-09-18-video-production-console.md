# 视频制作控制台 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将新版视频创作工作台的分镜展开区改造成脚本、资产、首帧和生成控制分层清晰的制作控制台。

**Architecture:** 保持 `video-generation.js` 的 API 和 `data-action` 合约，通过有限的标记类补充和 `workbenches-new.css` 的局部布局规则实现。测试只断言必须存在的视觉区块和响应式保障，不依赖运行时数据。

**Tech Stack:** 原生 HTML、CSS、JavaScript、Node assert 前端检查、Maven 静态资源构建。

## Global Constraints

- 不修改后端接口、数据库结构、既有元素 ID、`data-action` 或请求参数。
- 不删除旧页面；只修改 `video-generation-new.html` 使用的脚本与共享新版样式。
- 不触发真实生成、上传、删除或保存操作进行验证。
- 当前工作树包含用户其他改动；只编辑本计划明确列出的文件，且不提交。

---

### Task 1: 建立制作控制台结构回归测试

**Files:**
- Modify: `src/test/frontend/workbenches-new-pages.test.js`
- Test: `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: `video-generation.js` 已存在的 `video-scene-workspace`、`video-output-console` 与 `video-control-panel`。
- Produces: 对“首帧操作、视频操作、版本操作”三组控制区以及浅色控制台样式的静态回归保障。

- [x] **Step 1: 写入失败断言**

```js
assert.match(script, /class="video-frame-actions"/);
assert.match(script, /class="video-primary-actions"/);
assert.match(css, /\.video-production-list \.video-control-panel\s*\{[^}]*background:\s*#fffefa/);
assert.match(css, /\.video-production-list \.video-action-groups\s*\{[^}]*gap:\s*14px/);
```

- [x] **Step 2: 运行测试确认失败**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL，提示控制面板尚未使用纸白背景或操作组间距规则不存在。

- [x] **Step 3: 最小实现控制台视觉分组**

在共享样式表中对 `.video-production-list` 下的控制面板、动作组和首帧空状态增加局部选择器；不得改动 JavaScript 的 API 请求或 `data-action`。

- [x] **Step 4: 运行测试确认通过**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: PASS，所有 workbench preview 测试通过。

### Task 2: 实现桌面与窄屏制作布局

**Files:**
- Modify: `src/main/resources/static/studio/css/workbenches-new.css`
- Modify: `src/main/resources/static/video-generation-new.html`
- Test: `src/test/frontend/workbenches-new-pages.test.js`

**Interfaces:**
- Consumes: `#sceneList` 中由 `renderScenes` 动态创建的 `.video-production-stage`。
- Produces: 保持原选择器兼容的两栏桌面布局与单栏窄屏降级布局。

- [x] **Step 1: 写入失败断言**

```js
assert.match(css, /\.video-production-list \.video-stage-layout\s*\{[^}]*grid-template-columns:\s*minmax\(0,1fr\) minmax\(280px,\.34fr\)/);
assert.match(css, /@media \(max-width: ?960px\)[\s\S]*?\.video-production-list \.video-stage-layout\s*\{\s*grid-template-columns:1fr/);
```

- [x] **Step 2: 运行测试确认失败**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: FAIL，提示新的控制台列定义不存在。

- [x] **Step 3: 最小实现布局规则与缓存刷新**

将动态分镜展开体设置为“内容区 + 右侧控制区”；内容区内脚本/资产采用自适应网格，首帧区缩短空状态高度。更新 `video-generation-new.html` 的新版样式缓存参数。

- [x] **Step 4: 运行测试确认通过**

Run: `node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: PASS。

### Task 3: 完整验证

**Files:**
- Verify: `src/test/frontend/video-generation-page.test.js`
- Verify: `src/test/frontend/workbenches-new-pages.test.js`
- Verify: `src/main/resources/static/video-generation-new.html`

**Interfaces:**
- Consumes: 已实现的 CSS 与既有动态渲染脚本。
- Produces: 前端回归、Maven 静态资源与浏览器视觉验证证据。

- [x] **Step 1: 运行针对性前端测试**

Run: `node src/test/frontend/video-generation-page.test.js; node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: 两个命令都通过。

- [x] **Step 2: 构建静态资源**

Run: `mvn resources:resources -DskipTests; mvn package -DskipTests`

Expected: 两个 Maven 命令均以 `BUILD SUCCESS` 结束。

- [x] **Step 3: 浏览器只读验证**

打开 `http://localhost:8081/video-generation-new.html?novelId=54&chapterNum=1`，仅检查首个分镜的脚本、资产、首帧空状态和控制区是否可见；不点击生成、上传、删除或保存按钮。

Expected: 页面无横向溢出、控制区为浅色、控制按钮按分组显示、控制台无错误。

### Task 4: 将分镜列表改为固定舞台切换

**Files:**
- Modify: `src/test/frontend/video-generation-page.test.js`
- Modify: `src/test/frontend/workbenches-new-pages.test.js`
- Modify: `src/main/resources/static/js/video-generation.js`
- Modify: `src/main/resources/static/studio/css/workbenches-new.css`
- Modify: `src/main/resources/static/video-generation-new.html`

**Interfaces:**
- Consumes: `renderScenes(scenes, assetMap)`、全部既有 `data-action` 按钮、`loadFirstFrames` 与 `loadHistory`。
- Produces: `renderSceneQuickNav(quickNav, sceneCards)` 与 `setActiveSceneQuickNav(quickNav, activeSceneId)`；快速导航切换固定的 `#video-stage-host` 舞台内容而不调用 `scrollIntoView`。

- [ ] **Step 1: 写入失败断言**

```js
assert(script.includes('id="video-stage-host"'));
assert(script.includes('function renderActiveScene'));
assert(script.includes('stageHost.replaceChildren(card)'));
assert(!script.includes("entry.card.scrollIntoView"));
```

- [ ] **Step 2: 运行测试确认失败**

Run: `node src/test/frontend/video-generation-page.test.js`

Expected: FAIL，提示固定舞台容器和 `renderActiveScene` 尚不存在。

- [ ] **Step 3: 最小实现单舞台切换**

在 `renderScenes` 中建立一次快速导航和 `#video-stage-host`。将每个动态分镜卡片保存在 `sceneCards`，不直接追加到列表。`renderActiveScene` 以 `stageHost.replaceChildren(card)` 替换当前舞台并激活对应按钮；既有事件监听和 API 操作继续绑定在该卡片上。

- [ ] **Step 4: 运行测试确认通过**

Run: `node src/test/frontend/video-generation-page.test.js; node --test src/test/frontend/workbenches-new-pages.test.js`

Expected: PASS。

- [ ] **Step 5: 浏览器验证**

打开 `http://localhost:8081/video-generation-new.html?novelId=54&chapterNum=1`，点击第二个快速导航按钮。

Expected: 只保留一个 `.video-production-stage`，其标题与快速导航的第二项一致，`window.scrollY` 不因切换而变化，且控制区按钮仍可见。
