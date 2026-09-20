# AI Assistant Read-Only Tools Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task with review checkpoints.

**Goal:** Add a read-only AI assistant that can answer questions about the current novel, chapters, storyboard, assets, and generation tasks through an explicit backend tool allowlist and a reusable contextual drawer in the workbench pages.

**Architecture:** The backend exposes `POST /api/assistant/chat`. A first AI JSON-object call selects one allowlisted read-only tool and arguments; the tool registry validates and executes existing mapper queries; a second AI call turns the tool result into a concise Chinese answer. The frontend injects a fixed assistant trigger and responsive right-side drawer, sending current URL context with each question.

**Tech Stack:** Spring Boot 2.6, MyBatis mapper queries, Jackson, JUnit 5/Mockito, vanilla HTML/CSS/JavaScript.

## Global Constraints

- Do not modify existing business APIs, database schema, or generation behavior.
- The assistant may only call explicitly registered read-only tools.
- Do not expose API keys, arbitrary SQL, write operations, generation operations, delete operations, or retry operations through the assistant.
- Preserve Chinese UI and responsive behavior.
- Run targeted Maven tests, frontend tests, package build, and `git diff --check` before claiming completion.

---

### Task 1: Define assistant contracts and read-only tool registry

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/dto/AssistantChatRequest.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/dto/AssistantChatResponse.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/AssistantService.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/AssistantReadOnlyToolRegistry.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/AssistantServiceImpl.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/AssistantReadOnlyToolRegistryTest.java`

**Interfaces:**
- `AssistantReadOnlyToolRegistry.execute(String toolName, Map<String,Object> arguments, AssistantChatRequest context)` returns a map with `tool`, `success`, `data`, and `source`.
- Registered tools: `getCurrentNovel`, `getNovelList`, `getNovelChapters`, `getChapterContent`, `getLatestStoryboard`, `getVisualAssets`, `getVideoTaskStatus`, `getAssetTaskStatus`.
- Unknown tools throw `IllegalArgumentException` and no mapper write method is reachable.

- [ ] Write a failing test asserting `getNovelChapters` returns chapter data and source context while an unknown tool is rejected.
- [ ] Run `mvn -Dtest=AssistantReadOnlyToolRegistryTest test` and observe the expected missing-class failure.
- [ ] Implement the DTOs, registry, mapper-backed read methods, and service interface.
- [ ] Run the focused test and make it pass.

### Task 2: Add assistant orchestration endpoint

**Files:**
- Create: `novel/src/main/java/com/novelgeneration/novel/controller/AssistantController.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/AssistantServiceImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/utils/StructuredOutputSchemas.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/controller/AssistantControllerTest.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/AssistantServiceImplTest.java`

**Interfaces:**
- `POST /api/assistant/chat` accepts `message`, optional `novelId`, `chapterNum`, `sceneId`, `taskId`, and `page`.
- Response includes `answer`, `readOnly=true`, `toolCalls`, and `sources`.
- The service performs a JSON-object planning call, validates tool/arguments, executes one read-only tool, then performs a final answer call. If no tool is needed, it answers directly; if AI planning fails, it returns a clear configuration/error response.

- [ ] Write failing tests for the controller delegation and service’s validated tool-plan flow.
- [ ] Run the focused tests and observe failure because the endpoint/service do not exist.
- [ ] Add assistant plan schema, orchestration, tool-call audit data, and endpoint.
- [ ] Run focused tests and then the existing structured-output regression suite.

### Task 3: Add reusable frontend assistant drawer

**Files:**
- Create: `novel/src/main/resources/static/studio/css/assistant-panel.css`
- Create: `novel/src/main/resources/static/studio/js/assistant-panel.js`
- Modify: `novel/src/main/resources/static/storyboard-workbench-new.html`
- Modify: `novel/src/main/resources/static/novel-detail-new.html`
- Modify: `novel/src/main/resources/static/asset-library-new.html`
- Modify: `novel/src/main/resources/static/video-generation-new.html`
- Test: `novel/src/test/frontend/assistant-panel.test.js`

**Interfaces:**
- Any page that loads `assistant-panel.js` gets a fixed “AI 助手” trigger and responsive drawer.
- The script reads `novelId`, `chapterNum`, `sceneId`, and `taskId` from the URL where available, posts to `/api/assistant/chat`, renders loading/empty/error/success states, and never mutates business data.
- Context chips show the current page and identifiers; suggested questions are read-only.

- [ ] Write a failing frontend test for injected trigger, drawer open, and request payload context.
- [ ] Run the frontend test and observe missing assistant script behavior.
- [ ] Implement CSS/JS and include the assets on the four current workbench pages.
- [ ] Run all frontend tests and inspect the assistant drawer in the running app.

### Task 4: Regression verification and handoff

**Files:**
- No new source files; verify all files above.

- [ ] Run `mvn -Dtest=AssistantReadOnlyToolRegistryTest,AssistantServiceImplTest,AssistantControllerTest,AssetExtractionServiceImplTest,StoryboardServiceImplTest,GenerateOutlineServiceImplTest,GenerateFullServiceImplTest,AiUtilStructuredOutputTest test`.
- [ ] Run the frontend test command discovered from the repository configuration.
- [ ] Run `mvn -DskipTests package`.
- [ ] Run `git diff --check` and inspect `git status --short`.
- [ ] Report the endpoint, integrated pages, read-only tools, and exact verification results.
