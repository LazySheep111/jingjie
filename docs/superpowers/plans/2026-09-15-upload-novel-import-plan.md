# Upload Novel Import Implementation Plan

> **For agentic workers:** Implement inline in this session with focused tests after each task.

**Goal:** Allow users to upload TXT or DOCX novels, extract and split their chapters, save them as normal novel chapters, and reuse the existing chapter storyboard generation flow.

**Architecture:** Add a synchronous import service for the first version. The controller accepts a multipart file and novel title, the service parses TXT/DOCX into chapter records, and the existing chapter/storyboard APIs remain the source of truth after import. Store the original file under the configured upload directory for traceability.

**Tech Stack:** Spring Boot 2.6, Spring MVC `MultipartFile`, MyBatis annotations, Java ZIP/XML parsing for DOCX, UTF-8/GBK text decoding, existing static HTML/JavaScript UI.

## Global Constraints

- First version supports `.txt` and `.docx`; PDF/OCR is out of scope.
- Do not send the uploaded file directly to the AI; only extracted chapter text is sent through the existing storyboard generation endpoint.
- Preserve existing generated-novel flows and do not sync or commit to Gitee.
- Reject empty files, unsupported extensions, and novels with no detectable content.

### Task 1: Parser and importer service

**Files:** Create `novel/src/main/java/com/novelgeneration/novel/service/NovelImportService.java`, `novel/src/main/java/com/novelgeneration/novel/service/impl/NovelImportServiceImpl.java`, DTO/VO files, and focused service tests.

- Parse TXT as UTF-8 first, falling back to GBK when replacement characters dominate.
- Parse DOCX `word/document.xml` using `ZipFile` and namespace-aware DOM; preserve paragraph breaks.
- Detect headings matching `第...章`, `第...节`, `Chapter N`, or `CHAPTER N`; when no headings exist, create one chapter.
- Save source file under `uploads/novels/{novelId}/source/` and insert `novel_info` plus `novel_chapter_text` records.

### Task 2: Database mapper and controller

**Files:** Modify `NovelTextMapper`, add import mapper methods, add `NovelImportController`, and add parameter/controller tests.

- Insert imported novel metadata and chapter text using existing tables.
- Expose `POST /api/novel/import` with multipart field `file` and optional `novelTitle`.
- Return `novelId`, title, chapter count, and chapter summaries for redirect/preview.

### Task 3: Upload UI

**Files:** Modify the creation page HTML/CSS/JS and add frontend tests.

- Add an “上传小说” section alongside AI creation.
- Accept `.txt,.docx`, show upload status, and redirect to `novel-detail.html?novelId=...` after success.
- Keep current chapter selection and “生成分镜脚本” behavior unchanged; uploaded chapters use the same storyboard API.

### Task 4: Verification

- Run JavaScript syntax and static page tests.
- Run focused Maven tests for parser, mapper, and controller.
- Run `git diff --check`; do not execute any database migration unless separately requested.
