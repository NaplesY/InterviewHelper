# 后端开发路线（类级）

> 本文件是 `TIMELINE.md` 阶段 1–4（第 7–22 项）的**后端细化版**，粒度到「类」。
> **用途**：随时中断/捡起。捡起时看**下方第一个 ⬜ 任务**，从它开始；每个任务结束有「完成标志」+「提交点」，能确认上次做到哪、验证过没。
> **状态标记**：`✅` 已完成 · `🔵` 正在做 · `⬜` 待做。
> **事实源**：接口/字段/枚举以 `docs/tech-spec.md` §4 为准，本路线只做工程化拆分，不与契约冲突。

---

## Goal

按「先骨架后能力、先 mock 后真实」的顺序，把后端从空工程做到 9 个接口符合契约、真实录音端到端跑通。

**架构**：Spring Boot 3.3.x（MVC + SseEmitter）单模块 Maven 工程，MyBatis 手写 SQL + MySQL，ffmpeg 转码 + 云 ASR + DeepSeek；ASR/LLM 抽接口、mock 先行。

**Tech Stack**：JDK 17+（本机 21）· Spring Boot 3.3.x · MyBatis 3.x · MySQL 8.0 · WebClient（WebFlux 客户端）· ffmpeg

**Spec**：`docs/PRD.md`、`docs/tech-spec.md`（本路线从它们拆解，执行时一起读）

---

## 全局约束（每个任务都遵守）

- 包根 `com.recap`，分层 `controller / service / service.impl / mapper / entity / dto / config / exception`。
- DB `snake_case` ↔ JSON `camelCase`：`mybatis.map-underscore-to-camel-case: true`。
- **状态枚举**（tech-spec §4.3）：`0=TRANSFERING / 1=TRANSCRIBED / 2=SUMMARIZING / 3=DONE / 4=FAILED`，JSON 输出字符串。
- **错误码**（tech-spec §4.4）：`0/1001/1002/1003/1004/1005/1006`；成功 HTTP 200 直接返回业务数据，失败 HTTP 4xx/5xx 返回 `{ code, message }`。1003/1004/1005 仅用于同步接口。
- **时间字段**：JSON 输出 `yyyy-MM-dd HH:mm:ss`。
- **密钥走环境变量**：`MYSQL_PASSWORD` / `DEEPSEEK_API_KEY` / `ASR_APP_ID` / `ASR_SECRET_KEY`，绝不硬编码、不进 Git。
- `server.address=0.0.0.0`；multipart `max-file-size=100MB` / `max-request-size=110MB`。
- 提交用中文，格式 `<类型>: <简述>`（如 `feat: 录音上传接口`）。

---

## 文件结构（完整类清单 + 职责）

| 类（`com.recap.*`） | 职责 | 依赖 |
|---|---|---|
| `RecapApplication` | 主类，`@SpringBootApplication` + `@EnableAsync` | — |
| `entity.RecordingStatus` | 状态枚举，DB int ↔ JSON str 互转 | — |
| `entity.Recording` | 对应 `recording` 表 | — |
| `entity.Message` | 对应 `message` 表 | — |
| `mapper.RecordingMapper` | Recording 的 insert/查询/更新状态与字段/删除 | `entity.Recording` |
| `mapper.MessageMapper` | Message 的 insert/查询/删除 | `entity.Message` |
| `exception.ErrorCode` | 错误码枚举（0/1001–1006，含 httpStatus + message） | — |
| `exception.BizException` | 业务异常，携带 `ErrorCode` | `ErrorCode` |
| `exception.ErrorResponse` | 错误响应体 `{ code, message }` | — |
| `exception.GlobalExceptionHandler` | `@RestControllerAdvice`，异常 → `ErrorResponse` | `ErrorCode`/`BizException`/`ErrorResponse` |
| `config.AsyncConfig` | `ThreadPoolTaskExecutor` bean | — |
| `config.WebClientConfig` | `WebClient.Builder` bean（含超时） | — |
| `service.FileStorageService` | 文件落盘/删除/路径解析 | 配置 `recap.upload-dir` |
| `service.FfmpegService` | ffmpeg 转码 + 转码产物清理 | `FileStorageService` |
| `service.AsrService` | 接口：`transcribe` | — |
| `service.impl.MockAsrServiceImpl` | mock 转写 | `AsrService` |
| `service.impl.AsrServiceImpl` | 真实 ASR（讯飞） | `AsrService` |
| `service.LlmService` | 接口：`summarize` + `streamChat` | `dto.LlmMessage` |
| `service.impl.MockLlmServiceImpl` | mock 总结/流式 | `LlmService` |
| `service.impl.LlmServiceImpl` | 真实 DeepSeek（WebClient） | `LlmService` + `WebClientConfig` |
| `service.RecordingService` | CRUD + 上传 + 状态查询 + 手动总结 | `RecordingMapper`/`FileStorageService`/`AsyncTranscribeService` |
| `service.AsyncTranscribeService` | `@Async` 异步链路「转码→转写→总结」 | `FfmpegService`/`AsrService`/`LlmService`/`RecordingMapper` |
| `service.ChatService` | 对话上下文拼装 + SSE 流式 + 存库解耦 | `LlmService`/`MessageMapper`/`RecordingMapper` |
| `controller.RecordingController` | 9 个接口 | `RecordingService`/`ChatService` |
| `dto.RecordingUpdateRequest` | PATCH 请求体 | — |
| `dto.ChatRequest` | chat 请求体 | — |
| `dto.LlmMessage` | 内部拼上下文用（record：role/content） | — |
| `dto.RecordingVO` | 详情响应 | — |
| `dto.RecordingListItemVO` | 列表项响应（不含 transcript/summary） | — |
| `dto.RecordingStatusVO` | 状态响应 | — |
| `dto.MessageVO` | 历史消息响应 | — |

**依赖顺序**（先建谁后建谁）：

```
基础枚举/实体 → Mapper → 异常体系 → 配置/存储/转码 → AsrService(mock) → LlmService(mock)
→ RecordingService(上传+CRUD) → AsyncTranscribeService → ChatService → Controller → 真实 ASR/LLM 替换 → 自测
```

---

## Task 1：后端工程初始化（TIMELINE #7）

**Files:**
- Create: `backend/pom.xml`、`backend/src/main/resources/application.yml`、`backend/src/main/java/com/recap/RecapApplication.java`

**关键内容**（pom 依赖 + 主类 + 配置）：
- 依赖：`spring-boot-starter-web`、`spring-boot-starter-webflux`（**仅为 WebClient/Flux**，web 仍是 servlet 主容器）、`mybatis-spring-boot-starter`（3.x）、`mysql-connector-j`、`lombok`（optional）、`spring-boot-starter-test`。
- `application.yml` 完整写入（含已敲定的 multipart 配置）：

```yaml
server:
  address: 0.0.0.0
  port: 8080

spring:
  servlet:
    multipart:
      max-file-size: 100MB
      max-request-size: 110MB
  datasource:
    url: jdbc:mysql://localhost:3306/recap?useUnicode=true&characterEncoding=utf8mb4&serverTimezone=Asia/Shanghai
    username: root
    password: ${MYSQL_PASSWORD}
  profiles:
    active: mock          # 先跑 mock；接真实 ASR/LLM 后移除

mybatis:
  map-underscore-to-camel-case: true

recap:
  upload-dir: ./data/recordings
  asr:
    provider: xunfei                # 讯飞「语音转写（长音频 lfasr）」
    app-id: ${ASR_APP_ID}
    secret-key: ${ASR_SECRET_KEY}   # 讯飞接口密钥，用于签名
  llm:
    base-url: https://api.deepseek.com
    api-key: ${DEEPSEEK_API_KEY}
    model: deepseek-chat
```

```java
@SpringBootApplication
@EnableAsync
public class RecapApplication {
    public static void main(String[] args) {
        SpringApplication.run(RecapApplication.class, args);
    }
}
```

**完成标志**：`mvn spring-boot:run` 能启动且不报错（此时无接口、连库会失败属正常，可先注释 datasource 或确保 MySQL 已装）。

**提交点**：`git commit -m "chore: 后端工程初始化（Spring Boot 3.3 + MyBatis + 配置）"`

---

## Task 2：建库建表（TIMELINE #8）

**Files:**
- Create: `backend/src/main/resources/schema.sql`（DDL 照抄 tech-spec §4.2，含 5 状态注释）

**完成标志**：MySQL 里 `recap` 库 + `recording`/`message` 两表存在，`DESC recording` 字段与 DDL 一致。

**提交点**：`git commit -m "chore: 建库建表 DDL（recording/message）"`

---

## Task 3：entity + Mapper（TIMELINE #9）

**Files:**
- Create: `entity/Recording.java`、`entity/Message.java`、`mapper/RecordingMapper.java`、`mapper/MessageMapper.java`

**接口签名**（类型一致，后续任务照此调用）：

```java
public class Recording {
    private Long id; private String title; private String filePath;
    private Long fileSize; private Long durationMs; private String format;
    private Integer status;        // RecordingStatus.dbValue（0-4）
    private String transcript; private String summary;
    private String tags; private String errorMsg;
    private LocalDateTime createdAt;
    // getter/setter（lombok @Data）
}

public class Message {
    private Long id; private Long recordingId;
    private String role;           // "user" / "assistant"
    private String content; private LocalDateTime createdAt;
}

@Mapper
public interface RecordingMapper {
    int insert(Recording r);
    Recording findById(Long id);
    List<Recording> findAll();                                    // ORDER BY created_at DESC
    int updateTitleAndTags(Long id, String title, String tags);   // 动态（title/tags 非空才改）
    int updateStatus(Long id, Integer status, String errorMsg);
    int updateTranscript(Long id, String transcript);
    int updateSummary(Long id, String summary);
    int deleteById(Long id);
}

@Mapper
public interface MessageMapper {
    int insert(Message m);
    List<Message> findByRecordingId(Long recordingId);            // ORDER BY created_at ASC
    int deleteByRecordingId(Long recordingId);
}
```

- 主类或配置加 `@MapperScan("com.recap.mapper")`。

**完成标志**：写一个临时 `main` 或单测，通过 `RecordingMapper.insert` + `findById` 能读写 `recording` 表（验证 `map-underscore-to-camel-case` 生效）。

**提交点**：`git commit -m "feat: entity + Mapper（recording/message）"`

---

## Task 4：状态枚举 + 错误码 + 全局异常（TIMELINE #10 前半）

**Files:**
- Create: `entity/RecordingStatus.java`、`exception/ErrorCode.java`、`exception/BizException.java`、`exception/ErrorResponse.java`、`exception/GlobalExceptionHandler.java`

**接口签名**：

```java
public enum RecordingStatus {
    TRANSFERING(0, "TRANSFERING"), TRANSCRIBED(1, "TRANSCRIBED"),
    SUMMARIZING(2, "SUMMARIZING"), DONE(3, "DONE"), FAILED(4, "FAILED");
    private final int dbValue; private final String jsonValue;
    public static RecordingStatus fromDbValue(int dbValue) { /* 找不到抛 BizException(PARAM_INVALID) */ }
    public int getDbValue(); public String getJsonValue();
}

public enum ErrorCode {
    SUCCESS(0, 200, "成功"),
    RECORDING_NOT_FOUND(1001, 404, "记录不存在"),
    FILE_EMPTY(1002, 400, "文件为空"),
    FORMAT_NOT_SUPPORTED(1003, 400, "音频格式不支持 / 转码失败"),
    ASR_FAILED(1004, 502, "ASR 转写失败"),
    LLM_FAILED(1005, 502, "LLM 调用失败"),
    PARAM_INVALID(1006, 400, "参数校验失败");
    private final int code; private final int httpStatus; private final String message;
    public int getCode(); public int getHttpStatus(); public String getMessage();
}

public class BizException extends RuntimeException {
    private final ErrorCode errorCode;
    // 构造 BizException(ErrorCode) 与 BizException(ErrorCode, String detail)
}

public class ErrorResponse {
    private int code; private String message;
    // 静态工厂 of(ErrorCode) / of(ErrorCode, String)
}

@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(BizException.class)       // → 对应 httpStatus + ErrorResponse
    @ExceptionHandler(Exception.class)          // → 500 + ErrorResponse(500, "服务器内部错误")
    // 可选：MethodArgumentNotValidException / MaxUploadSizeExceededException → 1006/1002
}
```

**关键约定**：成功返回**不包** `ErrorResponse`（controller 直接 return 数据），只有异常走 `GlobalExceptionHandler` 返回 `{ code, message }`——这是 tech-spec §4.4 的契约，别把成功也包成 `{code:0, data}`。

**完成标志**：临时接口抛 `BizException(RECORDING_NOT_FOUND)`，curl 返回 `404 {"code":1001,"message":"记录不存在"}`。

**提交点**：`git commit -m "feat: 状态枚举 + 错误码 + 全局异常"`

---

## Task 5：文件上传接口（TIMELINE #11）

**Files:**
- Create: `service/FileStorageService.java`、`service/RecordingService.java`（先只实现 `upload`）、`controller/RecordingController.java`（先只实现上传）

**接口签名**：

```java
@Component
public class FileStorageService {
    // 落盘到 recap.upload-dir，返回相对 filePath（如 "2026/09/uuid.m4a"），供入库
    public String store(MultipartFile file);   // file 为空抛 BizException(FILE_EMPTY)
    public void delete(String filePath);       // 容错删除
}

public interface RecordingService {
    Long upload(MultipartFile file, String title, Long durationMs);  // 同步落盘+落库，返回 id
}
```

**关键约定（落盘边界）**：`upload` 内**同步**完成 `FileStorageService.store(file)` → 组装 `Recording`（title 缺省用原始文件名、format 取扩展名、status=TRANSFERING）→ `RecordingMapper.insert` → 返回 id。**不要**把落盘放异步（`MultipartFile` 请求结束即失效）。

**Controller 上传接口**：`POST /api/recordings`，`@RequestParam("file") MultipartFile` + 可选 `@RequestParam title/durationMs`，返回 `{ "id": 1 }`（直接返回 Map 或小 DTO）。

**完成标志**：
```bash
curl -F "file=@录音.m4a" http://localhost:8080/api/recordings
# → {"id":1}；且磁盘 upload-dir 下有文件、recording 表多一条 status=0 的记录
```

**提交点**：`git commit -m "feat: 录音上传接口（multipart + 落盘 + 落库）"`

---

## Task 6：CRUD 接口（TIMELINE #12）

**Files:**
- Create: `dto/RecordingUpdateRequest.java`、`dto/RecordingVO.java`、`dto/RecordingListItemVO.java`、`dto/RecordingStatusVO.java`
- Modify: `service/RecordingService.java`（补 list/detail/status/update/delete）、`controller/RecordingController.java`（补 5 接口）、`mapper/RecordingMapper.java`（补 update/delete 方法）

**接口签名**：

```java
public interface RecordingService {
    Long upload(MultipartFile file, String title, Long durationMs);
    List<RecordingListItemVO> list();
    RecordingVO detail(Long id);                       // 不存在抛 BizException(RECORDING_NOT_FOUND)
    RecordingStatusVO status(Long id);
    RecordingVO update(Long id, RecordingUpdateRequest req);   // 只改非空字段
    void delete(Long id);                              // 删库 + 删磁盘文件 + 删该录音 message
}
```

**DTO 字段**（JSON camelCase，`status`/`createdAt` 为 String）：

```java
class RecordingVO {          // 详情：id,title,status,durationMs,format,tags,transcript,summary,errorMsg,createdAt }
class RecordingListItemVO {  // 列表：id,title,status,durationMs,tags,createdAt }（无 transcript/summary）
class RecordingStatusVO {    // status, errorMsg }
class RecordingUpdateRequest { String title; String tags; }
```

**关键点**：`status` 输出用 `RecordingStatus.fromDbValue(entity.status).getJsonValue()`；`createdAt` 格式化为 `yyyy-MM-dd HH:mm:ss`（entity `LocalDateTime` + `@JsonFormat` 或 VO 用 String）。列表按 `createdAt` 倒序（Mapper `findAll` 已保证）。

**完成标志**：逐个 curl 验证 5 接口（列表/详情/状态/PATCH/DELETE），字段与 tech-spec §4.4 ②③④⑤⑥ 完全一致；删除后磁盘文件消失、message 清空。

**提交点**：`git commit -m "feat: 录音 CRUD 接口（列表/详情/状态/更新/删除）"`

---

## Task 7：AsrService 接口 + Mock（TIMELINE #13）

**Files:**
- Create: `service/AsrService.java`、`service/impl/MockAsrServiceImpl.java`

**接口签名**：

```java
public interface AsrService {
    String transcribe(String wavFilePath);   // 返回转写文本；失败抛 BizException(ASR_FAILED)
}

@Service @Profile("mock")
public class MockAsrServiceImpl implements AsrService {
    public String transcribe(String wavFilePath) { return "【mock 转写】这是模拟转写文本……"; }
}
```

**完成标志**：注入 `AsrService` 调 `transcribe` 能拿到 mock 文本（编译 + 手动或单测）。

**提交点**：`git commit -m "feat: AsrService 接口 + mock 实现"`

---

## Task 8：ffmpeg 转码模块（TIMELINE #14）

**Files:**
- Create: `service/FfmpegService.java`

**接口签名**：

```java
@Component
public class FfmpegService {
    // input 转 16k 单声道 wav，返回 wav 绝对路径；失败抛 BizException(FORMAT_NOT_SUPPORTED)
    public Path transcodeToWav(Path input);
    public void deleteQuietly(Path path);   // 容错删除（用于清理转码产物）
}
```

**关键点**：用 `ProcessBuilder` 执行 `ffmpeg -y -i <in> -ar 16000 -ac 1 -c:a pcm_s16le <out>.wav`，等待完成并检查 exit code；异常流读 stderr 记入 errorMsg。**转码产物是临时文件，供 Task 9 在送完 ASR 后调用 `deleteQuietly` 清理。**

**完成标志**：对一个 m4a/amr 文件跑 `transcodeToWav`，产出 wav 且 `ffprobe` 确认 16k/单声道；不存在 ffmpeg 时抛 `FORMAT_NOT_SUPPORTED`。

**提交点**：`git commit -m "feat: ffmpeg 转码模块（16k 单声道 wav）"`

---

## Task 9：异步转写链路（TIMELINE #15）

**Files:**
- Create: `config/AsyncConfig.java`、`service/AsyncTranscribeService.java`
- Modify: `service/RecordingService.java`（upload 末尾触发异步）、`RecordingController`（无需改）

**接口签名**：

```java
@Configuration @EnableAsync
public class AsyncConfig {
    @Bean(name = "transcribeExecutor")
    public ThreadPoolTaskExecutor transcribeExecutor() { /* core=2, max=4, queue=50 */ }
}

public interface AsyncTranscribeService {
    @Async("transcribeExecutor")
    void process(Long recordingId);   // 异步执行，见下
}
```

**异步链路（当前只做到转写，总结在 Task 12 补）**：
```
读 recording.filePath → FfmpegService.transcodeToWav → AsrService.transcribe(wav)
→ deleteQuietly(wav)          // 转码产物立即清理
→ RecordingMapper.updateTranscript(id, text) + updateStatus(id, RecordingStatus.TRANSCRIBED.getDbValue(), null)
→ 失败(转码/转写)：updateStatus(id, RecordingStatus.FAILED.getDbValue(), 原因)
```
> `updateStatus` 的 status 参数是 `Integer`，一律用 `RecordingStatus.X.getDbValue()` 转 int。

> ⚠️ `@Async` 方法必须**通过注入的 `AsyncTranscribeService` 代理调用**（`RecordingService` 里 `@Autowired AsyncTranscribeService` 后调用），不能同类 self-invoke，否则不生效。

**完成标志**：上传后立即返回 id，几秒后 `GET /api/recordings/{id}/status` 从 `TRANSFERING` 变为 `TRANSCRIBED`，`transcript` 有 mock 文本；传一个坏格式文件 → `FAILED` + errorMsg。

**提交点**：`git commit -m "feat: 异步转写链路（转码→ASR→存 transcript）"`

---

## Task 10：真实 ASR 接入（讯飞 lfasr）（TIMELINE #16）

**Files:**
- Create: `service/impl/AsrServiceImpl.java`（`@Profile("!mock")`）

**方案**：讯飞「语音转写（长音频 lfasr）」——面试录音是长音频，走非实时转写。凭据 `app_id` + `secret_key`。

**流程（lfasr 是「上传 → 轮询取结果」异步模式）**：
```
① upload：POST https://raasr.xfyun.cn/v2/api/upload（appId + signa + ts + 音频文件）→ 返回 taskId
② getResult：POST https://raasr.xfyun.cn/v2/api/getResult（appId + signa + ts + taskId）→ 轮询到完成 → 取转写全文
```

**签名（核心，容易错）**：
```
signa = Base64(HmacSHA1(MD5(appId + ts).getBytes(), secretKey.getBytes()))
// ts = 10 位秒级时间戳；signa 作为 URL 参数需 urlencode
```

```java
@Service @Profile("!mock")
public class AsrServiceImpl implements AsrService {
    // @Value 注入 recap.asr.app-id / recap.asr.secret-key；注入 WebClient
    public String transcribe(String wavFilePath) {
        String taskId = upload(wavFilePath);   // 传 16k 单声道 wav，得 taskId
        return pollResult(taskId);             // 轮询（间隔 2s，超时 5min），取转写全文
        // 任一步失败/超时 → 抛 BizException(ASR_FAILED)
    }
}
```

**关键点**：
- 凭据取 `recap.asr.app-id` / `recap.asr.secret-key`；签名 `HmacSHA1(MD5(appId+ts), secretKey)`。
- `transcribe` 内部「上传 + 轮询」是**阻塞**的，跑在 `@Async` 线程里，不占请求线程。
- 失败抛 `BizException(ASR_FAILED)`，异步链路置 `status=FAILED` + errorMsg（对齐 PRD US-5）。
- **替换方式**：移除 `spring.profiles.active: mock` 即切到真实实现（两个 impl 靠 `@Profile` 互斥）。
- 接口/鉴权细节以官方文档为准：<https://www.xfyun.cn/doc/asr/lfasr/API.html>。

**完成标志**：真实录音转出真实文本；失败时 `status=FAILED` + errorMsg。

**提交点**：`git commit -m "feat: 真实 ASR 接入（讯飞 lfasr）"`

---

## Task 11：LlmService 接口 + Mock（TIMELINE #17）

**Files:**
- Create: `service/LlmService.java`、`service/impl/MockLlmServiceImpl.java`、`dto/LlmMessage.java`

**接口签名**：

```java
public record LlmMessage(String role, String content) {}

public interface LlmService {
    String summarize(String transcript);                 // 非流式总结，失败抛 BizException(LLM_FAILED)
    Flux<String> streamChat(List<LlmMessage> messages);  // 流式，返回增量文本 Flux<String>，结束自然 complete
}

@Service @Profile("mock")
public class MockLlmServiceImpl implements LlmService {
    public String summarize(String t) { return "【mock 总结】" + t.substring(0, Math.min(t.length(), 100)); }
    public Flux<String> streamChat(List<LlmMessage> m) { return Flux.just("这是", "mock", "流式回复"); }
}
```

**完成标志**：注入 `LlmService`，`summarize` 拿 mock 总结、`streamChat` 能订阅到分段 Flux。

**提交点**：`git commit -m "feat: LlmService 接口 + mock 实现"`

---

## Task 12：总结链路 + 手动重试（TIMELINE #18）

**Files:**
- Modify: `service/AsyncTranscribeService.java`（转写完成后接总结）、`service/RecordingService.java`（补 `retrySummary`）、`controller/RecordingController.java`（补 `POST /summary`）

**接口签名**：

```java
public interface RecordingService {
    // ... 原有 ...
    String retrySummary(Long id);   // 仅 TRANSCRIBED 可调；同步总结，成功 status=DONE 并返回 summary
}
```

**异步链路补全（转写完成后）**：
```
updateTranscript 后 → updateStatus(id, RecordingStatus.SUMMARIZING.getDbValue(), null)
→ LlmService.summarize(transcript) → updateSummary(id, s) + updateStatus(id, RecordingStatus.DONE.getDbValue(), null)
→ 总结失败：updateStatus(id, RecordingStatus.TRANSCRIBED.getDbValue(), "总结失败原因")   // 文字稿仍可读，可重试
```

**手动重试**：`retrySummary` 校验 `status==TRANSCRIBED`，否则抛 `BizException(PARAM_INVALID)`；调 `summarize`，成功落库 + `DONE`；失败抛 `BizException(LLM_FAILED)`（同步接口用 1005）。

**完成标志**：上传后状态走 `TRANSFERING → SUMMARIZING → DONE`，summary 有 mock 文本；mock 总结失败时状态落到 `TRANSCRIBED`，调 `POST /summary` 后回 `DONE`。

**提交点**：`git commit -m "feat: 总结链路 + 手动重试接口"`

---

## Task 13：DeepSeek 非流式总结（TIMELINE #19）

**Files:**
- Create: `config/WebClientConfig.java`、`service/impl/LlmServiceImpl.java`（先实现 `summarize`）

**接口签名**：

```java
@Configuration
public class WebClientConfig {
    @Bean
    public WebClient.Builder webClientBuilder() { /* 连接/响应超时（如 60s） */ }
}

@Service @Profile("!mock")
public class LlmServiceImpl implements LlmService {
    public String summarize(String transcript) { /* WebClient POST DeepSeek chat/completions，非流式 */ }
    public Flux<String> streamChat(List<LlmMessage> messages) { /* Task 14 实现，先可抛 UnsupportedOperationException */ }
}
```

**关键点**：prompt 按 PRD §F4 五段结构（概览/问题清单/回答要点/可改进处/补课建议）要求 DeepSeek 输出 JSON 或 Markdown 结构；从 `recap.llm.*` 取 base-url/api-key/model。

**完成标志**：真实 LLM 生成结构化总结；失败抛 `LLM_FAILED`（走 `TRANSCRIBED` + errorMsg）。

**提交点**：`git commit -m "feat: DeepSeek 非流式总结"`

---

## Task 14：SSE 流式对话（TIMELINE #20）

**Files:**
- Create: `service/ChatService.java`、`dto/ChatRequest.java`、`dto/MessageVO.java`
- Modify: `service/impl/LlmServiceImpl.java`（实现 `streamChat`，`stream: true`，`bodyToFlux(ServerSentEvent)`）、`controller/RecordingController.java`（补 `POST /chat`、`GET /messages`）

**接口签名**：

```java
public interface ChatService {
    SseEmitter chat(Long recordingId, String message);   // 存 user + 拼上下文 + 流式 + 存 assistant
    List<MessageVO> messages(Long recordingId);          // 按 createdAt 正序
}

class ChatRequest { String message; }
class MessageVO { String role; String content; String createdAt; }
```

**ChatService.chat 流程（存库与客户端连接解耦，tech-spec §4.5 ③）**：
```
① MessageMapper.insert(user 消息)
② 拼 messages：system prompt + 转写全文(截断~8000 字注明「已截断」) + 总结 + 最近 10 条历史 + 本次 user
③ LlmService.streamChat(messages) 订阅：
   StringBuilder full 累积每个 delta → 同时 try { emitter.send(delta) } catch 吞掉（客户端断不断开）
   doOnComplete → 存 assistant 完整回复（落库点，不依赖 emitter 回调）
   doOnError   → 存「已累积部分 + [回复中断]」占位 assistant，保证 user 不孤儿
```

**关键点**：`POST /chat` 返回 `SseEmitter`（`text/event-stream`），逐 chunk 发 `data: <增量>`，结束发 `data: [DONE]`；`GET /messages` 返回历史（user/assistant 交替）。

**完成标志**：curl 请求 `/chat` 能逐字收到增量并 `data: [DONE]` 结束；中途断连（Ctrl+C）后 `GET /messages` 仍能看到完整 assistant 回复（验证解耦）。

**提交点**：`git commit -m "feat: SSE 流式对话（存库与连接解耦）"`

---

## Task 15：接口全量自测（TIMELINE #21）

**Files:** 无需新类，必要时补 `docs/backend-api-test.md`（curl 清单）

**完成标志**：9 个接口（上传/列表/详情/状态/更新/删除/chat/messages/summary）逐个 curl 验证，响应与 tech-spec §4.4 JSON 契约完全一致；错误路径（404/1001、空文件/1002、坏格式/1003）返回正确 `{ code, message }`。

**提交点**：`git commit -m "test: 后端接口全量自测通过"`

---

## Task 16：真实端到端（TIMELINE #22）

**Files:** 无新类

**完成标志**：一条真实录音走「上传 → 转码 → 真实 ASR → 真实 DeepSeek 总结 → 追问」全链路；状态正确流转到 `DONE`，转写/总结/流式追问都真实可用。**切换方式**：移除 `spring.profiles.active: mock`，确保 `.env` 有真实密钥。

**提交点**：`git commit -m "test: 后端真实端到端跑通"`

---

## 当前状态

- Task 1 ✅ 已完成（工程初始化，`mvn spring-boot:run` 启动验证通过）。
- 其余 ⬜ 待做。**下一件事 = Task 2：建库建表。**
