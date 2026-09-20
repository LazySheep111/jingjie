# Model Management Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add a secure, database-backed model management page for text, image, and video AI providers without breaking the existing property-based configuration.

**Architecture:** Add a versioned `ai_model_config` table and a service that encrypts API keys, exposes masked view data, tests configurations without persisting them, and resolves the active configuration for each AI capability. Existing AI clients will consume this resolver and fall back to their current `application.properties` values when no database configuration is enabled. Add a standalone model management page and a bottom-pinned sidebar entry to the shared workbench shell.

**Tech Stack:** Spring Boot 2.6.13, Java, MyBatis annotation mappers, MySQL, Jackson, JDK AES-GCM, vanilla HTML/CSS/JavaScript, Node-based frontend smoke tests, Maven/Surefire.

## Global Constraints

- The page must expose fixed capability slots for text, image, and video models.
- API Key must be encrypted at rest and never returned in full by an API.
- The model management page is `/model-management.html` and its navigation entry is the last sidebar item.
- Text uses a short real request for connection testing; image and video tests must not submit generation jobs.
- A failed test or save must not replace the currently enabled configuration.
- Database configuration takes precedence; when absent, existing `application.properties` behavior remains unchanged.
- Existing creative workflows and provider-specific Ark/MiniMax request formats must remain functional.
- Preserve unrelated working-tree changes and stage only files belonging to this feature in each commit.

## File Map

Create:

- `novel/src/main/resources/sql/create_ai_model_config_tables.sql` — versioned configuration table.
- `novel/src/main/java/com/novelgeneration/novel/entity/AiModelConfig.java` — persistence entity with encrypted secret.
- `novel/src/main/java/com/novelgeneration/novel/dto/AiModelConfigRequest.java` — write/test payload.
- `novel/src/main/java/com/novelgeneration/novel/vo/AiModelConfigVO.java` — masked response model.
- `novel/src/main/java/com/novelgeneration/novel/vo/AiModelConfigSnapshot.java` — internal resolved configuration containing the decrypted key.
- `novel/src/main/java/com/novelgeneration/novel/mapper/AiModelConfigMapper.java` — MyBatis queries and version writes.
- `novel/src/main/java/com/novelgeneration/novel/service/AiModelConfigService.java` — management and test operations.
- `novel/src/main/java/com/novelgeneration/novel/service/ModelConfigResolver.java` — active-config lookup for AI clients.
- `novel/src/main/java/com/novelgeneration/novel/service/impl/AiModelConfigServiceImpl.java` — encryption, validation, versioning, and provider probes.
- `novel/src/main/java/com/novelgeneration/novel/service/impl/ModelConfigResolverImpl.java` — database-first/property-fallback resolution.
- `novel/src/main/java/com/novelgeneration/novel/utils/ApiKeyEncryptor.java` — AES-GCM encryption/decryption and masking.
- `novel/src/main/java/com/novelgeneration/novel/controller/AiModelConfigController.java` — REST endpoints.
- `novel/src/main/resources/static/model-management.html` — standalone management page.
- `novel/src/main/resources/static/js/model-management.js` — load, test, save, disable, and status rendering.
- `novel/src/main/resources/static/css/model-management.css` — dense responsive model cards.
- `novel/src/test/java/com/novelgeneration/novel/utils/ApiKeyEncryptorTest.java` — encryption and masking tests.
- `novel/src/test/java/com/novelgeneration/novel/service/impl/AiModelConfigServiceImplTest.java` — service and test-probe tests.
- `novel/src/test/java/com/novelgeneration/novel/service/impl/ModelConfigResolverImplTest.java` — precedence and fallback tests.
- `novel/src/test/java/com/novelgeneration/novel/controller/AiModelConfigControllerTest.java` — endpoint payload and error tests.
- `novel/src/test/frontend/model-management-page.test.js` — static page contract checks.

Modify:

- `novel/src/main/resources/static/studio/js/workbench-shell.js` — append the model management entry as a bottom-pinned navigation link.
- `novel/src/main/resources/static/studio/css/workbench-shell.css` — style the bottom-pinned model management entry and small-screen placement.
- `novel/src/test/frontend/workbench-shell-page.test.js` — assert the new route, label, and final navigation position.
- `novel/src/main/java/com/novelgeneration/novel/utils/AiUtil.java` — resolve the text model dynamically.
- `novel/src/main/java/com/novelgeneration/novel/service/impl/CompositeImageServiceImpl.java` — resolve the image model dynamically.
- `novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredStoryboardFrameGenerator.java` — resolve the image model dynamically for first-frame generation.
- `novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClient.java` — resolve provider, endpoint, query URL, key, and model dynamically.
- `novel/src/main/resources/application.properties.example` — document `ai.config.encryption-key=${AI_CONFIG_ENCRYPTION_KEY:}` without adding a real secret.

## Task 1: Add the bottom-pinned navigation entry

**Files:**

- Modify: `novel/src/main/resources/static/studio/js/workbench-shell.js:10-20`
- Modify: `novel/src/main/resources/static/studio/css/workbench-shell.css:48-75`
- Test: `novel/src/test/frontend/workbench-shell-page.test.js`

**Interfaces:**

- Consumes: the existing `data-shell-page` and `navItems` rendering loop.
- Produces: a `model-management.html` link with label `模型管理`, key `model`, rendered after `资产库` and pinned below the main navigation group.

- [ ] **Step 1: Write the failing test.**

Read `workbench-shell.js` in `workbench-shell-page.test.js` and add these ordered markers after the existing video marker:

```js
const navOrder = [
    "['/outline', '小说大纲工作台', 'outline']",
    "['/novel-detail.html'",
    "'小说全文工作台', 'detail']",
    "['/history-list.html', '我的历史作品', 'history']",
    "['/storyboard-workbench.html'",
    "'分镜脚本工作台', 'storyboard']",
    "['/video-generation.html'",
    "'视频创作工作台', 'video']",
    "['/asset-library.html', '资产库', 'assets']",
    "['/model-management.html', '模型管理', 'model']"
];
```

Also assert `model-management.html` is included in the page list and loads the shared shell assets.

- [ ] **Step 2: Run the focused test and verify it fails.**

Run:

```powershell
D:\develop\node.exe -e "const fs=require('fs'),path=require('path'); const file=path.resolve('novel/src/test/frontend/workbench-shell-page.test.js'); new Function('require','__dirname','__filename',fs.readFileSync(file,'utf8'))(require,path.dirname(file),file);"
```

Expected: `AssertionError` for the missing `模型管理` navigation marker.

- [ ] **Step 3: Implement the minimal navigation change.**

Keep the existing six workbench items unchanged and append:

```js
['/asset-library.html', '资产库', 'assets'],
['/model-management.html', '模型管理', 'model']
```

Render the model link separately from `.workbench-nav-links` so it is visually pinned at the bottom:

```js
nav.innerHTML = `<a class="workbench-brand" href="/outline"><strong>镜界</strong><small>AI 视频创作工作台</small></a><nav class="workbench-nav-links">${navItems.filter(([, , key]) => key !== 'model').map(([href, label, key]) => `<a class="${page === key ? 'active' : ''}" href="${href}">${label}</a>`).join('')}</nav><a class="workbench-nav-model-link ${page === 'model' ? 'active' : ''}" href="/model-management.html">模型管理</a><p class="workbench-nav-note">从故事到镜头，再到可生成的视频输出。</p>`;
```

Add:

```css
.workbench-nav-model-link {
    display: flex;
    min-height: 44px;
    align-items: center;
    gap: 10px;
    margin-top: auto;
    padding: 0 12px;
    border-radius: 9px;
    color: #bdd0d7;
    text-decoration: none;
}
.workbench-nav-model-link:hover,
.workbench-nav-model-link:focus-visible,
.workbench-nav-model-link.active { background: #1b5368; color: #fff; }
```

For screens at or below 720px, remove `margin-top:auto` and keep the entry after the horizontal navigation links so it remains reachable without taking over the viewport.

- [ ] **Step 4: Run the focused test and verify it passes.**

Run the same Node command. Expected: `workbench shell page checks passed`.

- [ ] **Step 5: Commit only this task.**

```powershell
git add -- novel/src/main/resources/static/studio/js/workbench-shell.js novel/src/main/resources/static/studio/css/workbench-shell.css novel/src/test/frontend/workbench-shell-page.test.js
git commit -m "feat: add model management navigation"
```

## Task 2: Add encrypted, versioned model configuration storage

**Files:**

- Create: `novel/src/main/resources/sql/create_ai_model_config_tables.sql`
- Create: `novel/src/main/java/com/novelgeneration/novel/entity/AiModelConfig.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/dto/AiModelConfigRequest.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/vo/AiModelConfigVO.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/vo/AiModelConfigSnapshot.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/mapper/AiModelConfigMapper.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/utils/ApiKeyEncryptor.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/utils/ApiKeyEncryptorTest.java`

**Interfaces:**

- Produces: `AiModelConfigRequest` with `capabilityType`, `providerType`, `apiUrl`, `queryUrl`, `apiKey`, and `modelName`; `AiModelConfigVO` with the same non-secret fields plus `apiKeyMasked`, status, version, and timestamps; `AiModelConfigSnapshot` with decrypted `apiKey` for internal consumers only.
- Mapper methods: `selectActive(String capabilityType)`, `selectLatestVersion(String capabilityType)`, `insertVersion(AiModelConfig config)`, and `disableActive(String capabilityType)`.

- [ ] **Step 1: Write encryption tests.**

Test these concrete behaviors:

```java
@Test
void encryptThenDecryptReturnsOriginalKey() {
    ApiKeyEncryptor encryptor = new ApiKeyEncryptor("0123456789abcdef0123456789abcdef");
    String encrypted = encryptor.encrypt("sk-test-value");
    assertNotEquals("sk-test-value", encrypted);
    assertEquals("sk-test-value", encryptor.decrypt(encrypted));
}

@Test
void eachEncryptionUsesDifferentNonce() {
    ApiKeyEncryptor encryptor = new ApiKeyEncryptor("0123456789abcdef0123456789abcdef");
    assertNotEquals(encryptor.encrypt("same"), encryptor.encrypt("same"));
}

@Test
void maskKeepsOnlyTheLastFourCharacters() {
    ApiKeyEncryptor encryptor = new ApiKeyEncryptor("0123456789abcdef0123456789abcdef");
    assertEquals("sk-****alue", encryptor.mask("sk-test-value"));
}
```

- [ ] **Step 2: Run the focused test and verify it fails.**

Run:

```powershell
mvn -f novel/pom.xml -Dtest=ApiKeyEncryptorTest test
```

Expected: compilation failure because `ApiKeyEncryptor` does not exist.

- [ ] **Step 3: Implement AES-GCM and the table contract.**

Use a 12-byte random nonce and `AES/GCM/NoPadding` with a 128-bit authentication tag. Store the encoded value as `base64(nonce) + "." + base64(ciphertextAndTag)`. Reject a missing or non-32-byte UTF-8 encryption key with `IllegalStateException("模型配置加密密钥未配置或长度不正确")`; never log the input key.

Create the table with `utf8mb4`, `capability_type`, `provider_type`, `api_url`, optional `query_url`, `encrypted_api_key`, `model_name`, `enabled`, `test_status`, `last_test_at`, `last_error`, `version`, `create_time`, and `update_time`. Add an index on `(capability_type, enabled, version)`.

Use Lombok `@Data`, `@NoArgsConstructor`, and `@AllArgsConstructor` consistently with existing DTO/VO classes. Keep `AiModelConfigSnapshot` outside controller responses and never serialize its `apiKey`.

- [ ] **Step 4: Run the focused test and verify it passes.**

Run the same Maven command. Expected: all `ApiKeyEncryptorTest` tests pass.

- [ ] **Step 5: Commit only this task.**

```powershell
git add -- novel/src/main/resources/sql/create_ai_model_config_tables.sql novel/src/main/java/com/novelgeneration/novel/entity/AiModelConfig.java novel/src/main/java/com/novelgeneration/novel/dto/AiModelConfigRequest.java novel/src/main/java/com/novelgeneration/novel/vo/AiModelConfigVO.java novel/src/main/java/com/novelgeneration/novel/vo/AiModelConfigSnapshot.java novel/src/main/java/com/novelgeneration/novel/mapper/AiModelConfigMapper.java novel/src/main/java/com/novelgeneration/novel/utils/ApiKeyEncryptor.java novel/src/test/java/com/novelgeneration/novel/utils/ApiKeyEncryptorTest.java
git commit -m "feat: add encrypted model config storage"
```

## Task 3: Implement management service and REST API

**Files:**

- Create: `novel/src/main/java/com/novelgeneration/novel/service/AiModelConfigService.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/AiModelConfigServiceImpl.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/controller/AiModelConfigController.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/AiModelConfigServiceImplTest.java`
- Test: `novel/src/test/java/com/novelgeneration/novel/controller/AiModelConfigControllerTest.java`

**Interfaces:**

- `List<AiModelConfigVO> list()` — returns exactly one masked current entry for `TEXT`, `IMAGE`, and `VIDEO`, using `NOT_CONFIGURED` for missing entries.
- `AiModelConfigTestResult test(AiModelConfigRequest request)` — validates without persistence.
- `AiModelConfigVO save(String capabilityType, AiModelConfigRequest request)` — creates and enables a new version only after validation and a successful test result supplied by the service flow.
- `AiModelConfigVO disable(String capabilityType)` — disables the active version and returns its masked state.
- Controller routes: `GET /api/model-configs`, `POST /api/model-configs/{type}/test`, `PUT /api/model-configs/{type}`, and `POST /api/model-configs/{type}/disable`.

- [ ] **Step 1: Write service and controller tests.**

Cover these exact cases:

```java
@Test
void listAlwaysReturnsThreeCapabilitySlotsWithoutPlainKey() {
    when(mapper.selectActive("TEXT")).thenReturn(config("TEXT"));
    when(mapper.selectActive("IMAGE")).thenReturn(null);
    when(mapper.selectActive("VIDEO")).thenReturn(config("VIDEO"));

    List<AiModelConfigVO> result = service.list();

    assertEquals(List.of("TEXT", "IMAGE", "VIDEO"),
        result.stream().map(AiModelConfigVO::getCapabilityType).collect(Collectors.toList()));
    assertTrue(result.stream().noneMatch(item -> "sk-test-value".equals(item.getApiKeyMasked())));
}

@Test
void failedProbeDoesNotInsertOrDisableExistingConfig() {
    when(mapper.selectActive("TEXT")).thenReturn(config("TEXT"));
    mockServer.expect(requestTo("https://text.example/v1/chat/completions"))
        .andRespond(withStatus(HttpStatus.UNAUTHORIZED));

    assertThrows(IllegalStateException.class, () -> service.save("TEXT", request("TEXT")));

    verify(mapper, never()).insertVersion(any(AiModelConfig.class));
    verify(mapper, never()).disableActive(anyString());
}

@Test
void saveDisablesOldVersionAndInsertsNextVersionAfterSuccessfulProbe() {
    when(mapper.selectActive("TEXT")).thenReturn(config("TEXT"));
    when(mapper.selectLatestVersion("TEXT")).thenReturn(2);
    mockServer.expect(requestTo("https://text.example/v1/chat/completions"))
        .andRespond(withSuccess("{\"choices\":[{\"message\":{\"content\":\"ok\"}}]}", MediaType.APPLICATION_JSON));

    service.save("TEXT", request("TEXT"));

    verify(mapper).disableActive("TEXT");
    verify(mapper).insertVersion(argThat(item -> item.getVersion() == 3
        && Boolean.TRUE.equals(item.getEnabled())));
}

@Test
void controllerNeverReturnsApiKeyField() throws Exception {
    mockMvc.perform(get("/api/model-configs"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.data[0].apiKey").doesNotExist())
        .andExpect(jsonPath("$.data[0].apiKeyMasked").exists());
}
```

- [ ] **Step 2: Run focused tests and verify they fail.**

```powershell
mvn -f novel/pom.xml -Dtest=AiModelConfigServiceImplTest,AiModelConfigControllerTest test
```

Expected: compilation failure for the missing service and controller.

- [ ] **Step 3: Implement validation, probing, and versioning.**

Validate capability against `TEXT`, `IMAGE`, `VIDEO`; provider against `OPENAI_COMPATIBLE` for text/image and `ARK` or `MINIMAX` for video; require nonblank API URL and model name; require a new API key when no active config exists, otherwise preserve the existing encrypted key when the request key is blank.

Use `@Transactional` for `save`: disable the active row, calculate `latestVersion + 1`, encrypt the key, and insert the new enabled row. If any step fails, the transaction rolls back and the old row remains enabled.

Implement probes as follows:

- `TEXT`: POST to the supplied URL with `model`, one short user message (`连接测试`) and `max_tokens: 1`; require a 2xx response and a nonempty response body.
- `IMAGE` and `VIDEO`: issue an authenticated `HEAD` request to the supplied endpoint. Treat 2xx/3xx as reachable; treat 401/403 as authentication failure; treat 405 as reachable-but-not-generation-tested; do not POST a generation request.

Return `Result.ok` for success and `Result.fail` with a bounded, user-readable message for expected provider failures. Strip API keys from exception messages before persisting `last_error`.

- [ ] **Step 4: Run focused tests and verify they pass.**

Run the same Maven command. Expected: service and controller tests pass with no secret in serialized responses.

- [ ] **Step 5: Commit only this task.**

```powershell
git add -- novel/src/main/java/com/novelgeneration/novel/service/AiModelConfigService.java novel/src/main/java/com/novelgeneration/novel/service/impl/AiModelConfigServiceImpl.java novel/src/main/java/com/novelgeneration/novel/controller/AiModelConfigController.java novel/src/test/java/com/novelgeneration/novel/service/impl/AiModelConfigServiceImplTest.java novel/src/test/java/com/novelgeneration/novel/controller/AiModelConfigControllerTest.java
git commit -m "feat: add model config management api"
```

## Task 4: Add database-first resolution and wire all AI clients

**Files:**

- Create: `novel/src/main/java/com/novelgeneration/novel/service/ModelConfigResolver.java`
- Create: `novel/src/main/java/com/novelgeneration/novel/service/impl/ModelConfigResolverImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/utils/AiUtil.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/CompositeImageServiceImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredStoryboardFrameGenerator.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClient.java`
- Modify: `novel/src/main/resources/application.properties.example`
- Test: `novel/src/test/java/com/novelgeneration/novel/service/impl/ModelConfigResolverImplTest.java`

**Interfaces:**

- `AiModelConfigSnapshot resolveText()`
- `AiModelConfigSnapshot resolveImage()`
- `AiModelConfigSnapshot resolveVideo()`

Each snapshot contains capability, provider, API URL, query URL, decrypted key, model name, enabled flag, and source (`DATABASE` or `PROPERTIES`). It is an internal object and must not cross a controller boundary.

- [ ] **Step 1: Write precedence and fallback tests.**

Test that a database row wins over properties, a missing row returns property values, and an inactive database row does not override properties. Use reflection to set the existing property fields on the resolver in the same style as current tests.

- [ ] **Step 2: Run the focused test and verify it fails.**

```powershell
mvn -f novel/pom.xml -Dtest=ModelConfigResolverImplTest test
```

Expected: compilation failure for the missing resolver.

- [ ] **Step 3: Implement the resolver and client integration.**

The resolver loads `selectActive(capabilityType)`, decrypts the key, and maps property fallbacks exactly as follows:

```text
TEXT  -> ai.api-url, ai.api-key, ai.api-model, provider OPENAI_COMPATIBLE
IMAGE -> image.api-url, image.api-key, image.api-model, provider OPENAI_COMPATIBLE
VIDEO -> video.generation.endpoint, video.generation.query-url,
         video.generation.api-key or video.generation.minimax-api-key,
         video.generation.model, video.generation.provider,
         video.generation.enabled
```

Replace direct use of injected API/model fields in the four consumers with a resolver call at request execution time. Preserve all existing request bodies, model-specific branches, response parsing, upload paths, polling, and provider names. `ConfiguredVideoGenerationClient` must continue using the selected provider’s API key and query URL and must continue rejecting disabled video generation.

Document:

```properties
ai.config.encryption-key=${AI_CONFIG_ENCRYPTION_KEY:}
```

in `application.properties.example`; do not add a real key to `application.properties`.

- [ ] **Step 4: Run focused resolver and existing provider tests.**

```powershell
mvn -f novel/pom.xml -Dtest=ModelConfigResolverImplTest,ConfiguredVideoGenerationClientTest,ConfiguredStoryboardFrameGeneratorTest,CompositeImageServiceImplTest test
```

Expected: all selected tests pass and existing property-only behavior remains covered.

- [ ] **Step 5: Commit only this task.**

```powershell
git add -- novel/src/main/java/com/novelgeneration/novel/service/ModelConfigResolver.java novel/src/main/java/com/novelgeneration/novel/service/impl/ModelConfigResolverImpl.java novel/src/main/java/com/novelgeneration/novel/utils/AiUtil.java novel/src/main/java/com/novelgeneration/novel/service/impl/CompositeImageServiceImpl.java novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredStoryboardFrameGenerator.java novel/src/main/java/com/novelgeneration/novel/service/impl/ConfiguredVideoGenerationClient.java novel/src/main/resources/application.properties.example novel/src/test/java/com/novelgeneration/novel/service/impl/ModelConfigResolverImplTest.java
git commit -m "feat: resolve active ai models dynamically"
```

## Task 5: Build the model management page

**Files:**

- Create: `novel/src/main/resources/static/model-management.html`
- Create: `novel/src/main/resources/static/css/model-management.css`
- Create: `novel/src/main/resources/static/js/model-management.js`
- Create: `novel/src/test/frontend/model-management-page.test.js`

**Interfaces:**

- Consumes: `GET /api/model-configs`, `POST /api/model-configs/{type}/test`, `PUT /api/model-configs/{type}`, and `POST /api/model-configs/{type}/disable`.
- Produces: accessible forms with `data-capability="TEXT|IMAGE|VIDEO"`, status regions, masked keys, and provider-specific controls.

- [ ] **Step 1: Write the failing frontend contract test.**

Assert that the page loads `outline.css`, `workbench-shell.css`, and `model-management.css`; has `data-shell-page="model"`; includes three capability cards; includes fields for provider, API URL, API Key, model name; includes `测试连接`, `保存并启用`, and `停用模型`; and includes `aria-live="polite"` status regions.

- [ ] **Step 2: Run the focused frontend test and verify it fails.**

```powershell
D:\develop\node.exe -e "const fs=require('fs'),path=require('path'); const file=path.resolve('novel/src/test/frontend/model-management-page.test.js'); new Function('require','__dirname','__filename',fs.readFileSync(file,'utf8'))(require,path.dirname(file),file);"
```

Expected: file-not-found or assertion failure because the page does not exist.

- [ ] **Step 3: Implement the page and interaction state machine.**

Use a three-card layout with stable field names based on capability, for example `provider-TEXT`, `apiUrl-TEXT`, `apiKey-TEXT`, and `modelName-TEXT`. Use `<input type="password">` for keys and a separate masked value element for saved keys. Show the query URL only for video as an advanced field, with Ark/MiniMax selection controlling its visibility.

On load, render `apiKeyMasked` but never copy it into the editable API key input. When the API key input is blank during save, send an empty key so the server preserves the existing encrypted key. Disable the card’s buttons while a request is pending, show `测试中…`/`保存中…`, and render errors inside the same card. After save or disable, update only that card from the response.

Implement exact client calls:

```js
fetch('/api/model-configs')
fetch(`/api/model-configs/${type}/test`, {method: 'POST', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)})
fetch(`/api/model-configs/${type}`, {method: 'PUT', headers: {'Content-Type': 'application/json'}, body: JSON.stringify(payload)})
fetch(`/api/model-configs/${type}/disable`, {method: 'POST'})
```

Add visible helper text: “文本测试会发送极短请求；图片和视频测试不会提交生成任务，真实生成可能产生第三方费用。”

- [ ] **Step 4: Run the focused frontend test and verify it passes.**

Run the same Node command. Expected: `model management page checks passed`.

- [ ] **Step 5: Commit only this task.**

```powershell
git add -- novel/src/main/resources/static/model-management.html novel/src/main/resources/static/css/model-management.css novel/src/main/resources/static/js/model-management.js novel/src/test/frontend/model-management-page.test.js
git commit -m "feat: add model management page"
```

## Task 6: End-to-end verification and migration documentation

**Files:**

- Verify: `novel/src/test/frontend/workbench-shell-page.test.js`, `novel/src/test/frontend/model-management-page.test.js`, and `novel/src/main/resources/application.properties.example` from the preceding tasks.

- [ ] **Step 1: Run all frontend smoke tests.**

```powershell
D:\develop\node.exe -e "const fs=require('fs'),path=require('path'); const dir=path.resolve('novel/src/test/frontend'); const files=fs.readdirSync(dir).filter(f=>f.endsWith('.test.js')); for(const name of files){const file=path.join(dir,name); new Function('require','__dirname','__filename',fs.readFileSync(file,'utf8'))(require,dir,file);} console.log('frontend smoke tests passed: '+files.length);"
```

Expected: all existing checks plus the model management check pass.

- [ ] **Step 2: Run the full Maven test suite.**

```powershell
mvn test -f novel/pom.xml
```

Expected: `Failures: 0, Errors: 0` and `BUILD SUCCESS`.

- [ ] **Step 3: Sync static resources.**

```powershell
mvn resources:resources -f novel/pom.xml -q
```

- [ ] **Step 4: Verify the live browser page.**

Open `http://localhost:8081/model-management.html`, confirm the sidebar entry is last, confirm three cards render, and inspect that no API key appears in the DOM response or visible text. Use a desktop viewport and a narrow viewport to verify no horizontal overflow. Read browser logs and expect no errors or warnings.

- [ ] **Step 5: Check the final diff.**

```powershell
git diff --check
git status --short
```

Confirm only feature files are staged and existing unrelated worktree changes remain untouched.

## Self-Review Checklist

- Spec coverage: navigation, fixed three-slot page, encrypted storage, masked response, low-cost tests, failure protection, provider selection, database precedence, property fallback, responsive layout, and automated verification are all assigned above.
- Placeholder scan: no `TBD`, `TODO`, or unspecified implementation step is required.
- Type consistency: controller requests use `AiModelConfigRequest`; public responses use `AiModelConfigVO`; internal AI consumers use `AiModelConfigSnapshot`; resolver exposes `resolveText`, `resolveImage`, and `resolveVideo`.
- Security check: plaintext API keys are limited to the internal snapshot during the outbound provider call; they are not serialized, logged, or persisted unencrypted.
