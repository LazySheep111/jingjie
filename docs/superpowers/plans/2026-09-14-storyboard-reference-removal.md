# 分镜参考资产移除 Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** 允许用户从当前分镜移除人物或场景参考资产，而不删除资产本体或既有视频记录。

**Architecture:** `StoryboardController` 暴露一个删除引用接口，`StoryboardService` 校验角色并委托 `StoryboardMapper` 使用带小说、章节、分镜约束的联表删除。视频生成页为每张参考资产卡片提供确认删除按钮，成功后重新加载页面数据。

**Tech Stack:** Spring Boot、MyBatis 注解 Mapper、原生 JavaScript、CSS、JUnit 5、Node.js 前端契约测试。

## Global Constraints

- 只删除 `novel_storyboard_asset_ref` 的当前分镜关联行。
- 不删除 `visual_asset`、版本、三视图文件、首帧、视频和已创建视频任务快照。
- 仅接受 `CHARACTER` 和 `LOCATION` 两种资产角色。
- 删除 SQL 必须限定小说 ID、章节号、分镜 ID、资产 ID 和资产角色。
- 不提交或同步到 Gitee，除非用户明确要求。

---

### Task 1: 后端删除当前分镜资产引用

**Files:**
- Modify: `novel/src/main/java/com/novelgeneration/novel/mapper/StoryboardMapper.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/StoryboardService.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/service/impl/StoryboardServiceImpl.java`
- Modify: `novel/src/main/java/com/novelgeneration/novel/controller/StoryboardController.java`
- Modify: `novel/src/test/java/com/novelgeneration/novel/mapper/StoryboardMapperParameterTest.java`
- Modify: `novel/src/test/java/com/novelgeneration/novel/service/impl/StoryboardServiceImplTest.java`
- Create: `novel/src/test/java/com/novelgeneration/novel/controller/StoryboardControllerTest.java`

**Interfaces:**
- Produces: `void StoryboardService.removeAssetReference(Long novelId, Long chapterNum, Long sceneId, Long assetId, String assetRole)`.
- Produces: `DELETE /api/novel/{novelId}/chapters/{chapterNum}/storyboard/{sceneId}/assets/{assetId}?assetRole=CHARACTER|LOCATION`.
- Consumes: `StoryboardMapper.deleteAssetReference(...)`, returning the number of removed rows.

- [ ] **Step 1: 写入失败的服务、控制器与 Mapper 参数测试**

在 `StoryboardServiceImplTest` 中添加：

```java
@Test
void removesOnlyTheCurrentSceneAssetReference() {
    StoryboardMapper mapper = mock(StoryboardMapper.class);
    when(mapper.deleteAssetReference(1L, 2L, 3L, 4L, "CHARACTER")).thenReturn(1);
    StoryboardServiceImpl service = new StoryboardServiceImpl();
    ReflectionTestUtils.setField(service, "storyboardMapper", mapper);

    service.removeAssetReference(1L, 2L, 3L, 4L, "CHARACTER");

    verify(mapper).deleteAssetReference(1L, 2L, 3L, 4L, "CHARACTER");
}
```

同时增加非法角色断言 `assertThrows(IllegalArgumentException.class, () -> service.removeAssetReference(1L, 2L, 3L, 4L, "OTHER"));`，在新建 `StoryboardControllerTest` 中验证控制器调用服务，并在 Mapper 参数测试中验证五个 SQL 参数都有 `@Param`。

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
& 'D:\maven\apache-maven-3.9.8\bin\mvn.cmd' -Dtest=StoryboardServiceImplTest,StoryboardControllerTest,StoryboardMapperParameterTest test
```

Expected: 编译失败，提示 `removeAssetReference` 或 `deleteAssetReference` 尚不存在。

- [ ] **Step 3: 添加最小删除实现**

在 `StoryboardMapper` 中添加：

```java
@Delete("DELETE r FROM novel_storyboard_asset_ref r "
        + "JOIN novel_storyboard_scene s ON s.id = r.storyboard_scene_id "
        + "JOIN novel_storyboard b ON b.id = s.storyboard_id "
        + "WHERE b.novel_id=#{novelId} AND b.chapter_num=#{chapterNum} "
        + "AND r.storyboard_scene_id=#{sceneId} AND r.asset_id=#{assetId} AND r.asset_role=#{assetRole}")
int deleteAssetReference(@Param("novelId") Long novelId,
                         @Param("chapterNum") Long chapterNum,
                         @Param("sceneId") Long sceneId,
                         @Param("assetId") Long assetId,
                         @Param("assetRole") String assetRole);
```

在服务接口及实现中加入 `removeAssetReference`，拒绝空 ID、非 `CHARACTER`/`LOCATION` 角色，以及 Mapper 返回 `0` 的未关联请求。控制器添加 `@DeleteMapping("/{sceneId}/assets/{assetId}")`，接受 `assetRole` 请求参数并调用服务。

- [ ] **Step 4: 运行后端目标测试**

Run:

```powershell
& 'D:\maven\apache-maven-3.9.8\bin\mvn.cmd' -Dtest=StoryboardServiceImplTest,StoryboardControllerTest,StoryboardMapperParameterTest test
```

Expected: 三个测试类全部通过。

### Task 2: 视频生成页面的删除入口

**Files:**
- Modify: `novel/src/main/resources/static/js/video-generation.js:35-150`
- Modify: `novel/src/main/resources/static/css/outline.css:600-720`
- Modify: `novel/src/test/frontend/video-generation-page.test.js`

**Interfaces:**
- Consumes: `removeReferenceAsset(card, scene, assetId, assetRole)` 和 Task 1 的删除接口。
- Produces: 卡片按钮 `data-action="remove-reference-asset"` 及 `DELETE` 请求。

- [ ] **Step 1: 写入失败的前端契约测试**

在 `video-generation-page.test.js` 添加：

```js
assert(script.includes('data-action="remove-reference-asset"'));
assert(script.includes('function removeReferenceAsset(card, scene, assetId, assetRole)'));
assert(script.includes('/assets/${encodeURIComponent(assetId)}?assetRole='));
assert(script.includes('仅移除该资产在当前分镜中的引用'));
```

- [ ] **Step 2: 运行测试确认失败**

Run:

```powershell
node novel/src/test/frontend/video-generation-page.test.js
```

Expected: 失败，指出删除按钮或函数缺失。

- [ ] **Step 3: 添加卡片按钮、确认和删除请求**

将 `renderReferences` 的每张卡片改为在图片区域右上方带有删除按钮，按钮包含 `data-asset-id` 与 `data-asset-role`。在 `renderScenes` 中给该按钮绑定点击事件，调用：

```js
async function removeReferenceAsset(card, scene, assetId, assetRole) {
    const confirmed = window.confirm('仅移除该资产在当前分镜中的引用，不会删除资产库内容或影响已生成视频。');
    if (!confirmed) return;
    const response = await fetch(
        `/api/novel/${encodeURIComponent(novelId)}/chapters/${encodeURIComponent(chapterNum)}`
        + `/storyboard/${encodeURIComponent(scene.id)}/assets/${encodeURIComponent(assetId)}`
        + `?assetRole=${encodeURIComponent(assetRole)}`,
        {method: 'DELETE'}
    );
    if (!response.ok) throw new Error(`移除参考资产失败：${response.status}`);
    await loadPage();
}
```

在 `outline.css` 中为按钮提供定位、可见焦点和悬浮危险色样式；保留卡片图片和标题布局。

- [ ] **Step 4: 运行前端测试与语法检查**

Run:

```powershell
node novel/src/test/frontend/video-generation-page.test.js
node --check novel/src/main/resources/static/js/video-generation.js
```

Expected: 前端测试输出 `video-generation page checks passed`，语法检查无输出且退出码为 0。

### Task 3: 整体验证

**Files:**
- No additional files.

- [ ] **Step 1: 运行完整验证**

Run:

```powershell
Get-ChildItem novel/src/test/frontend/*.test.js | ForEach-Object { node $_.FullName; if ($LASTEXITCODE -ne 0) { exit $LASTEXITCODE } }
& 'D:\maven\apache-maven-3.9.8\bin\mvn.cmd' test
```

Expected: 所有前端契约测试通过，Maven 输出 `BUILD SUCCESS` 且失败和错误均为 0。

- [ ] **Step 2: 人工验证**

重启 Spring Boot 后，在视频生成页面删除一个人物和一个场景参考资产；确认当前分镜卡片消失，而资产库和其他分镜中同一资产仍存在。
