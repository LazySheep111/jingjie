# 分镜视频生成 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 为当前章节的每个分镜增加独立的视频生成任务、视频预览、重试和历史版本能力，并把分镜关联的人物/场景三视图与全书视觉风格一起提交给视频模型。

**Architecture:** 在现有分镜和视觉资产服务旁新增视频任务域。`VideoGenerationService` 负责校验、建任务、读取分镜/资产/视觉风格快照和持久化结果；`VideoGenerationClient` 负责隔离具体视频模型协议；异步执行器负责调用模型、下载视频并更新状态。前端增加当前章节视频页面，每个分镜单独轮询自己的任务。

**Tech Stack:** Spring Boot 2.6.13, Spring MVC, MyBatis 3.5.9, MySQL, Jackson, Java `ExecutorService`, 原生 HTML/CSS/JavaScript。

## Global Constraints

- 只处理当前章节，不实现整章一键生成或自动拼接。
- 每个分镜独立创建任务、独立生成、独立重试。
- 缺少关联资产三视图时不得调用视频模型。
- 每张参考三视图独立传入；模型不支持多图时直接失败，不合并、不截取第一张。
- 每次成功生成创建新视频版本，旧版本保留。
- 视频文件保存到 `novel/uploads/videos/`，数据库只保存相对路径。
- 自动把全书视觉风格加入视频提示词，并保存提示词/风格快照。
- 用户没有要求时不提交、不推送 Gitee。
- 所有改动使用 TDD：先添加针对行为的失败测试，再实现最小代码，再运行测试。

## File Map

**Create:**

- `novel/src/main/resources/sql/create_video_generation_tables.sql`：视频任务、视频版本、视频资产快照表。
- `novel/src/main/java/com/novelgeneration/novel/vo/VideoGenerationTaskVO.java`：任务查询结果。
- `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardVideoVO.java`：视频版本和资产快照。
- `novel/src/main/java/com/novelgeneration/novel/dto/VideoGenerationRequest.java`：分镜视频任务请求。
- `novel/src/main/java/com/novelgeneration/novel/dto/VideoProviderRequest.java`：模型适配器的规范化请求。
- `novel/src/main/java/com/novelgeneration/novel/service/VideoGenerationService.java`：视频任务服务接口。
- `novel/src/main/java/com/novelgeneration/novel/service/VideoGenerationClient.java`：视频模型客户端接口。
- `novel/src/main/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImpl.java`：任务校验、异步执行、下载、版本保存。
- `novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClient.java`：基于配置的 HTTP 模型实现。
- `novel/src/main/java/com/novelgeneration/novel/mapper/VideoGenerationTaskMapper.java`：任务 SQL 映射。
- `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardVideoMapper.java`：视频版本 SQL 映射。
- `novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java`：任务、轮询、历史版本、当前版本接口。
- `novel/src/main/resources/static/video-generation.html`：当前章节视频页面。
- `novel/src/main/resources/static/js/video-generation.js`：页面加载、独立生成、轮询、历史版本交互。
- `novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImplTest.java`：服务校验、版本和失败场景测试。
- `novel/src/test/java/com/novelgeneration/novel/controller/VideoGenerationControllerTest.java`：接口请求/响应测试。
- `novel/src/test/frontend/video-generation-page.test.js`：页面结构和关键文案检查。

**Modify:**

- `novel/src/main/resources/static/novel-detail.html`：分镜操作区增加“前往视频生成”。
- `novel/src/main/resources/static/js/novel-detail.js`：入口按钮、未保存弹窗、带 `novelId/chapterNum` 跳转。
- `novel/src/main/resources/static/css/outline.css`：入口按钮和视频页面复用样式。
- `novel/src/main/resources/application.properties`：视频模型 endpoint、key、model、下载目录、轮询和超时配置。

---

### Task 1: 创建视频数据库结构和领域对象

**Files:**
- Create: `novel/src/main/resources/sql/create_video_generation_tables.sql`
- Create: `novel/src/main/java/com/novelgeneration/novel/vo/VideoGenerationTaskVO.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardVideoVO.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/dto/VideoGenerationRequest.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/vo/VideoGenerationModelTest.java`

**Interfaces:**
- `VideoGenerationRequest.sceneId: Long` 是创建任务的唯一请求字段。
- `VideoGenerationTaskVO` 至少暴露 `taskId`, `novelId`, `chapterNum`, `sceneId`, `status`, `providerTaskId`, `videoId`, `videoUrl`, `errorMessage`。
- `StoryboardVideoVO` 暴露 `id`, `sceneId`, `version`, `videoPath`, `durationSec`, `promptSnapshot`, `styleSnapshot`, `isCurrent`, `createdAt`, `assets`。

- [ ] **Step 1: Write the failing model test**

```java
@Test
void requestContainsOnlySceneId() {
    VideoGenerationRequest request = new VideoGenerationRequest();
    request.setSceneId(7L);
    assertEquals(Long.valueOf(7L), request.getSceneId());
}
```

- [ ] **Step 2: Run the test and verify it fails**

Run: `mvn -q -Dtest=VideoGenerationModelTest test`
Expected: FAIL because the request/VO classes do not exist.

- [ ] **Step 3: Add the DTOs, VOs and SQL**

Create the three tables with:

```sql
CREATE TABLE video_generation_task (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  novel_id BIGINT NOT NULL,
  chapter_num BIGINT NOT NULL,
  storyboard_scene_id BIGINT NOT NULL,
  status VARCHAR(32) NOT NULL,
  provider_task_id VARCHAR(128),
  error_message TEXT,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  INDEX idx_video_task_scene (novel_id, chapter_num, storyboard_scene_id),
  INDEX idx_video_task_status (status)
);

CREATE TABLE storyboard_video (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  novel_id BIGINT NOT NULL,
  chapter_num BIGINT NOT NULL,
  storyboard_scene_id BIGINT NOT NULL,
  version INT NOT NULL,
  video_path VARCHAR(1000) NOT NULL,
  duration_sec INT,
  prompt_snapshot LONGTEXT,
  style_snapshot LONGTEXT,
  is_current TINYINT(1) NOT NULL DEFAULT 1,
  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  UNIQUE KEY uk_storyboard_video_version (storyboard_scene_id, version),
  INDEX idx_storyboard_video_scene (novel_id, chapter_num, storyboard_scene_id)
);

CREATE TABLE storyboard_video_asset (
  video_id BIGINT NOT NULL,
  asset_id BIGINT NOT NULL,
  asset_version INT NOT NULL,
  asset_name VARCHAR(255) NOT NULL,
  asset_type VARCHAR(32) NOT NULL,
  image_path VARCHAR(1000) NOT NULL,
  PRIMARY KEY (video_id, asset_id),
  CONSTRAINT fk_video_asset_video FOREIGN KEY (video_id) REFERENCES storyboard_video(id)
);
```

- [ ] **Step 4: Run the test and verify it passes**

Run: `mvn -q -Dtest=VideoGenerationModelTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add novel/src/main/resources/sql/create_video_generation_tables.sql novel/src/main/java/com/novelgeneration/novel/vo novel/src/main/java/com/novelgeneration/novel/dto/VideoGenerationRequest.java novel/src/test/java/com/novelgeneration/novel/vo/VideoGenerationModelTest.java
git commit -m "feat: add video generation data model"
```

### Task 2: Add MyBatis persistence for tasks and video versions

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/mapper/VideoGenerationTaskMapper.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardVideoMapper.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/mapper/VideoGenerationMapperParameterTest.java`

**Interfaces:**
- `createTask(VideoGenerationTaskVO task)` returns generated `id`.
- `updateStatus(Long taskId, String status, String providerTaskId, String errorMessage)`.
- `selectTask(Long taskId)` returns one task.
- `insertVideo(StoryboardVideoVO video)` returns generated `id`.
- `clearCurrent(Long sceneId)` sets `is_current=0`.
- `selectNextVersion(Long sceneId)` returns `max(version)+1`, defaulting to `1`.
- `insertVideoAsset(StoryboardVideoVO.AssetSnapshot asset)` inserts one snapshot.
- `selectByScene(Long novelId, Long chapterNum, Long sceneId)` returns current and historical versions ordered by version descending.
- `setCurrent(Long videoId)` clears the scene's other current flags and marks the requested version current in one transaction.

- [ ] **Step 1: Write parameter-name tests**

Use MyBatis mapper reflection or the existing mapper test pattern to assert every multi-argument method has `@Param` names matching the SQL placeholders: `taskId`, `status`, `providerTaskId`, `errorMessage`, `sceneId`, `novelId`, `chapterNum`, and `videoId`.

- [ ] **Step 2: Run and verify the tests fail**

Run: `mvn -q -Dtest=VideoGenerationMapperParameterTest test`
Expected: FAIL because the mappers and XML statements do not exist.

- [ ] **Step 3: Implement mapper interfaces and XML statements**

Use `@Param` on every argument and match existing MyBatis XML registration. Never reference an unbound name such as `version` when the Java method only exposes `arg0/arg1`.

- [ ] **Step 4: Run and verify the tests pass**

Run: `mvn -q -Dtest=VideoGenerationMapperParameterTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add novel/src/main/java/com/novelgeneration/novel/mapper novel/src/test/java/com/novelgeneration/novel/mapper
git commit -m "feat: persist video tasks and versions"
```

### Task 3: Define the provider adapter and configuration

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/dto/VideoProviderRequest.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/VideoGenerationClient.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClient.java`
- Modify: `novel/src/main/resources/application.properties`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClientTest.java`

**Interfaces:**

```java
public interface VideoGenerationClient {
    ProviderTask submit(VideoProviderRequest request);
    ProviderTaskStatus query(String providerTaskId);
}
```

`VideoProviderRequest` contains `prompt`, `durationSec`, `aspectRatio`, `stylePrompt`, and `List<ReferenceImage>` where each reference has `assetId`, `assetName`, `assetType`, and `imagePath`.

Configuration keys:

```properties
video.generation.enabled=false
video.generation.endpoint=
video.generation.api-key=
video.generation.model=
video.generation.download-dir=uploads/videos
video.generation.poll-interval-ms=2000
video.generation.timeout-ms=1800000
```

- [ ] **Step 1: Write client tests**

Test that the normalized request preserves every reference image as a separate item and that a provider response is mapped to `providerTaskId`, `status`, `videoUrl`, and `errorMessage`.

- [ ] **Step 2: Run and verify failure**

Run: `mvn -q -Dtest=ConfiguredVideoGenerationClientTest test`
Expected: FAIL because the client interface and implementation do not exist.

- [ ] **Step 3: Implement the adapter**

Use `RestTemplate` or the project's existing HTTP utility. Build headers from `video.generation.api-key`. Do not log API keys or image bytes. If the provider response explicitly reports that multiple images are unsupported, throw a typed exception that the service converts to `FAILED` without fallback merging.

- [ ] **Step 4: Run and verify pass**

Run: `mvn -q -Dtest=ConfiguredVideoGenerationClientTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add novel/src/main/java/com/novelgeneration/novel/dto/VideoProviderRequest.java novel/src/main/java/com/novelgeneration/novel/service novel/src/main/resources/application.properties novel/src/test/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClientTest.java
git commit -m "feat: add configurable video provider adapter"
```

### Task 4: Implement asynchronous video generation service

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/service/VideoGenerationService.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImpl.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImplTest.java`

**Interfaces:**

```java
VideoGenerationTaskVO createTask(Long novelId, Long chapterNum, Long sceneId);
VideoGenerationTaskVO getTask(Long taskId);
List<StoryboardVideoVO> listVideos(Long novelId, Long chapterNum, Long sceneId);
VideoGenerationTaskVO retry(Long taskId);
void setCurrent(Long videoId);
```

- [ ] **Step 1: Write service tests**

Cover these cases:

```java
missingSceneDoesNotCreateTask();
missingCompositeImageFailsBeforeProviderCall();
providerDoesNotSupportMultipleImagesMarksTaskFailed();
successfulProviderResultDownloadsAndCreatesVersionOne();
retryCreatesVersionTwoAndKeepsVersionOne();
failedDownloadDoesNotCreateSuccessfulVideo();
setCurrentOnlyChangesVersionFlags();
```

- [ ] **Step 2: Run and verify failure**

Run: `mvn -q -Dtest=VideoGenerationServiceImplTest test`
Expected: FAIL because the service does not exist.

- [ ] **Step 3: Implement task creation and validation**

Read the current chapter storyboard through `StoryboardMapper`, resolve `sceneId`, collect `characterAssetIds` and `locationAssetIds` through `VisualAssetMapper`, and read the style through `VisualStyleService`. Reject missing `compositeImagePath` before inserting a provider task. Save the full prompt/style/asset snapshot only after the model returns a successful video.

- [ ] **Step 4: Implement asynchronous execution**

Use a bounded executor with two worker threads. Insert `QUEUED`, submit the work, set `RUNNING`, call `VideoGenerationClient.submit`, poll `query` until success/failure/timeout, download the returned URL to the configured directory, and then insert a new version. Update the task to `SUCCESS` only after the file and database record exist.

- [ ] **Step 5: Run and verify pass**

Run: `mvn -q -Dtest=VideoGenerationServiceImplTest test`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add novel/src/main/java/com/novelgeneration/novel/service novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImplTest.java
git commit -m "feat: generate storyboard videos asynchronously"
```

### Task 5: Add REST endpoints

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/controller/VideoGenerationControllerTest.java`

**Interfaces:**

```http
POST /api/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/video-tasks
GET  /api/video-tasks/{taskId}
GET  /api/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/videos
POST /api/video-tasks/{taskId}/retry
PUT  /api/storyboard-videos/{videoId}/current
```

- [ ] **Step 1: Write controller tests**

Assert the create endpoint returns `taskId/status`, the status endpoint returns `videoUrl` on success, the history endpoint returns versions newest first, and validation failures return an error response without a provider call.

- [ ] **Step 2: Run and verify failure**

Run: `mvn -q -Dtest=VideoGenerationControllerTest test`
Expected: FAIL because the controller does not exist.

- [ ] **Step 3: Implement the controller**

Use `Result.ok(...)` to match existing controllers. Validate path values, pass `novelId/chapterNum/sceneId` to the service, and map typed validation failures to HTTP 400 with a Chinese-readable message.

- [ ] **Step 4: Run and verify pass**

Run: `mvn -q -Dtest=VideoGenerationControllerTest test`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java novel/src/test/java/com/novelgeneration/novel/controller/VideoGenerationControllerTest.java
git commit -m "feat: expose storyboard video APIs"
```

### Task 6: Add the current-chapter video page

**Files:**
- Create: `novel/src/main/resources/static/video-generation.html`
- Create: `novel/src/main/resources/static/js/video-generation.js`
- Modify: `novel/src/main/resources/static/css/outline.css`
- Test: `novel/src/test/frontend/video-generation-page.test.js`

**Interfaces:**
- Read `novelId` and `chapterNum` from `URLSearchParams`.
- Render one collapsible card per storyboard scene.
- Call the Task 5 endpoints using the exact path values.

- [ ] **Step 1: Write the frontend test**

Assert the page contains the current-chapter title, `videoSceneList`, video preview element, “生成视频”, “重新生成视频”, “历史版本”, and the task/status URL fragments.

- [ ] **Step 2: Run and verify failure**

Run: `node -e "require('./src/test/frontend/video-generation-page.test.js')"`
Expected: FAIL because the page and script do not exist.

- [ ] **Step 3: Implement the page shell**

Create a page with a header containing “返回分镜编辑”和“刷新状态”, then a collapsible card per scene. Each card must show the script fields, associated reference assets, a video element when `videoUrl` exists, the current task status, a per-scene “生成视频” button, “下载视频”, and an expandable history list.

- [ ] **Step 4: Implement page behavior**

On load, fetch the current chapter storyboard and each scene's versions. On click, call the create-task endpoint, disable only that scene's button, poll every 2 seconds, stop on `SUCCESS` or `FAILED`, and re-enable only that card. On retry, call the retry endpoint. On history selection, call the current-version endpoint and refresh that card.

- [ ] **Step 5: Run and verify pass**

Run: `node -e "require('./src/test/frontend/video-generation-page.test.js')"`
Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add novel/src/main/resources/static/video-generation.html novel/src/main/resources/static/js/video-generation.js novel/src/main/resources/static/css/outline.css novel/src/test/frontend/video-generation-page.test.js
git commit -m "feat: add storyboard video generation page"
```

### Task 7: Add the storyboard entry button and navigation guard

**Files:**
- Modify: `novel/src/main/resources/static/novel-detail.html`
- Modify: `novel/src/main/resources/static/js/novel-detail.js`
- Test: `novel/src/test/frontend/novel-detail-page.test.js`

- [ ] **Step 1: Write the failing test**

Assert the page contains `goVideoGenerationBtn` and the script contains `video-generation.html`, `novelId`, `chapterNum`, and the three unsaved-change actions.

- [ ] **Step 2: Run and verify failure**

Run: `node -e "require('./src/test/frontend/novel-detail-page.test.js')"`
Expected: FAIL because the entry button and handler do not exist.

- [ ] **Step 3: Add the button and guard**

Place the button in the storyboard action row after “保存分镜修改”. Disable it when there is no active storyboard. Before navigation, compare the editable scene fields with `activeStoryboard.scenes`; if changed, show the three-button choice. Save first using the existing update flow when the user chooses “保存并前往视频生成”.

- [ ] **Step 4: Run and verify pass**

Run: `node -e "require('./src/test/frontend/novel-detail-page.test.js')"`
Expected: PASS.

- [ ] **Step 5: Commit**

```bash
git add novel/src/main/resources/static/novel-detail.html novel/src/main/resources/static/js/novel-detail.js novel/src/test/frontend/novel-detail-page.test.js
git commit -m "feat: link storyboard editing to video generation"
```

### Task 8: Verify integration and local file serving

**Files:**
- Modify: `novel/src/main/resources/application.properties` only if the final configured upload mapping is missing.
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationIntegrationTest.java`

- [ ] **Step 1: Add integration checks**

Use mocks for the provider and filesystem to verify: one scene creates one task, success creates a local `.mp4` path, refresh returns the current version, and a second generation creates version `2` without deleting version `1`.

- [ ] **Step 2: Run targeted tests**

Run:

```bash
mvn -q -Dtest=VideoGenerationModelTest,VideoGenerationMapperParameterTest,ConfiguredVideoGenerationClientTest,VideoGenerationServiceImplTest,VideoGenerationControllerTest,VideoGenerationIntegrationTest test
node -e "require('./src/test/frontend/novel-detail-page.test.js'); require('./src/test/frontend/video-generation-page.test.js')"
```

Expected: all Java tests pass and both frontend checks print their success messages.

- [ ] **Step 3: Run formatting and diff checks**

Run: `git diff --check`
Expected: exit code `0`. Existing line-ending warnings are acceptable; whitespace errors are not.

- [ ] **Step 4: Manual browser acceptance test**

1. Open a chapter with saved storyboard scenes.
2. Click “前往视频生成”.
3. Confirm only the current chapter appears.
4. Click “生成视频” on scene 1 and confirm only scene 1 enters `QUEUED/RUNNING`.
5. Confirm scene 2 remains untouched.
6. Mock a successful provider response and confirm a playable local URL appears.
7. Generate scene 1 again and confirm history contains v1 and v2.
8. Remove one asset image and confirm the provider is not called and the missing asset name is shown.
9. Confirm an unsupported multi-image provider response becomes `FAILED` with a retry action.

- [ ] **Step 5: Commit verification changes**

```bash
git add novel/src/test/java novel/src/test/frontend
git commit -m "test: verify storyboard video generation flow"
```

## Execution Notes

- Run the SQL script manually against the configured MySQL database before starting the backend task tests that require the new tables.
- Keep provider credentials in local `application.properties` or environment-specific configuration; never commit real keys.
- Do not add generated `.mp4` files under Git. Add `novel/uploads/videos/` to the existing ignore rules if it is not already ignored.
- Do not push to Gitee unless the user explicitly requests synchronization.
