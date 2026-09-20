# 分镜首帧驱动的视频生成 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 复用现有 Seedream 生图配置为每个分镜生成实际画面首帧，再以 `first_frame` 方式调用 `doubao-seedance-1-5-pro-251215` 生成视频。

**Architecture:** 新增独立的 `StoryboardFrameGenerator` 隔离首帧生图协议；`VideoGenerationServiceImpl` 继续编排异步任务，先生成或复用首帧，再调用现有方舟视频客户端。任务和视频版本保存首帧相对路径，前端轮询时展示阶段状态和首帧预览。

**Tech Stack:** Java 17, Spring Boot 2.6.13, Spring MVC, MyBatis, MySQL, Jackson, RestTemplate, JUnit 5, Mockito, 原生 JavaScript。

## Global Constraints

- 使用现有 `image.api-key`、`image.api-url`、`image.api-model` 生成首帧。
- 视频模型保持 `doubao-seedance-1-5-pro-251215`。
- 三视图只提交给 Seedream；Seedance 只接收动态文本和一张 `first_frame`。
- 首帧是 16:9 实际影视画面，不得生成三视图排版稿。
- 每个分镜独立生成，视频失败后复用已成功生成的首帧。
- 生成文件不提交 Git；未经用户要求不得推送 Gitee。
- 所有生产代码先有失败测试，再写最小实现。

---

### Task 1: Persist first-frame paths

**Files:**
- Modify: `novel/src/main/resources/sql/create_video_generation_tables.sql`
- Create: `novel/src/main/resources/sql/alter_video_generation_add_first_frame.sql`
- Modify: `novel/src/main/java/com/novelgeneration/novel/vo/VideoGenerationTaskVO.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardVideoVO.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/VideoGenerationTaskMapper.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardVideoMapper.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/mapper/VideoGenerationMapperParameterTest.java`

**Interfaces:**
- `VideoGenerationTaskVO.firstFramePath: String`
- `StoryboardVideoVO.firstFramePath: String`
- `VideoGenerationTaskMapper.updateFirstFrame(Long taskId, String firstFramePath)`

- [ ] **Step 1: Write the failing mapper/VO test**

```java
@Test
void taskAndVideoExposeFirstFramePath() {
    VideoGenerationTaskVO task = new VideoGenerationTaskVO();
    task.setFirstFramePath("/api/storyboard-frames/files/1/chapter-1/scene-1/frame.png");
    StoryboardVideoVO video = new StoryboardVideoVO();
    video.setFirstFramePath(task.getFirstFramePath());
    assertEquals(task.getFirstFramePath(), video.getFirstFramePath());
}

@Test
void updateFirstFrameNamesBothMyBatisParameters() throws Exception {
    Method method = VideoGenerationTaskMapper.class.getMethod(
            "updateFirstFrame", Long.class, String.class);
    assertEquals("taskId", method.getParameters()[0].getAnnotation(Param.class).value());
    assertEquals("firstFramePath", method.getParameters()[1].getAnnotation(Param.class).value());
}
```

- [ ] **Step 2: Run the test and confirm RED**

Run: `mvn -q -Dtest=VideoGenerationMapperParameterTest test`

Expected: compilation fails because the fields and mapper method do not exist.

- [ ] **Step 3: Add schema and mapper support**

Add `first_frame_path VARCHAR(1000)` to both create-table definitions. The one-time migration contains:

```sql
ALTER TABLE video_generation_task
    ADD COLUMN first_frame_path VARCHAR(1000) NULL AFTER provider_task_id;
ALTER TABLE storyboard_video
    ADD COLUMN first_frame_path VARCHAR(1000) NULL AFTER video_path;
```

Update mapper projections/inserts and add:

```java
@Update("UPDATE video_generation_task SET first_frame_path=#{firstFramePath}, update_time=NOW() WHERE id=#{taskId}")
int updateFirstFrame(@Param("taskId") Long taskId,
                     @Param("firstFramePath") String firstFramePath);
```

- [ ] **Step 4: Run the test and confirm GREEN**

Run: `mvn -q -Dtest=VideoGenerationMapperParameterTest test`

Expected: PASS.

### Task 2: Generate a storyboard first frame with Seedream

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/dto/StoryboardFrameRequest.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/StoryboardFrameGenerator.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredStoryboardFrameGenerator.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/ConfiguredStoryboardFrameGeneratorTest.java`

**Interfaces:**

```java
public interface StoryboardFrameGenerator {
    String generate(StoryboardFrameRequest request);
}
```

`StoryboardFrameRequest` contains `novelId`, `chapterNum`, `sceneId`, `prompt`, and `List<String> referenceImageUrls`.

- [ ] **Step 1: Write a failing request-shape test**

Use `MockRestServiceServer` and invoke `generate`. Assert the outgoing JSON contains the configured model, prompt, every reference URL in `image`, `size: "2K"`, `response_format: "b64_json"`, `stream: false`, and `watermark: false`. Return `{"data":[{"b64_json":"AQID"}]}` and assert the path starts with `/api/storyboard-frames/files/`.

- [ ] **Step 2: Run the test and confirm RED**

Run: `mvn -q -Dtest=ConfiguredStoryboardFrameGeneratorTest test`

Expected: compilation fails because the generator does not exist.

- [ ] **Step 3: Implement the generator**

Submit this body to the normalized `/images/generations` endpoint:

```java
Map<String, Object> body = new HashMap<>();
body.put("model", apiModel);
body.put("prompt", request.getPrompt());
body.put("image", request.getReferenceImageUrls());
body.put("size", "2K");
body.put("sequential_image_generation", "disabled");
body.put("response_format", "b64_json");
body.put("stream", false);
body.put("watermark", false);
```

Accept `data[0].b64_json` or `data[0].url`. Save the bytes under:

```text
uploads/storyboard-frames/{novelId}/chapter-{chapterNum}/scene-{sceneId}/frame-{uuid}.png
```

Return `/api/storyboard-frames/files/{novelId}/chapter-{chapterNum}/scene-{sceneId}/{filename}`.

- [ ] **Step 4: Run the test and confirm GREEN**

Run: `mvn -q -Dtest=ConfiguredStoryboardFrameGeneratorTest test`

Expected: PASS.

### Task 3: Submit one first frame to Seedance

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/dto/VideoProviderRequest.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClient.java`
- Modify: `novel/src/test/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClientTest.java`

**Interfaces:**
- Replace provider-facing `referenceImages` with `firstFrameImageUrl`.
- Keep asset snapshots in the orchestration service; they are not part of the video provider request.

- [ ] **Step 1: Replace the Ark request test with a failing first-frame test**

```java
request.setFirstFrameImageUrl("https://example.com/frame.png");
client.submit(request);

assertTrue(body.contains("\"role\":\"first_frame\""));
assertTrue(body.contains("https://example.com/frame.png"));
assertFalse(body.contains("reference_image"));
assertTrue(body.contains("\"generate_audio\":true"));
```

- [ ] **Step 2: Run and confirm RED**

Run: `mvn -q -Dtest=ConfiguredVideoGenerationClientTest test`

Expected: FAIL because the DTO lacks `firstFrameImageUrl` and the client emits `reference_image`.

- [ ] **Step 3: Implement the Ark request change**

Build content as one text item and one image item:

```java
content.add(Map.of("type", "text", "text", joinPrompt(request)));
content.add(Map.of(
        "type", "image_url",
        "role", "first_frame",
        "image_url", Map.of("url", request.getFirstFrameImageUrl())));
body.put("generate_audio", true);
```

Reject a blank first-frame URL before the HTTP call. Keep `ratio`, `duration`, and `watermark` for Seedance 1.5 Pro.

- [ ] **Step 4: Run and confirm GREEN**

Run: `mvn -q -Dtest=ConfiguredVideoGenerationClientTest test`

Expected: PASS.

### Task 4: Orchestrate frame generation, reuse, and video persistence

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImpl.java`
- Create: `novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImplTest.java`

**Interfaces:**
- `prepare` collects asset URLs for Seedream and preserves `AssetSnapshot` records.
- `execute` advances through `GENERATING_FRAME` and `GENERATING_VIDEO`.
- `retry` forwards the failed task's `firstFramePath` as a reusable frame.

- [ ] **Step 1: Write failing orchestration tests**

Cover:

```java
firstFrameFailureNeverCallsVideoClient();
successfulFrameIsStoredBeforeVideoSubmission();
videoProviderReceivesPublicFirstFrameUrlOnly();
videoFailureKeepsFirstFramePath();
retryReusesReadableFirstFrame();
successfulVideoStoresFirstFrameAndAssetSnapshots();
```

Invoke the private `execute` method reflectively so assertions are deterministic.

- [ ] **Step 2: Run and confirm RED**

Run: `mvn -q -Dtest=VideoGenerationServiceImplTest test`

Expected: FAIL because the service does not generate or persist a first frame.

- [ ] **Step 3: Build the first-frame prompt**

```text
请生成一张单张16:9完整影视画面，作为该分镜视频的第一帧。不要生成三视图、设定稿、拼图、边框或文字标签。
严格保持参考图中人物的面部、发型、服装与体型，以及场景的建筑、材质和时代特征。
全书视觉风格：...
场景与时间：...
画面描述：...
人物动作：...
镜头与构图：...
```

- [ ] **Step 4: Implement state transitions and reuse**

Set `GENERATING_FRAME`, generate the frame, call `taskMapper.updateFirstFrame`, convert the local API path with `video.generation.image-base-url`, set `VideoProviderRequest.firstFrameImageUrl`, then set `GENERATING_VIDEO` and submit. Retry reuses the old frame only when the resolved file exists; otherwise it regenerates.

- [ ] **Step 5: Persist the frame on successful video versions**

Set `StoryboardVideoVO.firstFramePath` before `videoMapper.insert(video)`. Keep the asset snapshot loop.

- [ ] **Step 6: Run and confirm GREEN**

Run: `mvn -q -Dtest=VideoGenerationServiceImplTest test`

Expected: PASS.

### Task 5: Serve and display storyboard frames

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java`
- Modify: `novel/src/main/resources/static/js/video-generation.js`
- Modify: `novel/src/main/resources/static/css/outline.css`
- Modify: `novel/src/test/frontend/video-generation-page.test.js`

**Interfaces:**
- `GET /api/storyboard-frames/files/{novelId}/chapter-{chapterNum}/scene-{sceneId}/{filename}` returns an image.
- Task and video history JSON expose `firstFramePath`.

- [ ] **Step 1: Add failing frontend assertions**

Assert the script maps `GENERATING_FRAME` to “正在生成分镜首帧”, maps `GENERATING_VIDEO` to “正在生成视频”, and renders `task.firstFramePath` or `video.firstFramePath` as an image.

- [ ] **Step 2: Run and confirm RED**

Run: `node src/test/frontend/video-generation-page.test.js`

Expected: FAIL because no first-frame UI exists.

- [ ] **Step 3: Add the secured file endpoint**

Resolve under `${image.storyboard-frame-dir:uploads/storyboard-frames}`, normalize it, reject paths outside the root, and return `MediaType.IMAGE_PNG`.

- [ ] **Step 4: Update the card UI**

Render a compact “分镜首帧” preview above the video. During polling, update it as soon as `firstFramePath` appears, even if video generation later fails.

- [ ] **Step 5: Run and confirm GREEN**

Run: `node src/test/frontend/video-generation-page.test.js`

Expected: PASS.

### Task 6: Configuration and verification

**Files:**
- Modify: `novel/src/main/resources/application.properties.example`
- Modify: `novel/src/main/resources/application.properties`
- Modify: repository ignore rules only if generated directories are not already ignored.

- [ ] **Step 1: Add non-secret configuration**

```properties
image.storyboard-frame-dir=uploads/storyboard-frames
video.generation.model=doubao-seedance-1-5-pro-251215
```

Remove `video.generation.max-reference-images` because the video provider no longer consumes asset reference images.

- [ ] **Step 2: Run targeted Java tests**

Run:

```powershell
mvn -q -Dtest=ConfiguredStoryboardFrameGeneratorTest,ConfiguredVideoGenerationClientTest,VideoGenerationServiceImplTest,VideoGenerationMapperParameterTest test
```

Expected: all targeted tests PASS.

- [ ] **Step 3: Run frontend checks**

Run:

```powershell
node src/test/frontend/video-generation-page.test.js
node src/test/frontend/novel-detail-page.test.js
```

Expected: both checks PASS.

- [ ] **Step 4: Run regression and diff checks**

Run:

```powershell
mvn -q test
git diff --check
```

Expected: Maven exits 0 and `git diff --check` reports no whitespace errors.

- [ ] **Step 5: Manual acceptance**

Open `video-generation.html?novelId=53&chapterNum=1`, generate one scene, and verify the UI transitions through first-frame generation and video generation. Confirm the Seedream request receives the linked three-view images, the Ark request contains exactly one `first_frame`, the first frame remains visible on video failure, and retry does not create a second frame file.
