# 境界（JingJie）

境界是一个面向小说改编与 AI 视频创作的 Web 工作台。它把小说大纲、正文、分镜、视觉资产、首帧和视频生成整合到一条创作流程中，并提供历史作品管理与 AI 创作助手。

> 本项目仍在持续开发中。部分 AI、图片和视频能力需要自行配置对应服务商的 API；视频生成默认关闭。请勿把个人小说、生成媒体、数据库备份或任何 API 密钥提交到公开仓库。

## 功能

- **小说创作**：导入 TXT/DOCX 小说，生成结构化大纲和章节正文，浏览历史作品。
- **分镜创作**：根据章节正文生成分镜脚本，并查看、编辑分镜内容。
- **视觉资产**：提取人物与场景，管理资产描述和三视图，复用已有资产。
- **首帧与视频**：生成或上传分镜首帧，选择视频分辨率并提交视频生成任务，查看任务状态和历史结果。
- **AI 助手**：围绕当前作品进行对话；提供只读查询工具，并支持会话历史管理。
- **模型配置**：配置文本、图片和视频服务。模型密钥由应用配置后保存在本地环境/数据库中，不应写入版本库。

## 技术栈

| 层次 | 技术 |
| --- | --- |
| 后端语言 | Java 17 |
| Web 框架 | Spring Boot 2.6.13、Spring MVC、Thymeleaf |
| 数据访问 | MyBatis 2.2.2、MySQL Connector/J |
| 缓存与异步消息 | Redis（Spring Data Redis） |
| 前端 | HTML、CSS、原生 JavaScript |
| 构建与测试 | Maven、JUnit 5、Spring Boot Test、H2 |
| AI 服务 | DeepSeek / OpenAI 兼容接口；文本、图像与视频服务分别配置 |

AI 助手包含项目内实现的只读工具注册与调用逻辑。LangChain4j 的实验接入不属于当前 `master` 版本的运行前提。

## 项目结构

```text
.
├── novel/
│   ├── pom.xml                         # 主应用 Maven 配置
│   └── src/
│       ├── main/java/                  # Spring Boot 后端
│       ├── main/resources/static/      # Web 页面、样式和脚本
│       ├── main/resources/sql/         # 数据库建表及迁移脚本
│       └── test/                       # 后端与前端测试
├── docs/                               # 设计系统、需求规格与实现计划
├── designs/                            # 页面设计稿和交互预览
└── README.md
```

当前主应用入口为 `com.novelgeneration.novel.NovelApplication`，位于 `novel/src/main/java/`。仓库根目录还保留有早期代码与构建文件；运行主应用时请进入 `novel/` 目录。

## 环境要求

- JDK 17
- Maven 3.6+
- MySQL 8.x（或兼容版本）
- Redis 6.x 或兼容版本
- 可访问的文本模型 API；使用图片/视频生成时，还需配置相应服务商

## 本地运行

以下命令在 Windows PowerShell 中执行。

1. 克隆仓库并进入主应用目录：

   ```powershell
   git clone https://github.com/LazySheep111/jingjie.git
   Set-Location .\jingjie\novel
   ```

2. 准备本地配置文件：

   ```powershell
   Copy-Item .\src\main\resources\application.properties.example .\src\main\resources\application.properties
   ```

3. 在本机创建 MySQL 数据库，并启动 MySQL、Redis。按需执行 `src/main/resources/sql/` 中的建表/迁移脚本；不要把真实业务数据导出到仓库。

4. 编辑本地 `application.properties`，至少配置数据库、Redis 和文本模型；图片、视频生成按需配置。该文件已被忽略规则排除，不要使用 `git add -f` 强行加入版本库。

5. 在 `novel/` 目录构建或运行：

   ```powershell
   mvn test
   mvn spring-boot:run
   ```

6. 打开 [http://localhost:8081](http://localhost:8081)。

配置项名称和用途请参考 `novel/src/main/resources/application.properties.example`。示例中的密钥、用户名、密码和服务地址均为占位值；请在本地替换，不要把真实值粘贴到 README、问题截图或提交记录中。

### 配置项清单

| 配置项 | 何时需要 | 说明 |
| --- | --- | --- |
| `spring.datasource.url` | 必填 | MySQL JDBC 地址、数据库名及时区；先创建数据库并执行所需 SQL 脚本。 |
| `spring.datasource.username`、`spring.datasource.password` | 必填 | MySQL 用户名和密码。建议使用权限受限的应用专用账号，不要提交真实密码。 |
| `spring.datasource.driver-class-name` | 必填 | MySQL 驱动，模板值为 `com.mysql.cj.jdbc.Driver`。 |
| `spring.redis.host`、`spring.redis.port` | 必填 | 聊天会话缓存及异步消息使用 Redis。默认 `localhost:6379`。 |
| `spring.redis.password` | 仅 Redis 开启认证时 | 模板默认未设置；如 Redis 有密码，在本地配置文件中增加此项。 |
| `ai.api-key`、`ai.api-url`、`ai.api-model` | 必填 | 文本生成服务。默认 URL 指向 DeepSeek 兼容接口；填写当前有效的 API Key 和账户可用模型名。 |
| `ai.config.encryption-key` | 使用模型管理保存 API Key 时必填 | 用于加密数据库中保存的模型密钥，必须是 **32 字节 UTF-8**。模板通过环境变量 `AI_CONFIG_ENCRYPTION_KEY` 读取。设置后应妥善备份并保持稳定；更换它前需先迁移/重新加密已存密钥，否则旧密钥无法解密。 |
| `image.api-key`、`image.api-url`、`image.api-model` | 使用 AI 图片/首帧/资产图生成时 | 图片服务商凭据、生成接口和模型；服务需兼容项目当前使用的图片 API 格式。 |
| `image.reference-url` | 使用参考图/三视图合成时 | 可由图片服务访问的参考图地址；按部署环境填写可访问的 URL。 |
| `image.local-dir`、`image.storyboard-frame-dir` | 可选 | 生成图片和分镜首帧的本地保存目录，模板默认位于 `uploads/`。请确保应用有写入权限；目录内容不要提交。 |
| `video.generation.enabled` | 使用视频生成时 | 默认 `false`。确认服务商、模型和回调/查询配置可用后再改为 `true`。 |
| `video.generation.provider`、`endpoint`、`query-url`、`model` | 使用视频生成时 | 服务商只能是 `ark` 或 `minimax`；按所选服务商填写提交地址、任务查询地址和模型名。查询 URL 中保留 `{taskId}` 占位符。 |
| `video.generation.api-key` | `provider=ark` 时 | Ark 视频服务 API Key。 |
| `video.generation.minimax-api-key` | `provider=minimax` 时 | MiniMax 视频服务 API Key。只配置当前所选服务商的有效密钥。 |
| `video.generation.image-base-url` | 视频服务需要读取本地首帧时 | 应是视频服务可访问的公网基础地址；仅本机 `localhost` 通常不可被外部服务访问。 |
| `video.generation.download-dir` | 可选 | 生成视频的本地保存目录，模板默认位于 `uploads/videos`。 |

**最小启动配置**：MySQL、Redis、文本模型（`ai.api-key`、`ai.api-url`、`ai.api-model`）。图片和视频服务是可选项；不配置视频时保持 `video.generation.enabled=false`。如果 Redis 开启了密码认证，再增加 `spring.redis.password`。

本地配置模板位于 `novel/src/main/resources/application.properties.example`。不要将真实 Key 或密码写入 README、截图、聊天记录或 Git 提交中。生产环境优先使用环境变量/密钥管理服务；模型管理加密密钥 `AI_CONFIG_ENCRYPTION_KEY` 不要与数据库或 AI API Key 共用。

## 数据库脚本

建表及迁移脚本位于 `novel/src/main/resources/sql/`。首次运行前，请根据所使用的数据库状态执行所需脚本；已有数据库升级时，只运行尚未应用的迁移。执行前请备份数据库，并检查脚本中的表名和变更内容。当前仓库没有统一的一键数据库初始化命令。

## 测试

```powershell
Set-Location .\novel
mvn test
```

测试报告会输出到 `novel/target/surefire-reports/`。涉及外部模型、图片或视频服务的功能还需要在已配置相应服务后手动验证。

## 上传到 GitHub 前的隐私检查

公开仓库前至少完成以下检查：

1. **立即撤销并轮换**曾经写入 Git 历史的 AI API Key 和数据库密码。即使之后重写历史，已暴露的凭证也应视为失效。
2. 检查 `git status --short`、`git diff --cached` 和 `git ls-files`，确认没有本地配置、`.env`、小说原文、上传媒体、数据库备份、日志、IDE 文件或个人文档。`docs/` 与 `designs/` 中是项目技术设计资料；公开前也请逐份确认没有不适合公开的内容。
3. 根 `.gitignore` 不会自动停止跟踪已提交文件；对已经跟踪的隐私文件，必须先从版本控制索引移除，并按需要清理历史。
4. 本项目历史里出现过凭证。若要公开现有仓库历史，应先使用经过审查的 Git 历史清理流程，检查所有分支和标签，并确认远端协作者不会把旧历史重新推回来。若不需要保留旧提交，可建立仅包含已审查当前源码的新仓库历史。
5. `novel/uploads/` 属于运行时个人数据，不要提交；`application.properties.example` 是不含真实密钥的配置模板，可以提交。
6. 不要把“只忽略文件”当作泄露凭证的补救措施，也不要未经检查就对 GitHub 执行强制推送。

## 许可证

发布前请确认仓库根目录包含适用于本项目的许可证文件，并核实所有第三方素材、代码和生成内容的使用权限。
