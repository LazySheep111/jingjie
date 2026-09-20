# 分镜首帧大图预览 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 让视频生成页面的首帧缩略图支持单击选中、双击查看大图。

**Architecture:** 在页面中放置一个复用的原生 `dialog` 大图预览容器。`video-generation.js` 通过短延迟区分单击与双击：单击提交既有的首帧选择，双击取消待执行的选择并打开预览。

**Tech Stack:** 原生 HTML `dialog`、JavaScript、CSS、Node.js 前端契约测试。

## Global Constraints

- 单击缩略图仍然选择首帧，供视频生成使用。
- 双击缩略图只打开大图，不改变已选首帧。
- 不新增或修改后端接口、数据库表、文件存储逻辑。
- 关闭方式必须包括关闭按钮、点击遮罩和 `Esc`。
- 不提交或同步到 Gitee，除非用户明确要求。

---

### Task 1: 首帧大图预览交互

**Files:**
- Modify: `novel/src/main/resources/static/video-generation.html`
- Modify: `novel/src/main/resources/static/js/video-generation.js:262-301`
- Modify: `novel/src/main/resources/static/css/outline.css:738-777`
- Test: `novel/src/test/frontend/video-generation-page.test.js`

**Interfaces:**
- Consumes: `frame.imagePath`、`frame.version`、现有的 `selectedFirstFrames` 映射。
- Produces: `openFirstFramePreview(frame)`、`closeFirstFramePreview()` 和 `data-action="select-first-frame"` 的单击/双击事件。

- [ ] **Step 1: 写入失败的前端契约测试**

在 `video-generation-page.test.js` 的既有首帧断言之后加入：

```js
assert(html.includes('id="firstFramePreviewDialog"'));
assert(script.includes('function openFirstFramePreview(frame)'));
assert(script.includes('function closeFirstFramePreview()'));
assert(script.includes("button.addEventListener('dblclick'"));
assert(script.includes('previewDialog.showModal()'));
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
node novel/src/test/frontend/video-generation-page.test.js
```

Expected: 失败，并指出 `firstFramePreviewDialog` 或预览函数尚不存在。

- [ ] **Step 3: 添加大图弹窗标记和样式**

在 `video-generation.html` 的 `</main>` 后、脚本前添加：

```html
<dialog id="firstFramePreviewDialog" class="image-preview-dialog" aria-labelledby="firstFramePreviewTitle">
    <div class="image-preview-panel">
        <div class="image-preview-heading">
            <h2 id="firstFramePreviewTitle">分镜首帧预览</h2>
            <button id="closeFirstFramePreview" type="button" class="secondary-button">关闭</button>
        </div>
        <img id="firstFramePreviewImage" alt="分镜首帧大图预览">
    </div>
</dialog>
```

在 `outline.css` 追加 `dialog` 遮罩、最大宽高和 `object-fit: contain` 样式，使图片完整显示在视口内。

- [ ] **Step 4: 实现单击/双击区分与预览控制**

在 `video-generation.js` 顶部状态区域加入 `const firstFrameClickTimers = new Map();`。新增：

```js
function openFirstFramePreview(frame) {
    const previewDialog = document.getElementById('firstFramePreviewDialog');
    const previewImage = document.getElementById('firstFramePreviewImage');
    previewImage.src = frame.imagePath;
    previewImage.alt = `分镜首帧 v${frame.version} 大图预览`;
    previewDialog.showModal();
}

function closeFirstFramePreview() {
    const previewDialog = document.getElementById('firstFramePreviewDialog');
    if (previewDialog.open) previewDialog.close();
}
```

将首帧缩略图监听器改为：单击时保存一个约 220ms 的定时器，定时器触发后执行当前的选中和重绘；双击时清除同一首帧的定时器并调用 `openFirstFramePreview(frame)`。为弹窗关闭按钮和遮罩点击绑定 `closeFirstFramePreview()`；原生 `dialog` 保持 `Esc` 关闭能力。

- [ ] **Step 5: 运行前端测试和语法检查**

Run:

```powershell
node novel/src/test/frontend/video-generation-page.test.js
node --check novel/src/main/resources/static/js/video-generation.js
```

Expected: 输出 `video-generation page checks passed`，且语法检查无输出、退出码为 0。

- [ ] **Step 6: 人工页面验证**

在重启后的 Spring Boot 服务中打开：

```text
http://localhost:8081/video-generation.html?novelId=<小说ID>&chapterNum=<章节号>
```

验证单击缩略图选中、双击打开预览、关闭按钮/遮罩/`Esc` 均关闭预览，且双击未改变原已选首帧。
