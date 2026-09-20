# 影视分镜时间轴格式 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将分镜脚本展示、视频页脚本和 TXT 导出统一为按时长自动累计的影视分镜时间轴格式。

**Architecture:** 保留现有 JSON 和数据库字段，AI 继续输出结构化分镜字段；前端与后端导出层共同使用确定性的累计时码格式化逻辑。这样用户修改 `durationSec` 后无需重新调用 AI，后续时间码会重新计算。

**Tech Stack:** Spring Boot、MyBatis、原生 JavaScript、现有 JUnit 和 Node 静态检查。

## Global Constraints

- 时间从 `0:00` 开始，后一个分镜的开始时间等于前一个分镜的结束时间。
- 时间码必须根据 `durationSec` 自动计算，不由 AI 填写。
- 保留结构化 JSON、字段编辑、资产关联和视频生成能力。
- 不新增数据库字段，不执行 git commit、push 或 Gitee 同步。

## Task 1: AI prompt and server TXT export

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardServiceImpl.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/StoryboardServiceImplTest.java`

- [ ] **Step 1: Add failing assertions for prompt and export**

Assert the generation prompt mentions timeline-friendly fields and assert export contains `0:00 - 0:05 |` for scenes with durations 5 and 10 seconds.

- [ ] **Step 2: Run the focused test and verify it fails**

Run: `mvn -f novel/pom.xml -Dtest=StoryboardServiceImplTest test`

- [ ] **Step 3: Update the prompt and export formatter**

Add explicit instructions that the model must describe one executable shot per scene and preserve `durationSec`, `shotType`, `cameraMovement`, `visualDescription`, `characterAction`, `dialogue`, `characters`, and `location`. Add `formatTime(int seconds)` and accumulate duration while exporting:

```java
int start = 0;
for (StoryboardVO.Scene scene : storyboard.getScenes()) {
    int duration = scene.getDurationSec() == null ? 0 : scene.getDurationSec();
    text.append(formatTime(start)).append(" - ")
        .append(formatTime(start + duration)).append(" | ")
        .append(value(scene.getShotType()));
    start += duration;
}
```

- [ ] **Step 4: Run the focused test and verify it passes**

Run: `mvn -f novel/pom.xml -Dtest=StoryboardServiceImplTest test`

### Task 2: Shared browser timeline formatting

**Files:**
- Modify: `novel/src/main/resources/static/js/novel-detail.js`
- Modify: `novel/src/main/resources/static/js/video-generation.js`
- Test: `novel/src/test/frontend/novel-detail-page.test.js`
- Test: `novel/src/test/frontend/video-generation-page.test.js`

- [ ] **Step 1: Add failing static assertions**

Assert the scripts contain `formatStoryboardTime`, `0:00`, and the timeline separator `|`.

- [ ] **Step 2: Run frontend checks and verify failure**

Run: `node novel/src/test/frontend/novel-detail-page.test.js` and `node novel/src/test/frontend/video-generation-page.test.js`.

- [ ] **Step 3: Add deterministic time helpers**

Implement equivalent helpers in each existing standalone script:

```js
function formatStoryboardTime(seconds) {
    const total = Math.max(0, Number(seconds) || 0);
    const minutes = Math.floor(total / 60);
    const remainder = String(total % 60).padStart(2, '0');
    return `${minutes}:${remainder}`;
}

function storyboardTimeline(scenes) {
    let start = 0;
    return scenes.map(scene => {
        const duration = Number(scene.durationSec) || 0;
        const item = {start, end: start + duration};
        start += duration;
        return item;
    });
}
```

- [ ] **Step 4: Render the timeline line and separate dialogue**

For each card, render:

```text
0:00 - 0:05 | 全景 | 画面描述
台词（人物，情绪）：对白
```

Use `shotType` plus `cameraMovement` as `shotType -> cameraMovement` when both exist. Keep existing editable textareas below the formatted preview.

- [ ] **Step 5: Run frontend checks and verify they pass**

Run all frontend tests with the bundled Node runtime.

### Task 3: Full verification

**Files:**
- Modify only if a focused test identifies a contract mismatch.

- [ ] **Step 1: Run backend tests**

Run: `mvn -f novel/pom.xml test`

- [ ] **Step 2: Run all frontend tests**

Run: `Get-ChildItem novel/src/test/frontend/*.test.js | ForEach-Object { node $_.FullName }`

- [ ] **Step 3: Check formatting and report runtime actions**

Run `git diff --check`; tell the user to restart Spring Boot and refresh the page. Do not execute database or Gitee operations for this feature.
