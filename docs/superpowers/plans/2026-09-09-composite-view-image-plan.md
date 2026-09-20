# Composite Three-View Image Generation Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add an asynchronous “生成三视图” action that combines the three saved prompts, calls an OpenAI-compatible image API, downloads one composite image locally, stores its path on the asset version, and displays it in the asset card.

**Architecture:** Reuse the existing visual-asset version model and polling style. Add a dedicated image-generation task table/service so long-running image generation does not block the request. The image client accepts an OpenAI-compatible `/images/generations` response and stores the downloaded file under a configurable local upload directory.

**Tech Stack:** Spring Boot 2.6, MyBatis annotations, MySQL 8, RestTemplate, vanilla JavaScript/CSS.

## Global Constraints

- Do not push or synchronize to Gitee unless the user explicitly requests it.
- Preserve the three independent prompt fields; only the generated image is one composite image.
- Persist the local image path on the visual-asset version.
- Use asynchronous task creation plus frontend polling.
- Keep API keys in ignored local configuration; never print them.

---

### Task 1: Persist Composite Image Metadata

**Files:**
- Modify: `novel/src/main/resources/sql/create_visual_asset_tables.sql`
- Modify: `novel/src/main/java/com/novelgeneration/novel/vo/VisualAssetVO.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/VisualAssetMapper.java`

- [ ] Add `composite_image_path` to the asset-version schema and expose it in current-version queries.
- [ ] Add `compositeImagePath` to the VO and update statements.
- [ ] Add an ALTER statement for existing databases.
- [ ] Add mapper coverage for the new field.

### Task 2: Image API Client and Local Download

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/service/CompositeImageService.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/CompositeImageServiceImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/NovelApplication.java`
- Modify: `novel/src/main/resources/application.properties.example`

- [ ] Add configurable image API URL, key, model, and local directory.
- [ ] Build one Chinese composite prompt from front/side/back prompts.
- [ ] Call the OpenAI-compatible image endpoint and extract `data[0].url`.
- [ ] Download the remote image to a generated local filename.
- [ ] Return the local path without logging credentials.

### Task 3: Asynchronous Image Task

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/vo/CompositeImageTaskVO.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/mapper/CompositeImageTaskMapper.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/controller/CompositeImageTaskController.java`
- Modify: `novel/src/main/resources/sql/create_visual_asset_tables.sql`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/VisualAssetServiceImpl.java`

- [ ] Add task table and statuses `PENDING`, `PROCESSING`, `COMPLETED`, `FAILED`.
- [ ] Add POST start and GET status endpoints.
- [ ] Claim tasks atomically and prevent duplicate generation for the same asset version.
- [ ] On success, save the local path to the asset version.
- [ ] On failure, persist a user-readable error message.

### Task 4: Frontend Button and Polling

**Files:**
- Modify: `novel/src/main/resources/static/js/novel-detail.js`
- Modify: `novel/src/main/resources/static/css/outline.css`

- [ ] Add “生成三视图” beside “保存修改”.
- [ ] Submit the latest three textarea values so unsaved edits are included.
- [ ] Poll task status and update the button/status text.
- [ ] Render the single composite image when `compositeImagePath` is returned.
- [ ] Keep prompt editing, save, and merge actions unchanged.

### Task 5: Verification

**Files:**
- Add focused service, mapper, and frontend syntax tests as appropriate.

- [ ] Run focused tests for prompt composition, response parsing, task claiming, and path persistence.
- [ ] Run `mvn -q test`.
- [ ] Run `node --check novel/src/main/resources/static/js/novel-detail.js`.
- [ ] Run `git diff --check` and confirm no configuration secrets are staged.

