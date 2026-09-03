# Android 客户端 —— Agent 上下文

> 开工前先读 `../docs/PRD.md`（功能与验收）和 `../docs/tech-spec.md`（尤其 §5 Android 设计、§4.4 API 契约、§4.3 状态枚举）。

## 项目定位

面试复盘 App 的 Android 客户端，与 Spring Boot 后端（`../backend/`）配合。单用户、无登录。

## 技术栈（锁定，勿自行升级大版本）

- Kotlin 2.0.x、Jetpack Compose（BOM 最新）
- MVI + ViewModel + StateFlow（单向数据流）+ Coroutines
- Retrofit 2.11+ + OkHttp 4.12+（REST + Multipart 上传 + SSE）
- MediaPlayer（播放，**不引 ExoPlayer**）
- minSdk 26 / targetSdk 34+

## 编码约定

- 单 Activity + Compose；每个界面一个 `ViewModel` + 一个 `UiState`（`StateFlow`），事件用 `SharedFlow`。
- 包结构 `com.recap.app`：`ui/`（Screen + ViewModel）、`data/`（网络 + MediaStore 读取 + Repository）。
- DTO 字段与后端契约**严格对齐**（camelCase）；状态用字符串枚举 `TRANSFERING / DONE / FAILED`，不要自己造。

## 关键约束

- **BaseUrl 可配置**：默认 `http://<电脑局域网IP>:8080/`，存 SharedPreferences，首次进入可改。
- **权限分版本**：API 33+ 用 `READ_MEDIA_AUDIO`，否则 `READ_EXTERNAL_STORAGE`；拒绝时引导 SAF（`ACTION_OPEN_DOCUMENT`）。
- **SSE**：用 OkHttp `EventSource` 逐事件收 chunk，累积到 State 逐字渲染。
- **状态轮询**：详情页 `status=TRANSFERING` 时每 3s 轮询 `/api/recordings/{id}/status`。
- 客户端不涉及任何 API key（key 都在后端），不要写死任何密钥。

## 验收标准

编译通过 + 字段与契约一致 + 真机能跑通 PRD §7 验收清单。
