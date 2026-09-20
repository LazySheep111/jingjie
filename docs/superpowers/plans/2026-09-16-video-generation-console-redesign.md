# 分镜视频生成控制台视觉改造 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将分镜视频生成页重构为紧凑、清晰的深色制作控制台，同时保持全部既有业务功能。

**Architecture:** 页面结构仍由 `video-generation.js` 的 `renderScenes` 创建，CSS 只作用于视频生成页的专用类。前端静态测试先断言新增的语义分区和原有动作钩子，避免重构破坏请求与事件绑定。

**Tech Stack:** 原生 HTML、CSS、JavaScript、Node 静态断言、Maven。

## Global Constraints

- 不更改 URL、导航文本、页面 ID、`data-action`、`data-role` 或 API 地址。
- 深石墨与暖灰为基础色，琥珀色是唯一高强调色。
- 保持桌面两栏和移动端单栏布局。

---

### Task 1: 为控制台结构添加静态回归断言

**Files:**
- Modify: `novel/src/test/frontend/video-generation-page.test.js`
- Test: `novel/src/test/frontend/video-generation-page.test.js`

- [ ] **Step 1: 写入失败断言**

```js
assert(script.includes('class="video-scene-workspace"'));
assert(script.includes('class="video-control-panel"'));
assert(script.includes('data-role="scene-status"'));
```

- [ ] **Step 2: 运行测试并确认失败**

Run: `node src/test/frontend/video-generation-page.test.js`

Expected: 断言失败，因为控制台专用结构尚未生成。

- [ ] **Step 3: 保留既有动作断言**

```js
assert(script.includes('data-action="generate-video"'));
assert(script.includes('data-action="generate-first-frame"'));
assert(script.includes('data-action="upload-video"'));
```

- [ ] **Step 4: 重跑测试**

Run: `node src/test/frontend/video-generation-page.test.js`

Expected: 结构实现前仍仅因新增结构断言失败。

### Task 2: 重组分镜卡片的语义分区

**Files:**
- Modify: `novel/src/main/resources/static/js/video-generation.js`
- Test: `novel/src/test/frontend/video-generation-page.test.js`

- [ ] **Step 1: 更新 `renderScenes` 标记**

```html
<div class="video-scene-workspace">
  <section class="video-script-panel">...</section>
  <section class="video-assets-panel">...</section>
</div>
<div class="video-control-panel">...</div>
```

- [ ] **Step 2: 让状态节点可被摘要行复用**

```html
<span class="video-task-status" data-role="scene-status" data-role-status>尚未生成</span>
```

- [ ] **Step 3: 运行静态测试**

Run: `node src/test/frontend/video-generation-page.test.js`

Expected: `video-generation page checks passed`。

### Task 3: 实现深色制作台样式

**Files:**
- Modify: `novel/src/main/resources/static/css/outline.css`
- Test: `novel/src/test/frontend/video-generation-page.test.js`

- [ ] **Step 1: 增加视频页局部 token 与区域样式**

```css
.video-page { color: #e9e4da; }
.video-scene-card { background: #20211f; border-color: #45433d; }
.video-control-panel { background: #292a26; border-top: 1px solid #45433d; }
```

- [ ] **Step 2: 添加移动端单栏规则**

```css
@media (max-width: 760px) {
  .video-scene-workspace { grid-template-columns: 1fr; }
  .video-control-panel { align-items: stretch; }
}
```

- [ ] **Step 3: 检查静态结构测试**

Run: `node src/test/frontend/video-generation-page.test.js`

Expected: `video-generation page checks passed`。

### Task 4: 完整验证

**Files:**
- Verify: `novel/src/test/frontend/video-generation-page.test.js`
- Verify: `novel/pom.xml`

- [ ] **Step 1: 运行前端静态测试**

Run: `node src/test/frontend/video-generation-page.test.js`

Expected: `video-generation page checks passed`。

- [ ] **Step 2: 运行后端全量测试**

Run: `mvn test`

Expected: Maven 构建成功，失败与错误均为 0。

- [ ] **Step 3: 检查差异空白错误**

Run: `git diff --check`

Expected: 无输出。
