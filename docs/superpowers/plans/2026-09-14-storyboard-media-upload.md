# 分镜首帧与成片上传 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Let a user upload one first-frame image or one final video for a storyboard scene while preserving the same version history, selection, and fallback behavior used by AI-generated media.

**Architecture:** Extend the existing `storyboard_first_frame` and `storyboard_video` version tables with source and deletion metadata instead of adding parallel upload tables. Add multipart upload methods to the existing first-frame and video services, persist files under the current media roots, and expose them through the existing file routes. The video-generation page will add hidden single-file inputs and buttons that create upload versions, then refresh the established candidate and history views.

**Tech Stack:** Spring Boot 2.7, MyBatis annotation mappers, MySQL, `MultipartFile`, Java NIO, vanilla JavaScript, Node `assert` page-contract tests, JUnit 5 and Mockito.

## Global Constraints

- First frames accept only `PNG`, `JPG`, `JPEG`, and `WebP`, one file per request, up to 20 MB.
- Final videos accept only `MP4`, `WebM`, and `MOV`, one file per request, up to 500 MB.
- Check extension, declared MIME type, file size, and actual readable image/video content server-side; never trust a filename alone.
- Add `source` as `AI` or `UPLOAD`; existing records migrate to `AI`.
- Uploaded first frames create `SUCCESS` candidate versions and are automatically selected in the current page session.
- Uploaded videos create `UPLOAD` versions, become current immediately, and never call an AI provider.
- Video deletion is logical. Deleting the current version promotes the newest remaining undeleted version, or leaves no current video.
- Do not commit, push, or synchronize to Gitee unless the user explicitly asks.

---

## File Structure

- `novel/src/main/resources/sql/alter_storyboard_media_upload.sql`: idempotent migration that adds/backfills source and deletion columns plus lookup indexes.
- `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardFirstFrameVO.java`: adds `source`.
- `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardVideoVO.java`: adds `source` and `deleted`.
- `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardFirstFrameMapper.java`: maps source and inserts uploaded versions.
- `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardVideoMapper.java`: maps source/deletion, inserts uploads, and atomically supports current-version fallback.
- `novel/src/main/java/com/novelgeneration/novel/service/StoryboardFirstFrameService.java`: declares image upload API.
- `novel/src/main/java/com/novelgeneration/novel/service/VideoGenerationService.java`: declares video upload and delete APIs.
- `novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardFirstFrameServiceImpl.java`: validates, stores, and creates manual first-frame versions.
- `novel/src/main/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImpl.java`: validates, stores, creates manual video versions, and handles logical deletion/fallback.
- `novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java`: provides multipart upload and video-delete endpoints with correct media response types.
- `novel/src/main/resources/static/js/video-generation.js`: adds upload actions, file pickers, upload state, source labels, and delete-current refresh.
- `novel/src/main/resources/static/css/outline.css`: styles upload buttons and source badges without changing page layout.
- `novel/src/test/java/com/novelgeneration/novel/service/impl/StoryboardFirstFrameServiceImplTest.java`: first-frame upload tests.
- `novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImplTest.java`: video upload and fallback tests.
- `novel/src/test/java/com/novelgeneration/novel/controller/VideoGenerationControllerTest.java`: multipart route and response tests.
- `novel/src/test/java/com/novelgeneration/novel/mapper/StoryboardFirstFrameMapperParameterTest.java`: mapper source parameter contract.
- `novel/src/test/java/com/novelgeneration/novel/mapper/VideoGenerationMapperParameterTest.java`: mapper source/deletion/fallback parameter contract.
- `novel/src/test/frontend/video-generation-page.test.js`: upload UI and client request contract tests.

### Task 1: Add Safe, Backward-Compatible Media Version Metadata

**Files:**
- Create: `novel/src/main/resources/sql/alter_storyboard_media_upload.sql`
- Modify: `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardFirstFrameVO.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardVideoVO.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardFirstFrameMapper.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardVideoMapper.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/mapper/StoryboardFirstFrameMapperParameterTest.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/mapper/VideoGenerationMapperParameterTest.java`

**Interfaces:**
- Produces `StoryboardFirstFrameVO#getSource(): String` and `StoryboardVideoVO#getSource(): String`, where values are `AI` or `UPLOAD`.
- Produces `StoryboardVideoVO#getDeleted(): Boolean`.
- Produces mapper methods `softDelete(Long videoId)`, `selectLatestAvailableByScene(Long sceneId)`, and source-aware `insert(...)` methods.

- [ ] **Step 1: Write failing mapper contract tests**

Add assertions that inspect mapper annotation SQL and VO fields:

```java
assertTrue(StoryboardFirstFrameMapper.COLUMNS.contains("source AS source"));
assertTrue(StoryboardVideoMapper.COLUMNS.contains("source AS source"));
assertTrue(StoryboardVideoMapper.COLUMNS.contains("is_deleted AS deleted"));
assertNotNull(StoryboardVideoMapper.class.getMethod("softDelete", Long.class));
assertNotNull(StoryboardVideoMapper.class.getMethod("selectLatestAvailableByScene", Long.class));
```

- [ ] **Step 2: Run the mapper contract tests and verify failure**

Run: `mvn -q -Dtest=StoryboardFirstFrameMapperParameterTest,VideoGenerationMapperParameterTest test`

Expected: FAIL because `source`, `is_deleted`, and fallback mapper methods do not exist.

- [ ] **Step 3: Create idempotent database migration**

Create a migration using `information_schema.COLUMNS`, matching the repository's existing dynamic migration convention. Add and backfill:

```sql
ALTER TABLE storyboard_first_frame
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'AI' AFTER status;

ALTER TABLE storyboard_video
    ADD COLUMN source VARCHAR(16) NOT NULL DEFAULT 'AI' AFTER version,
    ADD COLUMN is_deleted TINYINT(1) NOT NULL DEFAULT 0 AFTER is_current;

UPDATE storyboard_first_frame SET source = 'AI' WHERE source IS NULL OR source = '';
UPDATE storyboard_video SET source = 'AI' WHERE source IS NULL OR source = '';
```

Add an index supporting undeleted video lookup by `(storyboard_scene_id, is_deleted, version)` only when absent.

- [ ] **Step 4: Extend VOs and mapper SQL**

Add fields:

```java
// StoryboardFirstFrameVO
private String source;

// StoryboardVideoVO
private String source;
private Boolean deleted;
```

Define a `COLUMNS` constant for `StoryboardVideoMapper`, use it in all video selects, filter `is_deleted=0` in normal scene-history queries, and add:

```java
@Update("UPDATE storyboard_video SET is_deleted=1, is_current=0 WHERE id=#{videoId}")
int softDelete(@Param("videoId") Long videoId);

@Select("SELECT " + COLUMNS + "FROM storyboard_video WHERE storyboard_scene_id=#{sceneId} "
      + "AND is_deleted=0 ORDER BY version DESC LIMIT 1")
StoryboardVideoVO selectLatestAvailableByScene(@Param("sceneId") Long sceneId);
```

Add `source` to both insert column lists and values. Set `source` to `AI` in existing AI create flows before calling their insert methods.

- [ ] **Step 5: Run mapper contract tests and verify pass**

Run: `mvn -q -Dtest=StoryboardFirstFrameMapperParameterTest,VideoGenerationMapperParameterTest test`

Expected: PASS.

### Task 2: Implement Validated First-Frame Upload Versions

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/StoryboardFirstFrameService.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardFirstFrameServiceImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/StoryboardFirstFrameServiceImplTest.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/controller/VideoGenerationControllerTest.java`

**Interfaces:**
- Consumes `MultipartFile file` uploaded as form field `file`.
- Produces `StoryboardFirstFrameService#upload(Long novelId, Long chapterNum, Long sceneId, MultipartFile file): StoryboardFirstFrameVO`.
- Produces `POST /api/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/first-frames/upload`.

- [ ] **Step 1: Write failing first-frame upload service tests**

Add tests using `MockMultipartFile` that assert:

```java
StoryboardFirstFrameVO frame = service.upload(1L, 2L, 3L,
    new MockMultipartFile("file", "frame.png", "image/png", pngBytes));

assertEquals("SUCCESS", frame.getStatus());
assertEquals("UPLOAD", frame.getSource());
assertEquals(7, frame.getVersion());
assertTrue(frame.getImagePath().contains("/api/storyboard-frames/files/1/chapter-2/scene-3/"));
```

Also test blank files, `image/gif`, a 20 MB plus one byte file, and unparseable image bytes; each must throw `IllegalArgumentException` before mapper insert.

- [ ] **Step 2: Run the focused service test and verify failure**

Run: `mvn -q -Dtest=StoryboardFirstFrameServiceImplTest test`

Expected: FAIL because `upload` is not declared.

- [ ] **Step 3: Declare and implement `upload`**

Add the service method:

```java
StoryboardFirstFrameVO upload(Long novelId, Long chapterNum, Long sceneId, MultipartFile file);
```

In the implementation:

```java
private static final long MAX_FIRST_FRAME_BYTES = 20L * 1024 * 1024;
private static final Set<String> FIRST_FRAME_EXTENSIONS = Set.of("png", "jpg", "jpeg", "webp");
private static final Set<String> FIRST_FRAME_CONTENT_TYPES = Set.of(
    "image/png", "image/jpeg", "image/webp");
```

Validate nonempty upload, normalized lowercase extension, declared MIME type, size, and actual image readability via `ImageIO.read`. For WebP, accept its declared content type only after reading bytes and checking the `RIFF....WEBP` signature; do not reject it merely because standard `ImageIO` lacks a WebP reader.

Create the directory below the existing configured `storyboardFrameDir`, copy stream bytes to a UUID filename, create a `SUCCESS/UPLOAD` version using `firstFrameMapper.nextVersion(sceneId)`, and on any mapper failure delete only the newly written file. Return the persisted VO with its generated ID and public `/api/storyboard-frames/files/...` path.

- [ ] **Step 4: Add controller multipart route**

Import `RequestParam` and `MultipartFile`, then add:

```java
@PostMapping(value = "/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/first-frames/upload",
             consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
public Result uploadFirstFrame(@PathVariable Long novelId,
                               @PathVariable Long chapterNum,
                               @PathVariable Long sceneId,
                               @RequestParam("file") MultipartFile file) {
    return Result.ok(storyboardFirstFrameService.upload(novelId, chapterNum, sceneId, file));
}
```

Write a `MockMvc` multipart test for successful forwarding and a missing-file 400 response.

- [ ] **Step 5: Run focused tests and verify pass**

Run: `mvn -q -Dtest=StoryboardFirstFrameServiceImplTest,VideoGenerationControllerTest test`

Expected: PASS.

### Task 3: Implement Final Video Upload and Current-Version Fallback

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/VideoGenerationService.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/controller/VideoGenerationController.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardVideoMapper.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImplTest.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/controller/VideoGenerationControllerTest.java`

**Interfaces:**
- Produces `VideoGenerationService#upload(Long novelId, Long chapterNum, Long sceneId, MultipartFile file): StoryboardVideoVO`.
- Produces `VideoGenerationService#delete(Long videoId): StoryboardVideoVO`, returning the promoted current video or `null`.
- Produces multipart `POST .../videos/upload` and `DELETE /api/storyboard-videos/{videoId}`.

- [ ] **Step 1: Write failing upload and fallback tests**

Add tests for:

```java
StoryboardVideoVO uploaded = service.upload(1L, 2L, 3L, mp4File);
assertEquals("UPLOAD", uploaded.getSource());
assertTrue(uploaded.getCurrent());
verify(videoMapper).clearCurrent(3L);
verify(videoMapper).insert(uploaded);

StoryboardVideoVO fallback = service.delete(42L);
verify(videoMapper).softDelete(42L);
verify(videoMapper).clearCurrent(3L);
verify(videoMapper).setCurrent(41L);
assertEquals(41L, fallback.getId());
```

Test `video/quicktime`, `video/webm`, and `video/mp4` uploads; reject an image, `.avi`, empty data, and files exceeding 500 MB without creating a database record.

- [ ] **Step 2: Run the focused service test and verify failure**

Run: `mvn -q -Dtest=VideoGenerationServiceImplTest test`

Expected: FAIL because upload/delete service methods do not exist.

- [ ] **Step 3: Implement video upload**

Declare:

```java
StoryboardVideoVO upload(Long novelId, Long chapterNum, Long sceneId, MultipartFile file);
StoryboardVideoVO delete(Long videoId);
```

Implement upload with:

```java
private static final long MAX_VIDEO_BYTES = 500L * 1024 * 1024;
private static final Set<String> VIDEO_EXTENSIONS = Set.of("mp4", "webm", "mov");
private static final Set<String> VIDEO_CONTENT_TYPES = Set.of(
    "video/mp4", "video/webm", "video/quicktime");
```

Validate extension/MIME/size and magic bytes before writing: MP4/MOV must contain an ISO Base Media `ftyp` box in the first 4 KB; WebM must start with EBML `1A 45 DF A3`. Store files under the existing `downloadDir/{novelId}/chapter-{chapterNum}/scene-{sceneId}/` root with a UUID filename while preserving the validated extension. Create a version with `source="UPLOAD"`, `current=true`, `firstFramePath=null`, `durationSec=null`, and empty prompt/style snapshots. Clear the old current version before insert. Delete the just-written file if database insertion fails.

Set `source="AI"` when `execute(...)` persists an AI-generated video.

- [ ] **Step 4: Implement logical deletion and current fallback**

Add mapper lookup `selectById(Long videoId)` returning scope and deletion state. Mark the requested version deleted, then in one `@Transactional` service method:

```java
videoMapper.softDelete(videoId);
videoMapper.clearCurrent(sceneId);
StoryboardVideoVO fallback = videoMapper.selectLatestAvailableByScene(sceneId);
if (fallback != null) {
    videoMapper.setCurrent(fallback.getId());
    fallback.setCurrent(true);
}
return fallback;
```

Reject missing/already-deleted videos with `视频版本不存在`. Do not remove its file or asset snapshot rows.

- [ ] **Step 5: Add controller routes and correct file content type**

Add the multipart video route and delete route. Update `file(...)` to use `Files.probeContentType(file)` with a safe `application/octet-stream` fallback rather than always returning `video/mp4`, so uploaded WebM and MOV preview correctly.

- [ ] **Step 6: Run focused tests and verify pass**

Run: `mvn -q -Dtest=VideoGenerationServiceImplTest,VideoGenerationControllerTest test`

Expected: PASS.

### Task 4: Add Upload Controls and Version Source Visibility

**Files:**
- Modify: `novel/src/main/resources/static/js/video-generation.js`
- Modify: `novel/src/main/resources/static/css/outline.css`
- Test: `novel/src/test/frontend/video-generation-page.test.js`

**Interfaces:**
- Consumes server routes from Tasks 2 and 3.
- Produces `uploadFirstFrame(card, scene, file)` and `uploadVideo(card, scene, file)` functions.
- Produces `data-action="upload-first-frame"`, `data-action="upload-video"`, hidden per-card file inputs, and source badges.

- [ ] **Step 1: Write failing page-contract assertions**

Add assertions:

```javascript
assert(script.includes('data-action="upload-first-frame"'));
assert(script.includes('data-action="upload-video"'));
assert(script.includes('/first-frames/upload'));
assert(script.includes('/videos/upload'));
assert(script.includes('FormData'));
assert(script.includes('手动上传'));
assert(script.includes('AI 生成'));
assert(script.includes('data-action="delete-video"'));
```

- [ ] **Step 2: Run the page-contract test and verify failure**

Run: `node novel/src/test/frontend/video-generation-page.test.js`

Expected: FAIL because upload controls do not exist.

- [ ] **Step 3: Render upload controls and hidden inputs**

In every scene card add two visible buttons and file inputs:

```html
<button type="button" class="secondary-button" data-action="upload-first-frame">上传首帧</button>
<input type="file" data-role="first-frame-upload" accept="image/png,image/jpeg,image/webp,.png,.jpg,.jpeg,.webp" hidden>
<button type="button" class="secondary-button" data-action="upload-video">上传视频</button>
<input type="file" data-role="video-upload" accept="video/mp4,video/webm,video/quicktime,.mp4,.webm,.mov" hidden>
```

Connect the visible buttons to `.click()` their matching input and connect each input `change` event to upload only its first selected file.

- [ ] **Step 4: Implement `FormData` upload flows**

For both uploads, disable only the initiating button, set `status` to an upload message, append the file as `file`, and do not set `Content-Type` manually:

```javascript
const formData = new FormData();
formData.append('file', file);
const response = await fetch(endpoint, {method: 'POST', body: formData});
```

After first-frame upload, set `selectedFirstFrames.set(String(scene.id), String(frame.id))`, refresh candidates, and show `首帧上传完成，已选中，可生成视频`. After video upload, refresh history and show `视频上传完成，已设为当前成片`. Reset `input.value = ''` in `finally` so the same file can be uploaded again as a new version.

- [ ] **Step 5: Render sources and video deletion**

Add a `mediaSourceLabel(source)` helper that returns `手动上传` only for `UPLOAD`, otherwise `AI 生成`. Display it on first-frame candidates and history rows. Add a delete button to each history row; call `DELETE /api/storyboard-videos/{videoId}`, then call `loadHistory(card, scene)` to render the service-selected fallback or empty result.

- [ ] **Step 6: Style the controls and labels**

Add compact `.media-source-badge`, `.media-source-upload`, and `.media-source-ai` classes next to existing first-frame styles. Keep buttons in the existing action line, use the current design system colors, and preserve readable wrapping at desktop widths.

- [ ] **Step 7: Run page-contract test and verify pass**

Run: `node novel/src/test/frontend/video-generation-page.test.js`

Expected: `video-generation page checks passed`.

### Task 5: Execute Migration and Complete Regression Verification

**Files:**
- Verify: `novel/src/main/resources/sql/alter_storyboard_media_upload.sql`
- Verify: `novel/src/test/java/com/novelgeneration/novel/service/impl/StoryboardFirstFrameServiceImplTest.java`
- Verify: `novel/src/test/java/com/novelgeneration/novel/service/impl/VideoGenerationServiceImplTest.java`
- Verify: `novel/src/test/java/com/novelgeneration/novel/controller/VideoGenerationControllerTest.java`
- Verify: `novel/src/test/frontend/video-generation-page.test.js`

**Interfaces:**
- Consumes all implementation tasks.
- Produces a migrated local MySQL schema and verified upload behavior.

- [ ] **Step 1: Inspect migration before execution**

Run: `Get-Content -Raw novel/src/main/resources/sql/alter_storyboard_media_upload.sql`

Expected: only idempotent column/index additions and `AI` backfill; no data-destructive statement.

- [ ] **Step 2: Execute the migration only after user approval**

Run with the project’s configured MySQL client: `source novel/src/main/resources/sql/alter_storyboard_media_upload.sql;`

Expected: `storyboard_first_frame.source`, `storyboard_video.source`, and `storyboard_video.is_deleted` exist; existing rows have `source='AI'`.

- [ ] **Step 3: Run backend test suite**

Run: `mvn -q test`

Expected: all tests pass with zero failures and errors.

- [ ] **Step 4: Run all frontend contract tests**

Run: `Get-ChildItem novel/src/test/frontend/*.test.js | ForEach-Object { node $_.FullName }`

Expected: every frontend test prints its pass marker.

- [ ] **Step 5: Check whitespace-only defects**

Run: `git diff --check`

Expected: no new whitespace errors introduced by this feature. Document any pre-existing unrelated output without modifying it.
