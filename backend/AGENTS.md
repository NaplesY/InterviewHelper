# 后端 —— Agent 上下文

> 开工前先读 `../docs/PRD.md`（功能与验收）和 `../docs/tech-spec.md`（尤其 §4 后端设计：分层/DDL/API 契约/状态枚举/错误码/关键机制）。

## 项目定位

面试复盘 App 的 Spring Boot 后端，提供 REST + SSE 接口。单用户、无鉴权。

## 技术栈（锁定，勿自行升级大版本）

- Java **17+（LTS，本机用 21）**、Spring Boot **3.3.x**、Maven 3.9+
- MyBatis（原生，手写 SQL）+ MySQL 8.0 + HikariCP
- Spring WebClient（调 DeepSeek / ASR 的 HTTP 客户端）
- ffmpeg（命令行转码，**前置依赖**，需本机安装并进 PATH）

## 编码约定

- 包根 `com.recap`，分层 `controller / service / mapper / entity / dto / config / exception`。
- DB `snake_case` ↔ JSON `camelCase`（`mybatis.map-underscore-to-camel-case: true`）。
- 状态枚举 `RecordingStatus`：`0=TRANSFERING / 1=TRANSCRIBED / 2=SUMMARIZING / 3=DONE / 4=FAILED`，JSON 输出字符串。
- 统一错误体 `{ "code": <int>, "message": "<str>" }`，错误码表见 tech-spec §4.4。

## 关键约束

- **mock 先行**：`AsrService` / `LlmService` 先写 mock 实现打通链路，再接真实 API。
- **异步转写**：`@Async` + `ThreadPoolTaskExecutor`；文件先**同步落盘**再返回 id，后台「转码 → 转写 → 总结」。
- **SSE**：Spring MVC `SseEmitter` + WebClient 流式，`data: [DONE]` 结束。
- **密钥走环境变量**（`MYSQL_PASSWORD` / `DEEPSEEK_API_KEY` / `ASR_APP_ID` / `ASR_SECRET_KEY`），绝不硬编码、绝不提交 Git。
- `server.address=0.0.0.0`（手机能连）。
- 转码：ffmpeg 转 16k 单声道 wav 再送 ASR。

## 验收标准

编译通过 + curl/Postman 验证 9 个接口符合契约 + 真实录音端到端跑通。
