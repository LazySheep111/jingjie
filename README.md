# 境界 | JingJie

<p align="center">
  <strong>从小说创作到 AI 视频生成的一体化创作工作台</strong>
</p>

<p align="center">
  <a href="#功能特性">功能特性</a> ·
  <a href="#技术栈">技术栈</a> ·
  <a href="#快速开始">快速开始</a> ·
  <a href="#配置说明">配置说明</a> ·
  <a href="#数据库脚本">数据库</a> ·
  <a href="#测试">测试</a>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Java-17-orange" alt="Java 17">
  <img src="https://img.shields.io/badge/Spring%20Boot-2.6.13-brightgreen" alt="Spring Boot 2.6.13">
  <img src="https://img.shields.io/badge/MySQL-8.x-blue" alt="MySQL 8.x">
  <img src="https://img.shields.io/badge/Redis-6.x-red" alt="Redis 6.x">
  <img src="https://img.shields.io/badge/License-MIT-green" alt="MIT License">
</p>

---

境界（JingJie）面向小说改编与 AI 视频创作。你可以在同一条创作流程中导入小说、生成大纲与正文、制作分镜、管理视觉资产，并继续生成首帧和视频。

> 项目仍在持续开发中。AI 文本、图片和视频能力需要配置对应服务；视频生成功能默认关闭。请勿提交个人小说、生成媒体、数据库备份、API Key 或密码。

## 功能特性

### 📚 小说创作

- 上传 TXT/DOCX 小说，或从历史作品中继续创作。
- 生成结构化小说大纲和章节正文，并管理章节内容。
- 在小说全文工作台中浏览作品与章节。

### 🎬 分镜与视频

- 根据章节正文生成、查看和编辑分镜脚本。
- 提取并管理人物、场景视觉资产及其描述、三视图。
- 生成或上传分镜首帧，选择视频分辨率并提交生成任务。
- 查看任务状态、失败原因和历史生成结果。

### 🤖 AI 创作助手

- 围绕当前作品进行对话，并提供只读查询工具。
- 支持聊天会话历史管理；会话消息通过 Redis 暂存并异步持久化到数据库。
- LangChain4j 的实验接入不是当前运行所必需的依赖。

### ⚙️ 模型配置

- 分别配置文本、图片和视频生成服务。
- 模型密钥应在本地配置或通过应用的模型管理功能保存，不要写入代码或提交到版本库。

## 技术栈

| 类别 | 技术 |
| --- | --- |
| 语言与运行时 | Java 17 |
| Web 框架 | Spring Boot 2.6.13、Spring MVC、Thymeleaf |
| 数据访问 | MyBatis 2.2.2、MySQL Connector/J |
| 数据库 | MySQL 8.x |
| 缓存与异步消息 | Redis、Spring Data Redis |
| 前端 | HTML、CSS、原生 JavaScript |
| AI 接口 | DeepSeek / OpenAI 兼容接口；图片与视频服务按需配置 |
| 构建与测试 | Maven、JUnit 5、Spring Boot Test、H2 |

AI 助手的只读工具注册与调用由项目自身实现。模型 API Key、数据库密码和加密密钥都应通过本地配置提供。

## 项目结构

```text
.
├── novel/                              # 主应用
│   ├── pom.xml
│   └── src/
│       ├── main/java/                  # Spring Boot 后端
│       ├── main/resources/static/      # Web 页面、样式和脚本
│       ├── main/resources/sql/         # 初始化与迁移 SQL
│       └── test/                       # 后端与前端测试
├── docs/                               # 技术设计、规格与实现计划
├── designs/                            # 页面设计稿与交互预览
├── .gitignore
├── LICENSE
└── README.md
```

主应用入口：`com.novelgeneration.novel.NovelApplication`。

## 快速开始

### 环境要求

- JDK 17
- Maven 3.6+
- MySQL 8.x（或兼容版本）
- Redis 6.x（或兼容版本）
- 可访问的文本模型 API；图片和视频服务按需配置

### 克隆、配置与启动

以下命令在 Windows PowerShell 中执行：

1. 克隆项目并进入主应用目录：

   ```powershell
   git clone https://github.com/LazySheep111/jingjie.git
   Set-Location .\jingjie\novel
   ```

2. 从模板创建本地配置文件：

   ```powershell
   Copy-Item .\src\main\resources\application.properties.example .\src\main\resources\application.properties
   ```

3. 启动 MySQL 和 Redis。首次初始化数据库时，在 MySQL Workbench 中打开并执行 [`init_novel_db.sql`](novel/src/main/resources/sql/init_novel_db.sql)。该脚本会创建并切换到 `novel_db`，只创建不存在的表，不导入或删除业务数据。

4. 编辑本地 `src/main/resources/application.properties`，至少配置数据库、Redis 和文本模型。图片、视频服务可按需配置；视频生成默认关闭。

5. 在 `novel/` 目录运行测试并启动应用：

   ```powershell
   mvn test
   mvn spring-boot:run
   ```

6. 浏览器访问 [http://localhost:8081](http://localhost:8081)。

## 配置说明

本地配置模板：`novel/src/main/resources/application.properties.example`。请复制模板后填写自己的配置；不要提交 `application.properties`、`.env` 或包含真实凭证的文件。

| 配置项 | 何时需要 | 说明 |
| --- | --- | --- |
| `spring.datasource.url` | 必填 | MySQL JDBC 地址、数据库名及时区；默认库名应为 `novel_db`。 |
| `spring.datasource.username`、`spring.datasource.password` | 必填 | 建议使用权限受限的应用专用账号。 |
| `spring.datasource.driver-class-name` | 必填 | MySQL 驱动，通常为 `com.mysql.cj.jdbc.Driver`。 |
| `spring.redis.host`、`spring.redis.port` | 必填 | Redis 地址和端口；默认 `localhost:6379`。 |
| `spring.redis.password` | Redis 开启认证时 | Redis 未启用认证时可不设置。 |
| `ai.api-key`、`ai.api-url`、`ai.api-model` | 必填 | 文本模型凭证、接口地址和模型名。 |
| `ai.config.encryption-key` | 使用模型管理保存 API Key 时 | 必须是 **32 字节 UTF-8**；通过 `AI_CONFIG_ENCRYPTION_KEY` 环境变量提供。请妥善备份并保持稳定。 |
| `image.api-key`、`image.api-url`、`image.api-model` | 使用图片生成时 | 图片服务凭证、接口地址和模型名。 |
| `image.reference-url` | 使用参考图/三视图合成时 | 图片服务能够访问的基础地址。 |
| `image.local-dir`、`image.storyboard-frame-dir` | 可选 | 本地生成图片和首帧保存目录；内容不要提交到版本库。 |
| `video.generation.enabled` | 使用视频生成时 | 默认 `false`；确认服务可用后再启用。 |
| `video.generation.provider`、`endpoint`、`query-url`、`model` | 使用视频生成时 | 服务商为 `ark` 或 `minimax`；查询 URL 中保留 `{taskId}`。 |
| `video.generation.api-key` | `provider=ark` 时 | Ark 视频服务 API Key。 |
| `video.generation.minimax-api-key` | `provider=minimax` 时 | MiniMax 视频服务 API Key。 |
| `video.generation.image-base-url` | 视频服务需读取本地首帧时 | 应填写视频服务可访问的地址；外部服务通常无法访问本机 `localhost`。 |
| `video.generation.download-dir` | 可选 | 生成视频的本地保存目录，默认位于 `uploads/videos`。 |

**最小启动配置**：MySQL、Redis、文本模型。图片和视频服务不是应用启动的必需项。

## 数据库脚本

脚本位于 `novel/src/main/resources/sql/`：

- `init_novel_db.sql`：新环境初始化脚本，创建 `novel_db` 和项目所需的 18 张表；使用 `IF NOT EXISTS`，可重复执行且不包含业务数据。
- `create_*.sql`：按功能拆分的建表脚本。
- `alter_*.sql`：已有数据库的增量迁移脚本。升级时只执行尚未应用的迁移，不要用初始化脚本代替迁移。

执行前请确认 MySQL 实例和目标库正确。不要将包含小说正文、聊天记录或其他业务数据的数据库导出文件提交到仓库。

## 测试

在 Windows PowerShell 中从仓库根目录执行：

```powershell
Set-Location .\novel
mvn test
```

测试报告输出到 `novel/target/surefire-reports/`。需要调用外部模型、图片或视频服务的功能，还应在配置对应服务后手动验证。

## 安全与隐私

- 不要提交 API Key、数据库/Redis 密码、本地配置、`.env`、小说原文、上传媒体、数据库数据导出或运行日志。
- `novel/uploads/` 是运行时数据目录，不应上传。
- `application.properties.example` 是配置模板；提交前请确认其中只有占位值。
- 曾经进入 Git 历史的凭证应立即撤销并轮换；仅删除当前文件或添加 `.gitignore` 不能消除历史泄露。
- 提交前检查 `git status --short`、`git diff --cached` 和待提交文件列表；不要未经检查执行强制推送。

## 许可证

本项目采用 [MIT License](LICENSE)。
