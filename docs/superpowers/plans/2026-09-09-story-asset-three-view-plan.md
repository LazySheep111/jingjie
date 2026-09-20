# 小说人物与场景三视图资产 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 在现有正文页中增加当前章节人物/场景资产提取、同小说跨章节复用、版本管理和三组视图提示词编辑能力。

**Architecture:** 新增小说级视觉资产库、资产版本、分镜资产关联和异步提取任务四类数据。后端由异步任务服务调用现有 `AiUtil`，先识别当前章节实体，再查询当前小说资产库，命中则复用，未命中则生成正面/侧面/背面提示词。前端保持三列布局，在第三列增加“分镜脚本”和“人物与场景资产”两个 Tab，并通过任务轮询恢复状态。

**Tech Stack:** Spring Boot 2.6.13, Java 17, MyBatis 注解 Mapper, MySQL, Jackson, Lombok, 原生 HTML/CSS/JavaScript, JUnit 5, Mockito。

## Global Constraints

- 第一版只生成和保存三视图提示词，不接入真实图片生成模型。
- 资产只在同一部小说内跨章节复用，不跨小说自动合并。
- 当前章节负责提取，整部小说资产库负责查询和复用。
- 同一人物或场景发生变化时创建新版本，不覆盖历史版本。
- 异步任务使用任务状态轮询，不新增 WebSocket 或 SSE 依赖。
- 页面只做桌面端布局，不新增手机端布局。
- 现有分镜生成、编辑、导出接口必须继续可用。
- 不读取、提交或输出本地 `application.properties` 中的 API Key 和数据库密码。

## File Map

- Create `novel/src/main/resources/sql/create_visual_asset_tables.sql`: 资产、资产版本、分镜关联和异步任务表。
- Create `novel/src/main/java/com/novelgeneration/novel/vo/VisualAssetVO.java`: 资产详情和三组提示词返回对象。
- Create `novel/src/main/java/com/novelgeneration/novel/vo/AssetTaskVO.java`: 提取任务状态返回对象。
- Create `novel/src/main/java/com/novelgeneration/novel/dto/AssetExtractRequest.java`: 当前章节资产提取请求。
- Create `novel/src/main/java/com/novelgeneration/novel/dto/AssetMergeRequest.java`: 资产合并请求。
- Create `novel/src/main/java/com/novelgeneration/novel/mapper/VisualAssetMapper.java`: MyBatis 查询、插入、更新和关联操作。
- Create `novel/src/main/java/com/novelgeneration/novel/service/VisualAssetService.java`: 资产查询、编辑、合并和章节关联接口。
- Create `novel/src/main/java/com/novelgeneration/novel/service/AssetExtractionService.java`: 异步任务启动、状态查询和任务执行接口。
- Create `novel/src/main/java/com/novelgeneration/novel/service/impl/VisualAssetServiceImpl.java`: 资产业务实现。
- Create `novel/src/main/java/com/novelgeneration/novel/service/impl/AssetExtractionServiceImpl.java`: AI 识别、去重、提示词生成和任务状态实现。
- Create `novel/src/main/java/com/novelgeneration/novel/controller/VisualAssetController.java`: 资产相关 REST API。
- Create `novel/src/main/java/com/novelgeneration/novel/controller/AssetTaskController.java`: 任务状态 REST API。
- Modify `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardVO.java`: 为分镜返回资产 ID 关联。
- Modify `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardMapper.java`: 查询和保存分镜资产关联。
- Modify `novel/src/main/resources/static/novel-detail.html`: 增加第三列 Tab、提取按钮和资产展示容器。
- Modify `novel/src/main/resources/static/js/novel-detail.js`: 增加资产任务轮询、资产加载、编辑、合并和渲染逻辑。
- Modify `novel/src/main/resources/static/css/outline.css`: 增加资产 Tab、资产卡片、三视图占位和状态样式。
- Create `novel/src/test/java/com/novelgeneration/novel/service/impl/VisualAssetServiceImplTest.java`: 资产复用、版本和合并测试。
- Create `novel/src/test/java/com/novelgeneration/novel/service/impl/AssetExtractionServiceImplTest.java`: 异步任务和 AI 解析测试。
- Create `novel/src/test/java/com/novelgeneration/novel/mapper/VisualAssetMapperParameterTest.java`: 多参数 `@Param` 回归测试。
- Modify `novel/src/test/java/com/novelgeneration/novel/service/impl/StoryboardServiceImplTest.java`: 验证分镜资产关联不破坏原有流程。

---

### Task 1: 建立视觉资产数据库结构

**Files:**
- Create: `novel/src/main/resources/sql/create_visual_asset_tables.sql`
- Test: 使用 MySQL 执行脚本，并检查唯一键、外键和重复插入行为。

**Interfaces:**
- Produces tables consumed by `VisualAssetMapper` and `AssetExtractionServiceImpl`.

- [ ] **Step 1: Write the schema script**

创建四张表：

```sql
CREATE TABLE IF NOT EXISTS novel_visual_asset (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    novel_id BIGINT NOT NULL,
    asset_type VARCHAR(32) NOT NULL,
    normalized_name VARCHAR(255) NOT NULL,
    display_name VARCHAR(255) NOT NULL,
    current_version INT NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'PROMPT_READY',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_visual_asset_identity (novel_id, asset_type, normalized_name),
    KEY idx_visual_asset_novel (novel_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS novel_visual_asset_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    asset_id BIGINT NOT NULL,
    version INT NOT NULL,
    core_features TEXT,
    front_prompt TEXT,
    side_prompt TEXT,
    back_prompt TEXT,
    status VARCHAR(32) NOT NULL DEFAULT 'PROMPT_READY',
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_visual_asset_version (asset_id, version),
    CONSTRAINT fk_visual_asset_version_asset FOREIGN KEY (asset_id)
        REFERENCES novel_visual_asset(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS novel_storyboard_asset_ref (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    storyboard_scene_id BIGINT NOT NULL,
    asset_id BIGINT NOT NULL,
    asset_version INT NOT NULL,
    asset_role VARCHAR(32) NOT NULL,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_storyboard_scene_asset (storyboard_scene_id, asset_id, asset_version, asset_role),
    CONSTRAINT fk_storyboard_asset_ref_scene FOREIGN KEY (storyboard_scene_id)
        REFERENCES novel_storyboard_scene(id) ON DELETE CASCADE,
    CONSTRAINT fk_storyboard_asset_ref_asset FOREIGN KEY (asset_id)
        REFERENCES novel_visual_asset(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE IF NOT EXISTS novel_asset_extract_task (
    id VARCHAR(64) PRIMARY KEY,
    novel_id BIGINT NOT NULL,
    chapter_num BIGINT NOT NULL,
    status VARCHAR(32) NOT NULL,
    total INT NOT NULL DEFAULT 0,
    reused INT NOT NULL DEFAULT 0,
    created INT NOT NULL DEFAULT 0,
    failed INT NOT NULL DEFAULT 0,
    error_message TEXT,
    create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    KEY idx_asset_task_chapter (novel_id, chapter_num, status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

- [ ] **Step 2: Add duplicate-task prevention index**

在任务表增加一个应用层约束：同一 `novelId + chapterNum` 只允许一个 `PENDING` 或 `PROCESSING` 任务。数据库层通过任务查询加事务锁实现，不使用会阻止历史任务保存的全局唯一键。

- [ ] **Step 3: Execute and verify the SQL**

运行：

```bash
mysql -u <username> -p novel_db < novel/src/main/resources/sql/create_visual_asset_tables.sql
```

验证：

```sql
SHOW TABLES LIKE 'novel_visual_asset%';
SHOW TABLES LIKE 'novel_storyboard_asset_ref';
SHOW TABLES LIKE 'novel_asset_extract_task';
SHOW INDEX FROM novel_visual_asset;
```

预期：四张表存在，资产身份唯一键存在，重复插入相同 `novel_id + asset_type + normalized_name` 被拒绝。

- [ ] **Step 4: Commit**

```bash
git add novel/src/main/resources/sql/create_visual_asset_tables.sql
git commit -m "feat: add visual asset tables"
```

### Task 2: 建立资产领域对象和 Mapper

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/vo/VisualAssetVO.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/vo/AssetTaskVO.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/dto/AssetExtractRequest.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/dto/AssetMergeRequest.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/mapper/VisualAssetMapper.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardVO.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardMapper.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/mapper/VisualAssetMapperParameterTest.java`

**Interfaces:**
- `VisualAssetVO` exposes `assetId`, `novelId`, `assetType`, `assetName`, `version`, `coreFeatures`, `frontPrompt`, `sidePrompt`, `backPrompt`, `status`, and `reuseCount`.
- `AssetTaskVO` exposes `taskId`, `novelId`, `chapterNum`, `status`, `total`, `reused`, `created`, `failed`, `errorMessage`, `createTime`, and `updateTime`.
- `AssetExtractRequest` accepts `chapterNum` and optional `forceRetry`.
- `AssetMergeRequest` accepts `targetAssetId` and `targetVersion`.

- [ ] **Step 1: Write the value objects**

Use Lombok `@Data`; IDs are `Long`, chapter number is `Long`, counters are `Integer`, and timestamps are `LocalDateTime`. Use `List<Long> characterAssetIds` and `List<Long> locationAssetIds` in the storyboard scene association object.

- [ ] **Step 2: Write the Mapper method signatures**

```java
VisualAssetVO selectAsset(Long novelId, String assetType, String normalizedName);
List<VisualAssetVO> selectAssets(Long novelId, String assetType);
VisualAssetVO selectAssetVersion(Long assetId, Integer version);
int insertAsset(VisualAssetVO asset);
int insertAssetVersion(VisualAssetVO asset);
int updateAsset(VisualAssetVO asset);
int updateAssetVersion(VisualAssetVO asset);
int insertSceneAssetRef(Long sceneId, Long assetId, Integer version, String assetRole);
List<VisualAssetVO> selectAssetsByChapter(Long novelId, Long chapterNum);
int countAssetReferences(Long assetId);
AssetTaskVO selectRunningTask(Long novelId, Long chapterNum);
int insertTask(AssetTaskVO task);
int updateTask(AssetTaskVO task);
```

Every method with more than one parameter must use explicit MyBatis `@Param`, for example:

```java
VisualAssetVO selectAsset(@Param("novelId") Long novelId,
                          @Param("assetType") String assetType,
                          @Param("normalizedName") String normalizedName);
```

- [ ] **Step 3: Write the parameter regression test**

Add a test that inspects the mapper methods with multiple arguments and verifies the SQL references the declared names `novelId`, `assetType`, `normalizedName`, `chapterNum`, and `assetId`. The test must fail if a future edit removes `@Param`.

- [ ] **Step 4: Run focused tests**

```bash
mvn -q -Dtest=VisualAssetMapperParameterTest test
```

Expected: PASS after MyBatis parameter names are explicit.

- [ ] **Step 5: Commit**

```bash
git add novel/src/main/java/com/novelgeneration/novel/vo novel/src/main/java/com/novelgeneration/novel/dto novel/src/main/java/com/novelgeneration/novel/mapper novel/src/test/java/com/novelgeneration/novel/mapper/VisualAssetMapperParameterTest.java
git commit -m "feat: add visual asset domain objects"
```

### Task 3: Implement asset CRUD, versioning, reuse and merge logic

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/service/VisualAssetService.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/VisualAssetServiceImpl.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/controller/VisualAssetController.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/VisualAssetServiceImplTest.java`

**Interfaces:**
- `List<VisualAssetVO> list(Long novelId, String assetType)`
- `VisualAssetVO get(Long novelId, Long assetId)`
- `VisualAssetVO update(Long novelId, Long assetId, VisualAssetVO request)`
- `VisualAssetVO merge(Long novelId, Long assetId, AssetMergeRequest request)`
- `List<VisualAssetVO> listByChapter(Long novelId, Long chapterNum)`
- `VisualAssetVO findOrCreate(Long novelId, String assetType, String displayName, String normalizedName, String coreFeatures)`
- `VisualAssetVO createNextVersion(Long novelId, Long assetId, String coreFeatures, String frontPrompt, String sidePrompt, String backPrompt)`

- [ ] **Step 1: Write failing service tests**

Cover these exact cases:

```java
@Test
void findOrCreate_reusesExistingAssetWithinSameNovel() {}

@Test
void findOrCreate_doesNotReuseAssetFromAnotherNovel() {}

@Test
void createNextVersion_keepsPreviousVersionUnchanged() {}

@Test
void merge_movesFutureAssociationsToTargetAndMarksSourceMerged() {}

@Test
void update_rejectsAssetFromAnotherNovel() {}
```

The tests must verify mapper calls and returned version/status values.

- [ ] **Step 2: Implement normalization**

Normalize only for identity lookup: trim, collapse consecutive whitespace, and lowercase Latin characters. Preserve the original display name. Do not remove Chinese characters or use fuzzy matching as the database identity rule.

- [ ] **Step 3: Implement reuse and version creation**

`findOrCreate` first queries by `novelId + assetType + normalizedName`. On a hit, return the current version. On a miss, insert the asset with `currentVersion = 1`, then insert its first version with the supplied features and empty prompts.

`createNextVersion` increments the asset version and inserts a new version record without modifying the old version row.

- [ ] **Step 4: Implement merge behavior**

Validate source and target belong to the same `novelId`. Update all scene references from the source asset to the target asset while preserving the target version selected by the request. Mark the source asset `MERGED`, keep its rows for historical traceability, and reject merging an asset into itself.

- [ ] **Step 5: Implement REST endpoints**

```text
GET  /api/novel/{novelId}/assets?assetType=CHARACTER
GET  /api/novel/{novelId}/assets/{assetId}
PUT  /api/novel/{novelId}/assets/{assetId}
POST /api/novel/{novelId}/assets/{assetId}/merge
GET  /api/novel/{novelId}/chapters/{chapterNum}/assets
```

Use HTTP 400 for invalid IDs or merge requests, HTTP 404 for assets outside the novel or not found, and HTTP 200 with the updated object for successful edits.

- [ ] **Step 6: Run focused tests and commit**

```bash
mvn -q -Dtest=VisualAssetServiceImplTest test
git add novel/src/main/java/com/novelgeneration/novel/service novel/src/main/java/com/novelgeneration/novel/controller/VisualAssetController.java novel/src/test/java/com/novelgeneration/novel/service/impl/VisualAssetServiceImplTest.java
git commit -m "feat: add visual asset reuse and versioning"
```

### Task 4: Implement asynchronous AI extraction and prompt generation

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/service/AssetExtractionService.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/AssetExtractionServiceImpl.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/controller/AssetTaskController.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/AssetExtractionServiceImplTest.java`

**Interfaces:**
- `AssetTaskVO start(Long novelId, Long chapterNum, AssetExtractRequest request)`
- `AssetTaskVO getStatus(String taskId)`
- `void execute(String taskId)`
- `List<VisualAssetVO> parseEntities(String aiResponse)`
- `VisualAssetVO generatePrompts(VisualAssetVO entity)`

- [ ] **Step 1: Define strict AI response JSON**

The first AI call must return only:

```json
{
  "entities": [
    {
      "assetType": "CHARACTER",
      "assetName": "凌程",
      "coreFeatures": "年轻工程师、黑发、深色短衫"
    },
    {
      "assetType": "LOCATION",
      "assetName": "玉带河",
      "coreFeatures": "乡村河流、傍晚、浅滩"
    }
  ]
}
```

The second AI call for a new entity must return only:

```json
{
  "frontPrompt": "正面三视图提示词",
  "sidePrompt": "侧面三视图提示词",
  "backPrompt": "背面三视图提示词"
}
```

- [ ] **Step 2: Write failing extraction tests**

Cover:

```java
@Test
void start_returnsExistingRunningTaskForSameNovelAndChapter() {}

@Test
void parseEntities_acceptsJsonObjectAndRejectsEmptyEntities() {}

@Test
void execute_reusesExistingAssetsAndCreatesOnlyNewEntities() {}

@Test
void execute_marksPartialFailedWhenOneEntityPromptFails() {}

@Test
void execute_persistsThreeIndependentPromptsForNewAsset() {}
```

- [ ] **Step 3: Implement task lifecycle**

Generate a UUID task ID, insert `PENDING`, and launch execution through a bounded executor. Before creating a new task, query `selectRunningTask`. Return the existing task when the status is `PENDING` or `PROCESSING`.

Use a bounded executor with a fixed pool of 2 threads. Do not create an unbounded thread per request.

- [ ] **Step 4: Implement entity extraction prompt**

Build a prompt that includes the current chapter title, chapter text, and current storyboard scene text. Require `assetType`, `assetName`, and `coreFeatures`, and instruct the AI to return strict JSON only.

- [ ] **Step 5: Implement reuse and prompt generation**

For each parsed entity:

1. Normalize the identity.
2. Query the current novel asset table.
3. If found, increment the reused counter and attach the current asset version to matching storyboard scenes.
4. If not found, call the prompt-generation AI request, persist the three prompt fields, increment created, and attach the new version.
5. If one entity fails, increment failed and continue processing remaining entities.

- [ ] **Step 6: Implement task REST endpoints**

```text
POST /api/novel/{novelId}/chapters/{chapterNum}/assets/extract
GET  /api/asset-tasks/{taskId}
```

The start endpoint returns HTTP 202 with `AssetTaskVO`. The status endpoint returns HTTP 200. A completed task includes `total`, `reused`, `created`, and `failed`.

- [ ] **Step 7: Run focused tests and commit**

```bash
mvn -q -Dtest=AssetExtractionServiceImplTest test
git add novel/src/main/java/com/novelgeneration/novel/service/AssetExtractionService.java novel/src/main/java/com/novelgeneration/novel/service/impl/AssetExtractionServiceImpl.java novel/src/main/java/com/novelgeneration/novel/controller/AssetTaskController.java novel/src/test/java/com/novelgeneration/novel/service/impl/AssetExtractionServiceImplTest.java
git commit -m "feat: add async asset extraction"
```

### Task 5: Add asset associations to storyboard data

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/vo/StoryboardVO.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardMapper.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardServiceImpl.java`
- Modify: `novel/src/main/resources/static/js/novel-detail.js`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/StoryboardServiceImplTest.java`

**Interfaces:**
- Every storyboard scene returns `characterAssetIds` and `locationAssetIds`.
- `StoryboardServiceImpl` can load scene asset references without changing existing storyboard JSON fields.

- [ ] **Step 1: Write the regression test**

Extend the existing storyboard service test to verify that loading a storyboard calls the asset-reference query and returns the IDs on each scene. Existing scene text fields and export output must remain unchanged.

- [ ] **Step 2: Add association fields**

Add to `StoryboardVO.Scene`:

```java
private List<Long> characterAssetIds = new ArrayList<>();
private List<Long> locationAssetIds = new ArrayList<>();
```

- [ ] **Step 3: Add Mapper association methods**

```java
List<Long> selectAssetIds(Long sceneId, String assetRole);
int deleteAssetRefs(Long sceneId);
int insertAssetRef(Long sceneId, Long assetId, Integer version, String assetRole);
```

Add `@Param` to every multi-parameter method.

- [ ] **Step 4: Load and persist associations**

After loading each scene, query its character and location IDs. When the extraction service attaches assets, insert references using the selected asset version. Do not copy prompt text into storyboard scene rows.

- [ ] **Step 5: Run regression tests and commit**

```bash
mvn -q -Dtest=StoryboardServiceImplTest,StoryboardMapperParameterTest test
git add novel/src/main/java/com/novelgeneration/novel/vo/StoryboardVO.java novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardMapper.java novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardServiceImpl.java novel/src/test/java/com/novelgeneration/novel/service/impl/StoryboardServiceImplTest.java
git commit -m "feat: link storyboard scenes to visual assets"
```

### Task 6: Implement the desktop frontend asset experience

**Files:**
- Modify: `novel/src/main/resources/static/novel-detail.html`
- Modify: `novel/src/main/resources/static/js/novel-detail.js`
- Modify: `novel/src/main/resources/static/css/outline.css`

**Interfaces:**
- Uses the task endpoints from Task 4 and asset endpoints from Task 3.
- Reads `characterAssetIds` and `locationAssetIds` from storyboard scenes.
- Preserves the current storyboard generation, save, export, chapter save, and chapter regeneration flows.

- [ ] **Step 1: Add the third-column tabs and containers**

Keep the existing three-column grid. Inside `.storyboard-panel`, add:

```html
<div class="asset-tabs">
    <button id="storyboardTabBtn" type="button" class="active">分镜脚本</button>
    <button id="assetTabBtn" type="button">人物与场景资产</button>
</div>
<section id="storyboardTabPanel"></section>
<section id="assetTabPanel" hidden>
    <button id="extractAssetsBtn" type="button">提取人物/场景</button>
    <span id="assetTaskSummary"></span>
    <div id="assetFilters"></div>
    <div id="assetList"></div>
</section>
```

- [ ] **Step 2: Add asset task state to JavaScript**

Add module-level state:

```javascript
let assetTaskTimer = null;
let activeAssetTaskId = null;
let assetList = [];
```

Clear `assetTaskTimer` when changing novels or chapters. Do not start a second interval if one already exists for the same task ID.

- [ ] **Step 3: Implement extraction and polling**

`extractAssets()` must:

1. Validate `novelId`, current chapter number, and current storyboard existence.
2. Disable the extraction button and show `正在提交资产提取任务...`.
3. POST to `/api/novel/{novelId}/chapters/{chapterNum}/assets/extract`.
4. Store the returned `taskId`.
5. Poll `/api/asset-tasks/{taskId}` every 1800 ms.
6. Stop polling on `COMPLETED`, `PARTIAL_FAILED`, or `FAILED`.
7. Refresh chapter assets and the storyboard association display after completion.

- [ ] **Step 4: Render asset cards and three-view placeholders**

Each card must show asset name, type, version, core features, reuse count, and three editable textareas:

```html
<textarea data-view="frontPrompt"></textarea>
<textarea data-view="sidePrompt"></textarea>
<textarea data-view="backPrompt"></textarea>
```

Each view also displays `图片待生成` until a future image URL exists. Existing assets show `已复用`; new assets show `提示词已生成`.

- [ ] **Step 5: Implement edit and merge actions**

Add buttons for `保存修改` and `合并到已有资产`. Save with `PUT`; merge only after `window.confirm` confirmation, then reload the chapter asset list and storyboard. Do not transmit API keys or local configuration values from the browser.

- [ ] **Step 6: Render storyboard asset chips**

For each storyboard scene, render `人物：名称 v版本` and `场景：名称 v版本` chips using the loaded asset map. If an association is missing, show `未关联资产` without failing the storyboard card.

- [ ] **Step 7: Add desktop styles**

Add styles for `.asset-tabs`, `.asset-card`, `.asset-view-grid`, `.asset-view-placeholder`, `.asset-status`, `.asset-chip`, and `.asset-task-status`. Keep the existing three-column widths and use the right panel's existing scroll behavior.

- [ ] **Step 8: Run a browser smoke test and commit**

Verify manually:

```text
1. Open a novel detail page.
2. Select a chapter with saved storyboard data.
3. Open “人物与场景资产”.
4. Click “提取人物/场景”.
5. Observe PROCESSING status and completion summary.
6. Confirm reused assets are not duplicated.
7. Edit one prompt and save.
8. Reload the page and confirm the edit remains.
9. Return to “分镜脚本” and confirm asset chips are visible.
```

Then run:

```bash
git add novel/src/main/resources/static/novel-detail.html novel/src/main/resources/static/js/novel-detail.js novel/src/main/resources/static/css/outline.css
git commit -m "feat: add visual asset panel to novel reader"
```

### Task 7: Full verification and documentation

**Files:**
- Modify: `docs/superpowers/specs/2026-09-09-story-asset-three-view-design.md` only if implementation behavior differs from the approved design.
- Test: all Java tests and the browser smoke test from Task 6.

- [ ] **Step 1: Run the complete test suite**

```bash
mvn -q test
```

Expected: all tests pass. If Maven cannot download dependencies, record the exact dependency/network error and do not claim the suite passed.

- [ ] **Step 2: Verify SQL and API behavior**

Use Postman or curl with a real local `novelId` and `chapterNum`:

```bash
curl -i -X POST "http://localhost:8081/api/novel/1/chapters/1/assets/extract" -H "Content-Type: application/json" -d "{\"chapterNum\":1}"
curl -i "http://localhost:8081/api/asset-tasks/<taskId>"
curl -i "http://localhost:8081/api/novel/1/chapters/1/assets"
```

Expected: first response is HTTP 202, task reaches a terminal status, and chapter asset response contains reused/new assets with three prompts.

- [ ] **Step 3: Check secret safety**

```bash
git grep -n -I -E "sk-[A-Za-z0-9]+|spring\.datasource\.password=[^$]" -- ':!*.example' ':!novel/src/test/resources/application.properties'
```

Expected: no production API key or database password in the new commits. Existing remote history may still contain old secrets and must be handled by rotating them outside the codebase.

- [ ] **Step 4: Review changed files and status**

```bash
git diff --check
git status --short
```

Expected: no whitespace errors; only intentional implementation files are changed.

- [ ] **Step 5: Commit documentation updates if needed**

```bash
git add docs/superpowers/specs/2026-09-09-story-asset-three-view-design.md
git commit -m "docs: align visual asset design with implementation"
```

## Plan Self-Review

- Spec coverage: goals, non-goals, three-column layout, same-novel reuse, versioning, async task states, APIs, editable prompts, merge behavior, error handling, and verification are covered by Tasks 1 through 7.
- Placeholder scan: no unresolved placeholder markers or vague implementation steps are used.
- Type consistency: `Long novelId`, `Long chapterNum`, `String taskId`, `Integer version`, `VisualAssetVO`, and `AssetTaskVO` are used consistently across Mapper, service, controller, and frontend contracts.
- Existing storyboard behavior: Task 5 explicitly preserves the current storyboard endpoints and export fields.
- Secret handling: the plan explicitly excludes local production configuration and requires a staged-file secret scan.
