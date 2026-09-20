# 视频分辨率选择 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为每个分镜视频增加分辨率选择，将选择传给视频模型，并在视频历史记录中保存和展示实际分辨率。

**Architecture:** 前端在每个分镜卡片保存独立的 resolution 值，创建任务请求携带该值。后端在服务层校验并快照分辨率，客户端按 provider/model 能力构造模型请求；数据库为视频记录增加可空 resolution 字段以兼容旧数据。

**Tech Stack:** Spring Boot 5、MyBatis 注解 SQL、MySQL、原生 HTML/CSS/JavaScript、Maven/JUnit、Node 前端静态检查。

## Global Constraints

- 默认分辨率必须是 `768P`。
- 页面选项统一为 `720P`、`768P`、`1080P`、`2K`。
- MiniMax-H3 只允许向模型发送 `768P` 或 `2K`。
- 非法或缺失分辨率回退为 `768P`。
- 不执行 git commit、push 或 Gitee 同步。
- 保留现有工作区中与本功能无关的修改。

## File Map

- Modify: `novel/src/main/java/com/novelgeneration/novel/dto/VideoProviderRequest.java`，增加 provider 请求分辨率。
- Modify: `novel/src/main/java/com/novelgeneration/novel/dto/VideoGenerationRequest.java`，接收前端分辨率。
- Modify: `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardVideoVO.java`，返回历史分辨率。
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardVideoMapper.java`，读写数据库分辨率。
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardGenerationPreparationServiceImpl.java`，把任务请求中的分辨率带入 provider request。
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImpl.java`，校验、快照和保存分辨率。
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClient.java`，按模型能力构造 resolution 字段。
- Modify: `novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java`，接收并传递请求字段。
- Modify: `novel/src/main/resources/static/js/video-generation.js`，增加每个分镜的下拉框、提交字段和历史显示。
- Modify: `novel/src/main/resources/static/css/video-generation.css`，增加分辨率控件样式。
- Create: `novel/src/main/resources/sql/alter_storyboard_video_add_resolution.sql`，数据库迁移脚本。
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClientTest.java`。
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImplTest.java`。
- Test: `novel/src/test/java/com/novelgeneration/novel/mapper/VideoGenerationMapperParameterTest.java`。
- Test: `novel/src/test/frontend/video-generation-page.test.js`。

### Task 1: Provider request and database contract

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/dto/VideoProviderRequest.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/dto/VideoGenerationRequest.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardVideoVO.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardVideoMapper.java`
- Create: `novel/src/main/resources/sql/alter_storyboard_video_add_resolution.sql`
- Test: `novel/src/test/java/com/novelgeneration/novel/mapper/VideoGenerationMapperParameterTest.java`

**Interfaces:** `resolution` is a nullable String at persistence boundaries and normalized to a supported value before provider submission.

- [ ] **Step 1: Write failing mapper and DTO assertions**

```java
assertTrue(StoryboardVideoMapper.class.getDeclaredMethods()[0].toString().contains("resolution"));
assertEquals("768P", new VideoProviderRequest().getResolution());
```

- [ ] **Step 2: Run the focused test and verify failure**

Run: `mvn -f novel/pom.xml -Dtest=VideoGenerationMapperParameterTest test`

Expected: FAIL because the new field and SQL projection are absent.

- [ ] **Step 3: Add the nullable schema column and Java fields**

```sql
ALTER TABLE novel_storyboard_video
    ADD COLUMN resolution VARCHAR(16) NULL AFTER duration_sec;
```

Add `private String resolution;` with getter/setter to the DTO and VO. Add `resolution AS resolution` to the mapper select and `#{resolution}` to the insert.

- [ ] **Step 4: Run the focused test and verify it passes**

Run: `mvn -f novel/pom.xml -Dtest=VideoGenerationMapperParameterTest test`

Expected: PASS.

### Task 2: Backend normalization and provider payload

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardGenerationPreparationServiceImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClient.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClientTest.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImplTest.java`

**Interfaces:** `VideoProviderRequest.resolution` flows from `VideoGenerationRequest.resolution`; MiniMax-H3 accepts only `768P` and `2K`; fallback is `768P`.

- [ ] **Step 1: Add failing tests for MiniMax payload values**

```java
request.setResolution("2K");
Map<String, Object> body = invokeMiniMaxBody(request);
assertEquals("2K", body.get("resolution"));
```

Also add an invalid-value test expecting `768P` and a H3 unsupported-value test expecting the safe fallback.

- [ ] **Step 2: Run focused tests and verify failure**

Run: `mvn -f novel/pom.xml -Dtest=ConfiguredVideoGenerationClientTest,VideoGenerationServiceImplTest test`

Expected: FAIL because the client currently hard-codes `2K`.

- [ ] **Step 3: Implement normalization and request propagation**

Use a single normalization method:

```java
private String normalizeResolution(String value) {
    if (value == null) return "768P";
    String normalized = value.trim().toUpperCase(Locale.ROOT);
    return Set.of("720P", "768P", "1080P", "2K").contains(normalized)
            ? normalized : "768P";
}
```

Set the normalized value on the provider request before task creation. In `miniMaxBody`, use the normalized value and restrict `MiniMax-H3` to `768P`/`2K`; retain the existing duration and ratio rules.

- [ ] **Step 4: Save the normalized value into the video snapshot**

When creating the `StoryboardVideoVO`, assign the same normalized value used for the provider request. This prevents history from showing a value different from the submitted payload.

- [ ] **Step 5: Run focused tests and verify they pass**

Run: `mvn -f novel/pom.xml -Dtest=ConfiguredVideoGenerationClientTest,VideoGenerationServiceImplTest test`

Expected: PASS.

### Task 3: Controller and frontend selection

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java`
- Modify: `novel/src/main/resources/static/js/video-generation.js`
- Modify: `novel/src/main/resources/static/css/video-generation.css`
- Test: `novel/src/test/frontend/video-generation-page.test.js`
- Test: `novel/src/test/java/com/novelgeneration/novel/controller/VideoGenerationControllerTest.java`

**Interfaces:** Each scene card renders `data-field="resolution"`, defaults to `768P`, and the POST body includes `resolution`.

- [ ] **Step 1: Add failing frontend assertions**

```js
assert(script.includes('data-field="resolution"'));
assert(script.includes("resolution: card.querySelector('[data-field=\\\"resolution\\\"]')"));
assert(script.includes('768P'));
```

- [ ] **Step 2: Run the frontend test and verify failure**

Run: `node novel/src/test/frontend/video-generation-page.test.js`

Expected: FAIL because the page has no resolution selector or request field.

- [ ] **Step 3: Render the selector in each scene card**

Use a native select with these options and `768P` selected by default:

```html
<label class="video-resolution-field">视频分辨率
  <select data-field="resolution">
    <option value="720P">720P</option>
    <option value="768P" selected>768P</option>
    <option value="1080P">1080P</option>
    <option value="2K">2K</option>
  </select>
</label>
```

Keep the selected value in the scene card state when rerendering status updates.

- [ ] **Step 4: Add resolution to the generation request and history display**

Read the selected value when the scene button is clicked, send it in the JSON request, and render `video.resolution || '未知分辨率'` beside version and duration.

- [ ] **Step 5: Add concise styling and run frontend tests**

Style the control consistently with the existing duration field, then run:

`node novel/src/test/frontend/video-generation-page.test.js`

Expected: PASS.

### Task 4: End-to-end verification

**Files:**
- Modify only if a focused test exposes a contract mismatch.

- [ ] **Step 1: Run the backend test suite**

Run: `mvn -f novel/pom.xml test`

Expected: all existing and new tests PASS.

- [ ] **Step 2: Run all frontend static checks**

Run: `Get-ChildItem novel/src/test/frontend/*.test.js | ForEach-Object { node $_.FullName }`

Expected: every frontend check reports PASS.

- [ ] **Step 3: Verify the migration SQL is safe to run once**

Run: `Get-Content novel/src/main/resources/sql/alter_storyboard_video_add_resolution.sql`

Confirm it targets only `novel_storyboard_video` and does not delete or rewrite existing rows.

- [ ] **Step 4: Report the required runtime action**

Tell the user to execute the migration once and restart Spring Boot before testing. Do not execute the database script or synchronize to Gitee without a separate explicit request.
