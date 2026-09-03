# 面试复盘 App —— 总体技术方案

> 版本：v1.0（MVP）
> 状态：已评审
> 读者：Android agent（Android Studio）、后端 agent（IntelliJ IDEA）、产品/技术总监（Claude Code）
> 与 `PRD.md` 配套，是本项目的**技术契约**。接口、字段、枚举、命名以本文档为准，改动必须先改文档。

---

## 1. 架构总览

```
┌──────────────────── Android 客户端（Kotlin）────────────────────┐
│ Compose UI：列表 / 详情 / 对话 / 播放                             │
│   ↑ 单向数据流（MVI：Intent → ViewModel → State）                │
│ ViewModel（Coroutines + StateFlow）                              │
│   ↓                                                              │
│ MediaStoreReader（读系统录音）  Retrofit/OkHttp（上传 + REST + SSE）│
│ MediaPlayer（本地播放）                                          │
└──────────────────────────┬───────────────────────────────────────┘
                           │ HTTP / Multipart / SSE
                           │ http://<后端IP>:8080/api/...
┌──────────────────────────▼── Spring Boot 后端（Java 17）─────────┐
│ RecordingController（REST + SseEmitter）                         │
│   ↓                                                              │
│ RecordingService（CRUD + 上传 + 状态）  ChatService（上下文拼装）  │
│   ↓                                 ↓                            │
│ AsrService（接口 → 云 ASR 实现）    LlmService（接口 → DeepSeek） │
│   ↓                                                              │
│ RecordingMapper / MessageMapper（MyBatis）                        │
│   ↓                                                              │
│ MySQL（recording / message）   本地磁盘 /data/recordings/          │
└──────────────────────────────────────────────────────────────────┘
```

**设计原则**：
- ASR、LLM 都抽成接口（`AsrService` / `LlmService`），换厂商只改实现类，不动 Controller/Service。
- 前后端并行开发的唯一同步点 = 第 4 节的 API 契约 + 第 5 节的数据模型。

---

## 2. 技术栈与版本锁定

> 版本必须锁定，禁止各 agent 自行升级大版本。实施时在锁定的大版本内用最新 patch。

### 2.1 后端

| 项 | 选型 | 版本锚点 |
|---|---|---|
| 语言 | Java | **JDK 17（LTS）** |
| 框架 | Spring Boot | **3.x（推荐 3.3.x）** |
| 构建 | Maven | 3.9+ |
| ORM | MyBatis（原生，手写 SQL） | mybatis-spring-boot-starter 3.x |
| 数据库 | MySQL | 8.0+ |
| 连接池 | HikariCP（Spring Boot 默认） | — |
| HTTP 客户端（调 LLM/ASR） | Spring WebClient（WebFlux） | Spring Boot 自带 |
| 转码 | ffmpeg（命令行，`ProcessBuilder` 调用） | 需本机安装 |

### 2.2 Android 客户端

| 项 | 选型 | 版本锚点 |
|---|---|---|
| 语言 | Kotlin | 2.0.x |
| UI | Jetpack Compose | Compose BOM（最新） |
| 架构 | MVI + ViewModel + StateFlow | — |
| 网络 | Retrofit 2.11+ + OkHttp 4.12+ | — |
| 流式 | OkHttp `EventSource`（SSE） | — |
| 播放 | MediaPlayer（MVP，不引 ExoPlayer） | 系统自带 |
| 构建 | AGP 8.5+ | — |
| minSdk | **26**（覆盖 API 33 前后两套权限） | — |
| target/compileSdk | 34+ | — |

---

## 3. 仓库与目录结构（Monorepo）

```
interview-review-app/
├── docs/
│   ├── PRD.md              # 产品需求
│   ├── tech-spec.md        # 本文档
│   └── devlog.md           # AI 协作过程记录（开发中持续追加）
├── android/                # Android Studio 打开此目录
│   ├── AGENTS.md           # 给 Android agent 的上下文注入
│   └── app/ ...            # Gradle 工程
├── backend/                # IntelliJ IDEA 打开此目录
│   ├── AGENTS.md           # 给后端 agent 的上下文注入
│   └── src/ ...            # Maven 工程
├── .gitignore
└── README.md               # 作品门面：架构图 + 功能 + 技术栈 + AI 协作说明
```

- `android/` 和 `backend/` 是**两个独立构建工程**（Gradle vs Maven），但共用一个 git 仓库。
- 根目录 `README.md` 是给面试官看的门面；`docs/devlog.md` 是 AI 协作证据。

---

## 4. 后端设计

### 4.1 分层与包结构

包根：`com.recap`

```
com.recap
├── RecapApplication.java
├── controller/          RecordingController.java
├── service/             RecordingService, AsrService(接口), LlmService(接口), ChatService
├── service/impl/        AsrServiceImpl(云ASR), LlmServiceImpl(DeepSeek)
├── mapper/              RecordingMapper, MessageMapper
├── entity/              Recording, Message
├── dto/                 RecordingCreateRequest, RecordingUpdateRequest, ChatRequest, RecordingVO 等
├── config/              AsyncConfig, WebClientConfig
└── exception/           GlobalExceptionHandler, BizException
```

### 4.2 数据模型（MySQL DDL）

**命名映射规则（写死）**：DB 字段 `snake_case`，JSON 字段 `camelCase`。MyBatis 开启 `map-underscore-to-camel-case: true`，前后端统一用 camelCase。

```sql
CREATE TABLE recording (
  id          BIGINT AUTO_INCREMENT PRIMARY KEY,
  title       VARCHAR(255) NOT NULL,           -- 默认取文件名
  file_path   VARCHAR(512) NOT NULL,           -- 本地磁盘路径
  file_size   BIGINT,
  duration_ms BIGINT,                          -- 时长（毫秒）
  format      VARCHAR(16),                     -- m4a/amr/wav/mp3...
  status      TINYINT NOT NULL DEFAULT 0,      -- 0=转写中 1=已完成 2=失败
  transcript  LONGTEXT,                        -- 转写全文
  summary     TEXT,                            -- AI 总结
  tags        VARCHAR(255),                    -- 逗号分隔
  error_msg   VARCHAR(255),                    -- 失败原因
  created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;

CREATE TABLE message (
  id           BIGINT AUTO_INCREMENT PRIMARY KEY,
  recording_id BIGINT NOT NULL,
  role         VARCHAR(16) NOT NULL,           -- user / assistant
  content      TEXT NOT NULL,
  created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  INDEX idx_recording (recording_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4;
```

### 4.3 状态枚举（前后端共用，写死）

| DB 值 | JSON 字符串 | 含义 |
|---|---|---|
| 0 | `TRANSFERING` | 转写中 |
| 1 | `DONE` | 已完成（转写 + 总结都完成） |
| 2 | `FAILED` | 失败（`errorMsg` 有原因） |

> JSON API 一律用字符串枚举 `TRANSFERING / DONE / FAILED`；后端用 `enum RecordingStatus` 与 TINYINT 互转。

### 4.4 API 契约

通用约定：
- Base URL：`/api`
- 成功：HTTP 200，返回业务数据。
- 失败：HTTP 4xx/5xx + 统一错误体 `{ "code": <int>, "message": "<str>" }`。
- 时间字段：ISO-8601 字符串 `yyyy-MM-dd HH:mm:ss`。

**错误码表（写死）**

| code | HTTP | 含义 |
|---|---|---|
| 0 | 200 | 成功 |
| 1001 | 404 | 记录不存在 |
| 1002 | 400 | 文件为空 |
| 1003 | 400 | 音频格式不支持 / 转码失败 |
| 1004 | 502 | ASR 转写失败 |
| 1005 | 502 | LLM 调用失败 |
| 1006 | 400 | 参数校验失败 |

#### ① 上传录音

`POST /api/recordings`  （`multipart/form-data`）

| 字段 | 类型 | 必填 | 说明 |
|---|---|---|---|
| file | file | 是 | 音频文件 |
| title | string | 否 | 标题，缺省用文件名 |
| durationMs | long | 否 | 时长毫秒，缺省后端用 ffprobe 获取 |

响应 `200`：
```json
{ "id": 1 }
```
> 后端收到后立即落库（status=TRANSFERING）并返回 id，**异步**执行：转码 → 转写 → 总结。

#### ② 列表

`GET /api/recordings`  → `200`
```json
[
  { "id": 1, "title": "字节一面", "status": "DONE", "durationMs": 3600000,
    "tags": "后端,八股", "createdAt": "2026-09-01 10:00:00" }
]
```
> 列表**不返回** transcript/summary（省流量），按 createdAt 倒序。

#### ③ 详情

`GET /api/recordings/{id}`  → `200`
```json
{
  "id": 1, "title": "字节一面", "status": "DONE", "durationMs": 3600000,
  "format": "m4a", "tags": "后端,八股",
  "transcript": "全文……", "summary": "总结……",
  "errorMsg": null, "createdAt": "2026-09-01 10:00:00"
}
```
> 不存在返回 `404 { "code": 1001, "message": "记录不存在" }`。

#### ④ 转写状态

`GET /api/recordings/{id}/status`  → `200`
```json
{ "status": "TRANSFERING", "errorMsg": null }
```

#### ⑤ 改标题/标签

`PATCH /api/recordings/{id}`  请求体（字段都可选，传哪个改哪个）：
```json
{ "title": "新标题", "tags": "后端,项目" }
```
响应 `200`：更新后的详情对象（同 ③）。

#### ⑥ 删除

`DELETE /api/recordings/{id}`  → `204`（同时删除磁盘文件 + 该录音的所有 message）

#### ⑦ 追问对话（SSE 流式）

`POST /api/recordings/{id}/chat`  请求体：
```json
{ "message": "这道 Handler 原理我答得对吗？" }
```
响应：`text/event-stream`，逐 chunk 推送增量文本，结束推送 `data: [DONE]`：
```
data: 你这道
data: 题答得
data: 部分正确……
data: [DONE]
```
> 后端流程：① 存 user 消息 → ② 拼上下文（见 4.5）→ ③ 调 DeepSeek 流式 → ④ chunk 转发客户端 → ⑤ 流结束后把 assistant 完整回复存库。

#### ⑧ 历史消息

`GET /api/recordings/{id}/messages`  → `200`
```json
[
  { "role": "user", "content": "……", "createdAt": "2026-09-01 11:00:00" },
  { "role": "assistant", "content": "……", "createdAt": "2026-09-01 11:00:05" }
]
```
> 按 createdAt 正序，用于重建对话历史。

#### ⑨ 手动重试总结

`POST /api/recordings/{id}/summary`  → `200 { "summary": "……" }`
> 转写完成但总结失败/为空时，客户端可手动触发。

### 4.5 关键机制

**① 异步转写 + 总结链路**
- 上传接口只落库 + 返回 id；用 `@Async` + `ThreadPoolTaskExecutor`（见 `AsyncConfig`）执行：
  `转码(ffmpeg → 16k wav) → AsrService.transcribe() → 存 transcript → LlmService.summarize() → 存 summary → status=DONE`。
- 任一步失败：`status=FAILED` + `errorMsg`。

**② 音频转码**
- 系统录音机多为 m4a/amr，ASR 通常要 16k 单声道 wav/pcm。
- 统一用 ffmpeg：`ffmpeg -y -i input.m4a -ar 16000 -ac 1 -c:a pcm_s16le output.wav`。
- ffmpeg 是**前置依赖**（本机安装并加入 PATH），agent 需自检。

**③ SSE 流式**
- 用 Spring MVC `SseEmitter`；`LlmServiceImpl` 用 `WebClient` 流式请求 DeepSeek（`stream: true`），把 `choices[].delta.content` 转成 emitter 的 `data:` 事件。

**④ 追问上下文拼装（ChatService）**
- system prompt 角色：「你是面试复盘助手，基于用户某场面试的转写内容，帮他复盘、答疑」。
- 上下文 = 转写全文（超长时截断到约 8000 字符并注明「已截断」）+ 总结 + 最近 10 条历史消息。
- 对话严格限定**单条录音内**，不跨录音。

### 4.6 配置与密钥管理

`application.yml` 关键项（密钥走环境变量，**绝不硬编码、不提交 Git**）：

```yaml
server:
  address: 0.0.0.0          # 必须，让手机能连到
  port: 8080

spring:
  datasource:
    url: jdbc:mysql://localhost:3306/recap?useUnicode=true&characterEncoding=utf8mb4
    username: root
    password: ${MYSQL_PASSWORD}

mybatis:
  map-underscore-to-camel-case: true

recap:
  upload-dir: ./data/recordings        # 录音文件本地存储目录
  asr:
    provider: xunfei                   # 默认讯飞，见开放项
    app-id: ${ASR_APP_ID}
    api-key: ${ASR_API_KEY}
  llm:
    base-url: https://api.deepseek.com
    api-key: ${DEEPSEEK_API_KEY}
    model: deepseek-chat
```

> 环境变量：`MYSQL_PASSWORD` / `ASR_APP_ID` / `ASR_API_KEY` / `DEEPSEEK_API_KEY`。提供 `.env.example` 模板，真实值放本机 `.env`（已 gitignore）。

---

## 5. Android 客户端设计

### 5.1 架构

- 单 Activity + Jetpack Compose + MVI。
- 每个界面一个 `ViewModel` + 一个 `UiState`（`StateFlow`），事件用 `SharedFlow`，单向数据流（沿用小智AI 的架构思想）。
- 分层：`data/`（网络 + 本地读取 + Repository）→ `ui/`（Screen + ViewModel）→ `ui/components`。

### 5.2 模块与关键实现

| 模块 | 说明 |
|---|---|
| MediaStoreReader | 读系统录音：`ContentResolver.query(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI)`，取 `_ID/DISPLAY_NAME/DURATION/DATE_ADDED/SIZE`，按 `DATE_ADDED` 倒序 |
| 权限 | `READ_MEDIA_AUDIO`（API 33+）/ `READ_EXTERNAL_STORAGE`（<33），Manifest 声明 + 运行时请求；拒绝时引导走 SAF |
| 网络层 | Retrofit（REST）+ OkHttp（上传 multipart + SSE）；`BaseUrl` 可配置，默认 `http://<电脑局域网IP>:8080/`，存 SharedPreferences，首次进入可改 |
| 上传 | Multipart，`RequestBody` 用 `Flow`/回调报告进度，UI 显示进度条 |
| SSE 对话 | OkHttp `EventSource` 逐事件收 chunk，`Flow` 累积到 State，Compose 逐字渲染 |
| 播放 | MediaPlayer，播本地 MediaStore URI（不上后端下载） |
| 状态轮询 | 详情页在 `status=TRANSFERING` 时每 3s 轮询 `/status`，变 DONE 后刷新详情 |

### 5.3 界面清单

1. **列表页**：录音卡片（标题、状态徽标、时长、日期、标签），FAB「导入录音」，点卡片进详情，长按/按钮删除。
2. **导入页**：列出系统录音（MediaStore）+「手动选文件」按钮。
3. **详情页**：播放条 + 状态/转写全文 + 总结卡片 + 「追问」入口。
4. **对话页**：消息列表（转写总结上下文 + 历史）+ 输入框，AI 回复流式渲染。

---

## 6. 协作规范（所有 agent 必读）

1. **单一事实源**：`docs/PRD.md` + `docs/tech-spec.md` 是唯一契约。改接口/字段先改文档，再改代码，并在 commit 里注明。
2. **上下文注入**：`android/AGENTS.md`、`backend/AGENTS.md` 各写一份，让对应 IDE 的 agent 了解本目录规范、技术栈、目录结构、编码约定。
3. **Git**：monorepo，主分支 `main`；每个功能拉 `feat/xxx` 分支，完成后合回。commit 用中文，格式 `<类型>: <简述>`（如 `feat: 录音上传接口`）。
4. **.gitignore**：排除 `build/`、`.gradle/`、`target/`、`.idea/`、`.env`、`*.iml`、录音文件、`.pdf_cache`。
5. **mock 先行**：`AsrService`/`LlmService` 先提供 mock 实现打通全链路，再接真实 API。
6. **验收标准**：交付 = 编译通过 + 对应测试通过 + 符合本文档契约，三者缺一不可。
7. **联调网络**：后端 `server.address=0.0.0.0`；真机连 `http://<电脑局域网IP>:8080`，模拟器连 `http://10.0.2.2:8080`；手机与电脑须在同一局域网。
8. **密钥安全**：API key 一律走环境变量/`.env`，发现任何 key 进 Git 立即清除并轮换。

---

## 7. AI 协作证据留存（秋招核心素材）

`docs/devlog.md` 在开发中持续追加，每条记录含：

- **日期 + 目标**：这次会话要做什么。
- **关键 Prompt**：能体现「我怎么用 AI」的 prompt 原文或摘要。
- **AI 产出**：AI 生成了什么（文件/代码/方案）。
- **AI 错误与我的纠错**：AI 哪里错了、我怎么定位修复的（这是最值钱的部分，重点记）。
- **我的架构决策**：哪些是我拍板的、哪些采纳/否决了 AI 建议、为什么。

最终提炼到根 `README.md` 的「AI 协作开发过程」章节，作为面试讲「AI 辅助开发经验」的弹药。

---

## 8. 开放项（实施时定，不影响启动）

| 项 | 待定 | 影响 |
|---|---|---|
| ASR 厂商 | 讯飞 vs 阿里 vs 腾讯（看免费额度 + m4a/amr 支持） | 只影响 `AsrServiceImpl`，接口不变 |
| MySQL 安装 | Docker 还是本机安装（Windows 环境） | 只影响本地环境，不影响代码 |
| 后端/Android 包名 | `com.recap` / `com.recap.app`（实施时统一确认） | 需前后端一致 |
| 跨录音对话 | MVP 严格单录音内；是否支持全局对话待定 | 影响 ChatService 上下文范围 |
