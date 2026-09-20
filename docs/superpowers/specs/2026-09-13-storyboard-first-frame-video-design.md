# 分镜首帧驱动的视频生成设计

## 目标

在用户点击某个分镜的“生成视频”后，系统先复用当前 Seedream 5.0 Lite 生图配置，根据分镜内容、全书视觉风格以及关联人物/场景三视图生成一张 16:9 分镜首帧，再将该首帧和动态提示词提交给 `doubao-seedance-1-5-pro-251215` 生成视频。

## 范围

- 保留现有“每个分镜独立生成视频”的交互。
- 保留方舟任务提交、轮询、视频下载、本地保存和历史版本机制。
- 不接入 MiniMax。
- 不把人物和场景三视图直接传给 Seedance。
- 不实现尾帧生成、整章批量生成或视频拼接。
- 不主动提交或推送 Gitee。

## 生成流程

1. 后端读取当前分镜、全书视觉风格和关联的视觉资产版本。
2. 校验所有关联资产都已有三视图图片；缺少时直接返回明确错误，不调用生图或视频接口。
3. 使用分镜画面描述、人物动作、场景信息和全书视觉风格构造首帧提示词。
4. 将关联人物/场景三视图作为 Seedream 参考图，生成一张实际影视画面的 16:9 静态首帧。
5. 将首帧保存到 `uploads/storyboard-frames/{novelId}/chapter-{chapterNum}/scene-{sceneId}/`。
6. 把首帧转换成模型可访问的公网 URL。
7. 向方舟视频接口提交一个 `text` 项和一个 `role=first_frame` 的 `image_url` 项。
8. 轮询 Seedance 任务；成功后下载 MP4，并保存视频版本、首帧路径、提示词快照、视觉风格快照和资产版本快照。

## 组件边界

### StoryboardFrameGenerator

专门负责首帧生图协议，不复用三视图业务服务。它使用现有 `image.api-key`、`image.api-url` 和 `image.api-model`，接受规范化的首帧提示词与参考图 URL，返回本地首帧路径。

首帧请求不得带有“三视图”“正面”“侧面”“背面”“白色背景”等三视图模板要求。提示词必须要求单张完整影视画面、16:9 构图、人物与场景外观遵循参考图。

### VideoGenerationService

继续负责任务编排。状态扩展为：

- `QUEUED`：等待执行。
- `GENERATING_FRAME`：正在生成并保存分镜首帧。
- `GENERATING_VIDEO`：首帧已生成，正在提交或轮询 Seedance。
- `SUCCESS`：视频已下载并完成持久化。
- `FAILED`：任一步骤失败。

失败任务保留已生成的首帧。重试时若首帧存在且文件可读，默认复用该首帧，避免重复生图；首帧生成失败或文件丢失时重新生成。

### ConfiguredVideoGenerationClient

保持方舟客户端身份，但请求格式从多张 `reference_image` 改为单张 `first_frame`：

```json
{
  "model": "doubao-seedance-1-5-pro-251215",
  "content": [
    {
      "type": "text",
      "text": "动态提示词"
    },
    {
      "type": "image_url",
      "image_url": { "url": "https://public.example/frame.png" },
      "role": "first_frame"
    }
  ],
  "ratio": "16:9",
  "duration": 10,
  "generate_audio": true,
  "watermark": false
}
```

`VideoProviderRequest` 只向视频客户端提供 `firstFrameImageUrl`，不再向它暴露多张三视图。资产快照仍由视频服务保存，用于追溯这张首帧由哪些资产版本生成。

## 数据保存

为 `video_generation_task` 增加 `first_frame_path`，使视频生成失败时仍可查看和复用已经生成的首帧。

为 `storyboard_video` 增加 `first_frame_path`，使每个成功视频版本都能追溯自己的首帧。

数据库保存应用内相对路径；提交给模型时再通过 `video.generation.image-base-url` 转换为公网 URL。首帧文件和视频文件都不提交 Git。

## 前端行为

- 点击“生成视频”后，该分镜卡片依次显示“正在生成分镜首帧”和“正在生成视频”。
- 首帧生成完成后，在视频区域显示首帧预览，随后继续自动生成视频，无需用户二次点击。
- 视频失败但首帧成功时，保留首帧预览和错误原因。
- 点击“重试”复用已有首帧；本版不增加“重新生成首帧”按钮。
- 其他分镜卡片不受当前任务影响。

## 错误处理

- 缺少三视图：在任务创建前失败，并指出具体资产名称。
- Seedream 配置缺失或返回格式错误：任务进入 `FAILED`，不调用 Seedance。
- 首帧无法转换为公网 URL：任务进入 `FAILED`，提示检查 `video.generation.image-base-url`。
- 方舟返回鉴权、模型、内容审核或参数错误：保存完整但限长的响应信息，供前端展示。
- 视频任务超时或下载为空：任务进入 `FAILED`，保留首帧供重试。

## 测试策略

- 首帧提示词包含分镜、全书视觉风格和资产引用约束，但不包含三视图排版要求。
- Seedream 请求包含全部可用资产参考图，并只请求一张 16:9 影视画面。
- 方舟请求只包含一个 `first_frame`，不再包含 `reference_image`。
- 状态按 `QUEUED -> GENERATING_FRAME -> GENERATING_VIDEO -> SUCCESS` 流转。
- 首帧成功、视频失败时保存首帧路径；重试复用首帧。
- 首帧失败时不调用视频客户端。
- 成功视频记录同时保存首帧和资产版本快照。

## 验收标准

1. 每个分镜仍有独立“生成视频”按钮。
2. 点击后先生成并展示一张符合分镜内容的静态首帧。
3. Seedance 请求中该图片的角色为 `first_frame`。
4. 视频成功后可在页面播放，并保存在本地与数据库中。
5. 视频失败后可直接复用已有首帧重试。
6. 现有三视图生成、资产库和分镜编辑功能不受影响。
