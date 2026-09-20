# 独立分镜首帧 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 将分镜首帧拆成可独立生成、保留、选择和删除的资源，并使视频任务显式引用用户选择的首帧。

**Architecture:** `storyboard_first_frame` 既是首帧候选记录也是异步首帧任务记录。一个共享的生成上下文服务负责从分镜、视觉风格和资产库构建首帧请求、视频请求与资产快照，首帧服务和视频服务分别消费它。视频任务创建时接收并快照 `firstFrameId`，从而重试不再生成首帧。

**Tech Stack:** Spring Boot 2.6、MyBatis 注解 Mapper、MySQL、Jackson、原生 HTML/CSS/JavaScript、JUnit 5、Mockito、Node 静态页面检查。

## Global Constraints

- 每次首帧任务只调用一次既有 `StoryboardFrameGenerator.generate`。
- 每个分镜的首帧任务彼此互斥，视频任务彼此互斥；两类任务可并行。
- 首帧选择仅存在浏览器当前页面，不创建“当前首帧”字段。
- 删除首帧为逻辑删除，不能删除本地图片文件或影响已生成视频。
- 视频创建、视频重试和已生成视频必须使用任务中快照的首帧路径。
- 不修改 MiniMax、Seedream、人物/场景资产或既有视频历史的协议。
- 不执行 Git 提交或 Gitee 同步，除非用户明确要求。

---

## File Structure

- Create: `novel/src/main/resources/sql/create_storyboard_first_frame_tables.sql` — 新表及视频任务迁移脚本。
- Create: `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardFirstFrameVO.java` — 首帧任务/候选记录。
- Create: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardFirstFrameMapper.java` — 首帧查询、写入、状态及逻辑删除。
- Create: `novel/src/main/java/com/novelgeneration/novel/dto/StoryboardGenerationContext.java` — 首帧和视频共用的准备结果。
- Create: `novel/src/main/java/com/novelgeneration/novel/service/StoryboardGenerationPreparationService.java` — 构建分镜生成上下文。
- Create: `novel/src/main/java/com/novelgeneration/novel/service/StoryboardFirstFrameService.java` — 首帧任务服务契约。
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardGenerationPreparationServiceImpl.java` — 加载分镜、资产与视觉风格并构建请求。
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardFirstFrameServiceImpl.java` — 首帧异步生成和状态维护。
- Modify: `novel/src/main/java/com/novelgeneration/novel/dto/VideoGenerationRequest.java` — 加入 `firstFrameId`。
- Modify: `novel/src/main/java/com/novelgeneration/novel/vo/VideoGenerationTaskVO.java` — 加入 `firstFrameId`。
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/VideoGenerationTaskMapper.java` — 保存和读取 `first_frame_id`，并把运行查询限定为视频状态。
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/VideoGenerationService.java` — 创建视频任务时接收首帧 ID。
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImpl.java` — 移除首帧生成，验证并快照所选首帧。
- Modify: `novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java` — 暴露首帧接口，转发 `firstFrameId`。
- Modify: `novel/src/main/resources/static/js/video-generation.js` — 首帧按钮、列表、选择、删除和独立轮询。
- Modify: `novel/src/main/resources/static/css/outline.css` — 首帧候选网格、选中态、删除按钮和加载态。
- Modify: `novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImplTest.java` — 验证视频仅使用选择首帧。
- Create: `novel/src/test/java/com/novelgeneration/novel/service/impl/StoryboardFirstFrameServiceImplTest.java` — 验证独立首帧任务生命周期。
- Create: `novel/src/test/java/com/novelgeneration/novel/mapper/StoryboardFirstFrameMapperParameterTest.java` — 验证 Mapper 参数名。
- Modify: `novel/src/test/java/com/novelgeneration/novel/controller/VideoGenerationControllerTest.java` — 验证新路由与 `firstFrameId`。
- Modify: `novel/src/test/frontend/video-generation-page.test.js` — 验证页面行为锚点。

## Task 1: 数据模型与 Mapper

**Files:**
- Create: `novel/src/main/resources/sql/create_storyboard_first_frame_tables.sql`
- Create: `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardFirstFrameVO.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardFirstFrameMapper.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/vo/VideoGenerationTaskVO.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/VideoGenerationTaskMapper.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/mapper/StoryboardFirstFrameMapperParameterTest.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/mapper/VideoGenerationMapperParameterTest.java`

**Interfaces:**
- Produces `StoryboardFirstFrameVO` with `id`, `novelId`, `chapterNum`, `sceneId`, `version`, `status`, `imagePath`, `promptSnapshot`, `styleSnapshot`, `errorMessage`, `deleted`, `createTime`, `updateTime`.
- Produces `StoryboardFirstFrameMapper.insert`, `selectById`, `selectByScene`, `selectRunning`, `nextVersion`, `updateStatus`, `softDelete`.
- Extends `VideoGenerationTaskVO` and `VideoGenerationTaskMapper` with `Long firstFrameId` / `first_frame_id`.

- [ ] **Step 1: Write failing Mapper parameter tests**

```java
@Test
void firstFrameMapperUsesNamedParameters() throws Exception {
    assertTrue(method(StoryboardFirstFrameMapper.class, "selectByScene")
        .getParameters()[0].isAnnotationPresent(Param.class));
    assertEquals("sceneId", paramValue(method(StoryboardFirstFrameMapper.class, "nextVersion"), 0));
}

@Test
void videoTaskMapperExposesFirstFrameId() throws Exception {
    assertNotNull(VideoGenerationTaskVO.class.getDeclaredField("firstFrameId"));
    assertTrue(VideoGenerationTaskMapper.class.getMethod("insert", VideoGenerationTaskVO.class) != null);
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run: `mvn -q -Dtest=StoryboardFirstFrameMapperParameterTest,VideoGenerationMapperParameterTest test`

Expected: failure because `StoryboardFirstFrameMapper` and `firstFrameId` do not exist.

- [ ] **Step 3: Add SQL, VO and Mapper implementation**

```sql
CREATE TABLE IF NOT EXISTS storyboard_first_frame (
  id BIGINT PRIMARY KEY AUTO_INCREMENT,
  novel_id BIGINT NOT NULL,
  chapter_num BIGINT NOT NULL,
  storyboard_scene_id BIGINT NOT NULL,
  version INT NOT NULL,
  status VARCHAR(32) NOT NULL,
  image_path VARCHAR(1000),
  prompt_snapshot LONGTEXT,
  style_snapshot LONGTEXT,
  error_message TEXT,
  is_deleted TINYINT(1) NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  UNIQUE KEY uk_first_frame_version (storyboard_scene_id, version),
  KEY idx_first_frame_scene (novel_id, chapter_num, storyboard_scene_id, is_deleted),
  KEY idx_first_frame_running (storyboard_scene_id, status)
);

ALTER TABLE video_generation_task ADD COLUMN first_frame_id BIGINT NULL AFTER storyboard_scene_id;
```

Implement the Mapper with `@Param` on every multi-argument method and query lists with `is_deleted=0 ORDER BY version DESC`. Ensure `selectRunning` only returns `QUEUED` or `GENERATING` first-frame rows.

- [ ] **Step 4: Run the Mapper tests and verify they pass**

Run: `mvn -q -Dtest=StoryboardFirstFrameMapperParameterTest,VideoGenerationMapperParameterTest test`

Expected: exit code 0.

## Task 2: 共享分镜生成上下文

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/dto/StoryboardGenerationContext.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/StoryboardGenerationPreparationService.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardGenerationPreparationServiceImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImpl.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/StoryboardGenerationPreparationServiceImplTest.java`

**Interfaces:**
- Consumes `StoryboardService`, `VisualAssetService`, `VisualStyleService` and `video.generation.image-base-url`.
- Produces `StoryboardGenerationContext prepare(Long novelId, Long chapterNum, Long sceneId, String promptOverride)`.
- `StoryboardGenerationContext` contains `StoryboardVO.Scene scene`, `StoryboardFrameRequest frameRequest`, `VideoProviderRequest videoRequest`, `List<StoryboardVideoVO.AssetSnapshot> assetSnapshots`, `String stylePrompt`.

- [ ] **Step 1: Write a failing preparation test**

```java
@Test
void prepareBuildsFrameAndVideoRequestsFromTheSameScene() {
    StoryboardGenerationContext context = service.prepare(1L, 2L, 3L, null);
    assertEquals(3L, context.getScene().getId());
    assertTrue(context.getFrameRequest().getPrompt().contains("单张16:9完整影视画面"));
    assertEquals(10, context.getVideoRequest().getDurationSec());
    assertEquals(2, context.getAssetSnapshots().size());
}
```

- [ ] **Step 2: Run the preparation test and verify it fails**

Run: `mvn -q -Dtest=StoryboardGenerationPreparationServiceImplTest test`

Expected: failure because `StoryboardGenerationPreparationService` does not exist.

- [ ] **Step 3: Extract the existing preparation logic into the new service**

```java
public interface StoryboardGenerationPreparationService {
    StoryboardGenerationContext prepare(Long novelId, Long chapterNum, Long sceneId, String promptOverride);
}
```

Move `prepare`, asset validation, provider image URL conversion, scene-video prompt construction and first-frame prompt construction out of `VideoGenerationServiceImpl`. Keep the existing validation text: missing storyboard, missing scene, missing asset three-view, and missing public `imageBaseUrl` must still throw `IllegalArgumentException` with their current Chinese messages.

- [ ] **Step 4: Run the preparation test and existing video tests**

Run: `mvn -q -Dtest=StoryboardGenerationPreparationServiceImplTest,VideoGenerationServiceImplTest test`

Expected: exit code 0.

## Task 3: 独立首帧任务服务与接口

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/service/StoryboardFirstFrameService.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardFirstFrameServiceImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/StoryboardFirstFrameServiceImplTest.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/controller/VideoGenerationControllerTest.java`

**Interfaces:**
- `StoryboardFirstFrameService.createTask(Long novelId, Long chapterNum, Long sceneId)` returns `StoryboardFirstFrameVO`.
- `StoryboardFirstFrameService.getTask(Long firstFrameId)` returns `StoryboardFirstFrameVO`.
- `StoryboardFirstFrameService.listByScene(Long novelId, Long chapterNum, Long sceneId)` returns `List<StoryboardFirstFrameVO>`.
- `StoryboardFirstFrameService.delete(Long firstFrameId)` performs logical deletion only.

- [ ] **Step 1: Write failing lifecycle and controller tests**

```java
@Test
void createTaskReturnsExistingRunningFirstFrameTask() {
    when(mapper.selectRunning(1L, 2L, 3L)).thenReturn(runningTask());
    assertEquals(9L, service.createTask(1L, 2L, 3L).getId());
    verify(generator, never()).generate(any());
}

@Test
void deleteMarksFirstFrameDeletedWithoutDeletingFiles() {
    service.delete(8L);
    verify(mapper).softDelete(8L);
    verifyNoInteractions(fileSystemMock);
}

@Test
void controllerCreatesFirstFrameTaskAtSceneRoute() {
    assertTrue(mapping("/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/first-frame-tasks", "POST"));
}
```

- [ ] **Step 2: Run the tests and verify they fail**

Run: `mvn -q -Dtest=StoryboardFirstFrameServiceImplTest,VideoGenerationControllerTest test`

Expected: failure because the independent first-frame service and routes do not exist.

- [ ] **Step 3: Implement the asynchronous first-frame lifecycle**

```java
public StoryboardFirstFrameVO createTask(Long novelId, Long chapterNum, Long sceneId) {
    StoryboardFirstFrameVO running = mapper.selectRunning(novelId, chapterNum, sceneId);
    if (running != null) return running;
    StoryboardGenerationContext context = preparationService.prepare(novelId, chapterNum, sceneId, null);
    StoryboardFirstFrameVO task = new StoryboardFirstFrameVO();
    task.setNovelId(novelId);
    task.setChapterNum(chapterNum);
    task.setSceneId(sceneId);
    task.setVersion(mapper.nextVersion(sceneId));
    task.setStatus("QUEUED");
    task.setPromptSnapshot(context.getFrameRequest().getPrompt());
    task.setStyleSnapshot(context.getStylePrompt());
    mapper.insert(task);
    executor.submit(() -> generate(task.getId(), context.getFrameRequest()));
    return task;
}
```

`generate` must transition `QUEUED -> GENERATING -> SUCCESS` with `imagePath`, or `FAILED` with `safeMessage(exception)`. The controller must expose create, task status, list and `DELETE` endpoints. Do not add physical file deletion.

- [ ] **Step 4: Run task/service/controller tests and verify they pass**

Run: `mvn -q -Dtest=StoryboardFirstFrameServiceImplTest,VideoGenerationControllerTest test`

Expected: exit code 0.

## Task 4: 视频任务显式引用首帧

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/dto/VideoGenerationRequest.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/VideoGenerationService.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/VideoGenerationTaskMapper.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImplTest.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/controller/VideoGenerationControllerTest.java`

**Interfaces:**
- `VideoGenerationRequest` adds `Long firstFrameId`.
- `VideoGenerationService.createTask(Long novelId, Long chapterNum, Long sceneId, Long firstFrameId, String promptOverride)` returns `VideoGenerationTaskVO`.
- Video retries use `old.getFirstFrameId()` and `old.getFirstFramePath()`; they do not call `StoryboardFrameGenerator.generate`.

- [ ] **Step 1: Write failing video-selection tests**

```java
@Test
void createTaskRejectsMissingFirstFrameId() {
    IllegalArgumentException error = assertThrows(IllegalArgumentException.class,
        () -> service.createTask(1L, 2L, 3L, null, null));
    assertEquals("请先选择当前分镜的有效首帧", error.getMessage());
}

@Test
void createTaskSnapshotsSelectedFirstFrameWithoutGeneratingAnother() {
    when(firstFrameMapper.selectAvailableById(8L)).thenReturn(frame(8L, 1L, 2L, 3L, "/api/storyboard-frames/files/1/chapter-2/scene-3/a.png"));
    service.createTask(1L, 2L, 3L, 8L, null);
    verify(taskMapper).insert(argThat(task -> task.getFirstFrameId().equals(8L)
        && task.getFirstFramePath().endsWith("a.png")));
    verify(frameGenerator, never()).generate(any());
}

@Test
void retryUsesFirstFrameSnapshotEvenWhenSourceFrameIsDeleted() {
    service.retry(99L, null);
    verify(videoClient).submit(argThat(request -> request.getFirstFrameImageUrl().endsWith("a.png")));
}
```

- [ ] **Step 2: Run the video-selection tests and verify they fail**

Run: `mvn -q -Dtest=VideoGenerationServiceImplTest,VideoGenerationControllerTest test`

Expected: failure because the create API does not accept or validate `firstFrameId`.

- [ ] **Step 3: Implement first-frame validation and snapshot flow**

```java
StoryboardFirstFrameVO frame = firstFrameMapper.selectAvailableById(firstFrameId);
if (frame == null || !novelId.equals(frame.getNovelId()) || !chapterNum.equals(frame.getChapterNum())
        || !sceneId.equals(frame.getSceneId()) || !"SUCCESS".equals(frame.getStatus())) {
    throw new IllegalArgumentException("请先选择当前分镜的有效首帧");
}
task.setFirstFrameId(frame.getId());
task.setFirstFramePath(frame.getImagePath());
```

Remove `GENERATING_FRAME` and `StoryboardFrameGenerator.generate` calls from `VideoGenerationServiceImpl.execute`. Build `VideoProviderRequest` from `StoryboardGenerationPreparationService`, set its first-frame image URL from the stored `firstFramePath`, then submit and poll only the video provider. Preserve `firstFramePath` in `StoryboardVideoVO` as the video-history snapshot.

- [ ] **Step 4: Run the video tests and verify they pass**

Run: `mvn -q -Dtest=VideoGenerationServiceImplTest,VideoGenerationControllerTest test`

Expected: exit code 0.

## Task 5: 视频页面首帧候选图交互

**Files:**
- Modify: `novel/src/main/resources/static/js/video-generation.js`
- Modify: `novel/src/main/resources/static/css/outline.css`
- Modify: `novel/src/test/frontend/video-generation-page.test.js`

**Interfaces:**
- `POST /api/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/first-frame-tasks`
- `GET /api/first-frame-tasks/{taskId}`
- `GET /api/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/first-frames`
- `DELETE /api/storyboard-first-frames/{firstFrameId}`
- Existing video create request body becomes `{sceneId, firstFrameId, promptOverride}`.

- [ ] **Step 1: Write failing page behavior checks**

```javascript
assert.match(source, /data-action="generate-first-frame"/);
assert.match(source, /data-role="first-frame-list"/);
assert.match(source, /selectedFirstFrameId/);
assert.match(source, /请先选择首帧/);
assert.match(source, /data-action="delete-first-frame"/);
assert.match(source, /first-frame-tasks/);
```

- [ ] **Step 2: Run the page check and verify it fails**

Run: `node src/test/frontend/video-generation-page.test.js`

Expected: failure because dedicated first-frame controls and selection state do not exist.

- [ ] **Step 3: Implement card-local first-frame state and controls**

```javascript
const selection = new Map();

function selectedFirstFrameId(card) {
    return selection.get(card.dataset.sceneId) || null;
}

async function generateVideo(card, scene, promptOverride = null) {
    const firstFrameId = selectedFirstFrameId(card);
    if (!firstFrameId) {
        setCardStatus(card, '请先选择首帧', true);
        return;
    }
    // POST body: {sceneId: scene.id, firstFrameId, promptOverride}
}
```

Render every undeleted candidate as a thumbnail button. On click, update only `selection`, re-render its selected outline and label. Add a per-thumbnail delete button that calls the `DELETE` API and clears the selection if the deleted ID was selected. Add a dedicated first-frame poller map separate from the video poller map. During a running first-frame task disable only `generate-first-frame`; during a running video task disable only `generate-video`. On failed video retry, leave `selection` unchanged and submit the stored selection again.

- [ ] **Step 4: Run the page check and verify it passes**

Run: `node src/test/frontend/video-generation-page.test.js`

Expected: `video-generation page checks passed`.

## Task 6: 全量验证与数据库交接

**Files:**
- Modify: `docs/superpowers/specs/2026-09-13-independent-storyboard-first-frame-design.md` only if verification exposes a design contradiction.
- Verify: `novel/src/main/resources/sql/create_storyboard_first_frame_tables.sql`

**Interfaces:**
- Database migration is delivered as an idempotent SQL script; it is not executed automatically.

- [ ] **Step 1: Inspect migration safety**

Run: `Get-Content novel/src/main/resources/sql/create_storyboard_first_frame_tables.sql`

Expected: table creation uses `IF NOT EXISTS`; the `first_frame_id` alteration is clearly separated and documented for one-time execution.

- [ ] **Step 2: Run all backend tests**

Run: `mvn -q test`

Expected: exit code 0 and no test failures.

- [ ] **Step 3: Run all frontend page checks**

Run: `Get-ChildItem src/test/frontend/*.test.js | ForEach-Object { node $_.FullName; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }`

Expected: each page check prints `passed`.

- [ ] **Step 4: Check the patch for whitespace errors**

Run: `git diff --check`

Expected: no whitespace errors attributable to the feature files.

- [ ] **Step 5: Hand off the migration command without executing it**

Provide the exact command:

```sql
source novel/src/main/resources/sql/create_storyboard_first_frame_tables.sql;
```

State that the user must run it against the configured MySQL database before using the new endpoints.

## Plan Self-Review

- Spec coverage: Tasks 1 and 3 cover independent storage/task lifecycle; Task 4 covers selected-frame video creation and retry; Task 5 covers all confirmed UI behavior; Task 6 covers verification and migration handoff.
- Placeholder scan: no unresolved placeholders or deferred implementation steps remain.
- Type consistency: `firstFrameId` is used in the request DTO, task VO, Mapper, service, controller and frontend request body. `StoryboardFirstFrameVO` is the sole source of selectable frames.
