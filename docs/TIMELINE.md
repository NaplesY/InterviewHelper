# 时间线（串行任务链）

> **这份文件怎么用**：严格按序号从上到下串行执行，一次只做一件事。
> **状态标记**：`✅` 已完成 · `🔵` 正在做 · `⬜` 待做 · `🟡` 未来优化（非 MVP）
> **下一件事 = 往下第一个 `⬜`**。每完成一项，把状态改成 `✅`，你自然就知道下一件该干什么。
> 有些任务技术上可以并行（如前后端），但按你的习惯**串行排**，做完一件再开下一件。

---

## 阶段 0：立项与准备

> 目标：把「地基」打好——契约文档、git 基线、agent 上下文、依赖与凭据。缺了任何一样，后面都会返工。

- [x] **1. 敲定 PRD + 技术方案** ✅
  - 产出：`docs/PRD.md`、`docs/tech-spec.md`
  - 说明：已经完成，是后续一切的唯一事实源。

- [x] **2. 写这份时间线** ✅
  - 产出：`docs/TIMELINE.md`（本文件）

- [ ] **3. git init + 首次提交（建基线）** ⬜
  - 做什么：仓库 `git init`，写 `.gitignore`（排除 `build/`、`.gradle/`、`target/`、`.idea/`、`.env`、`*.iml`、录音文件），提交 PRD/tech-spec/TIMELINE。
  - 产出：干净的 git 基线，之后每个改动可 diff 追溯。
  - 为什么先做：没有基线，agent 开工后无法追踪「谁改了什么」。

- [ ] **4. 写 agent 上下文注入文件** ⬜
  - 做什么：写 `android/AGENTS.md` 和 `backend/AGENTS.md`，各自讲清本目录的技术栈、目录结构、编码约定、契约文档位置。
  - 产出：两个 agent 一开工就能「读懂项目」。
  - 参考：tech-spec §6.2

- [ ] **5. 申请 API key（有延迟，提前办）** ⬜
  - 做什么：DeepSeek 注册充值拿 key；讯飞（或阿里）ASR 实名认证 + 申请免费额度。
  - 产出：`DEEPSEEK_API_KEY` + `ASR_APP_ID`/`ASR_API_KEY`，写入本机 `.env`（不入库）。
  - 为什么提前：实名/额度审批可能一两天，别等代码写完卡在这。

- [ ] **6. 装环境并自检** ⬜
  - 做什么：确认 JDK 17、ffmpeg（进 PATH）、MySQL（或 Docker）、IDEA、Android Studio 都可用；`ffmpeg -version`、`java -version` 能跑。
  - 产出：环境自检通过清单。

---

## 阶段 1：后端骨架

> 目标：先把后端 CRUD + 上传跑通。**先做后端**，因为 Android 的接口和字段完全依赖后端的契约。

- [ ] **7. 后端工程初始化** ⬜
  - 做什么：Spring Boot 3.3.x + Maven，包结构按 tech-spec §4.1，能启动空服务。
  - 产出：`backend/` 工程能 `mvn spring-boot:run` 起来。

- [ ] **8. 建库建表** ⬜
  - 做什么：MySQL 建 `recap` 库 + `recording`/`message` 两张表，DDL 照抄 tech-spec §4.2。
  - 产出：两张表存在，字段与 DDL 一致。

- [ ] **9. MyBatis 集成 + Mapper** ⬜
  - 做什么：接 mybatis-spring-boot-starter，写 `RecordingMapper`/`MessageMapper` + `entity`，开启 `map-underscore-to-camel-case`。
  - 产出：能通过 Mapper 读写两张表。

- [ ] **10. 统一返回体 + 错误码 + 全局异常** ⬜
  - 做什么：`GlobalExceptionHandler` + 错误码表（tech-spec §4.4）。
  - 产出：任何异常都返回 `{ code, message }`，错误码与契约一致。

- [ ] **11. 文件上传接口** ⬜
  - 做什么：`POST /api/recordings`，multipart 接收文件，落磁盘（`recap.upload-dir`）+ 落库（status=TRANSFERING）。
  - 产出：上传返回 `{ id }`，文件存到本地磁盘，记录入库。
  - 参考：tech-spec §4.4 ①

- [ ] **12. CRUD 接口** ⬜
  - 做什么：列表/详情/改标题标签/删除/状态查询（tech-spec §4.4 ②③④⑤⑥）。
  - 产出：5 个接口 curl 可验证，字段与契约完全一致。

---

## 阶段 2：ASR 集成（mock 先行）

> 目标：转写链路跑通。**先 mock 后真实**，避免一开始就卡在真实 API 的认证/格式上。

- [ ] **13. AsrService 接口 + Mock 实现** ⬜
  - 做什么：定义 `AsrService.transcribe(filePath): String`，先给 `MockAsrServiceImpl` 返回假文本。
  - 产出：调转写能拿到一段假文本，链路通。

- [ ] **14. ffmpeg 转码模块** ⬜
  - 做什么：`ProcessBuilder` 调 ffmpeg，m4a/amr → 16k 单声道 wav。
  - 产出：任意系统录音格式能转成 ASR 需要的 wav。
  - 参考：tech-spec §4.5 ②

- [ ] **15. 异步转写链路** ⬜
  - 做什么：`@Async` + `ThreadPoolTaskExecutor`，上传后异步执行「转码 → 转写 → 存 transcript → 更新 status」。
  - 产出：上传后立即返回，后台转写，状态可查。
  - 参考：tech-spec §4.5 ①

- [ ] **16. 真实 ASR 接入** ⬜
  - 做什么：写 `AsrServiceImpl`（讯飞/阿里），替换 mock。
  - 产出：真实录音能转出真实文本；失败时 status=FAILED + errorMsg。

---

## 阶段 3：LLM 集成（mock 先行）

> 目标：总结 + 追问对话跑通。同样是先 mock 后 DeepSeek。

- [ ] **17. LlmService 接口 + Mock 实现** ⬜
  - 做什么：`LlmService.summarize(transcript): String` + `MockLlmServiceImpl`。
  - 产出：能拿到一段假总结。

- [ ] **18. 总结链路** ⬜
  - 做什么：转写完成自动触发总结 → 存 summary → status=DONE；失败可手动重试（`POST /summary`）。
  - 产出：转写完成自动出总结，状态正确流转。

- [ ] **19. DeepSeek 非流式接入（总结）** ⬜
  - 做什么：`LlmServiceImpl` 用 WebClient 调 DeepSeek（非流式），替换 mock 总结。
  - 产出：真实 LLM 生成结构化总结（按 PRD §F4 五段结构）。

- [ ] **20. SSE 流式对话** ⬜
  - 做什么：`ChatService` 拼上下文 + `SseEmitter` + DeepSeek 流式；`POST /chat`、`GET /messages`、`POST /summary`。
  - 产出：追问能流式返回、历史消息可回看。
  - 参考：tech-spec §4.4 ⑦⑧⑨、§4.5 ③④

---

## 阶段 4：后端自测

> 目标：后端独立验证通过，再交给 Android。

- [ ] **21. 接口全量自测** ⬜
  - 做什么：curl/Postman 逐个验证 9 个接口，对照 tech-spec §4.4 的 JSON 契约。
  - 产出：9 个接口全部符合契约。

- [ ] **22. 真实端到端（后端侧）** ⬜
  - 做什么：拿一条真实录音，走「上传 → 转码 → 真实 ASR → 真实 DeepSeek 总结 → 追问」全链路。
  - 产出：后端侧端到端跑通。

---

## 阶段 5：Android 客户端

> 目标：客户端功能。依赖阶段 4 的稳定接口。

- [ ] **23. Android 工程初始化** ⬜
  - 做什么：Gradle + Compose + MVI 骨架，包结构，能跑起空 App。
  - 产出：App 能安装启动。

- [ ] **24. 网络层** ⬜
  - 做什么：Retrofit + OkHttp，DTO 对齐后端契约（字段/枚举），BaseUrl 可配置。
  - 产出：能连上后端任意接口。
  - 参考：tech-spec §5.2

- [ ] **25. 权限 + MediaStoreReader** ⬜
  - 做什么：运行时权限（API 33 前后两套）+ 读系统录音列表 + SAF 兜底。
  - 产出：能列出系统录音机里的文件。

- [ ] **26. 列表页** ⬜
  - 做什么：录音卡片（标题/状态徽标/时长/日期/标签）+「导入」入口。
  - 产出：列表页可用。

- [ ] **27. 上传功能** ⬜
  - 做什么：multipart 上传 + 进度条 + 失败重试。
  - 产出：能上传并看到进度。

- [ ] **28. 详情页** ⬜
  - 做什么：播放 + 转写全文 + 总结卡片 + 状态轮询（TRANSFERING 时每 3s 轮询）。
  - 产出：详情页完整可用。

- [ ] **29. 对话页** ⬜
  - 做什么：SSE 流式渲染 + 历史消息加载。
  - 产出：追问能逐字显示。

- [ ] **30. 管理功能补齐** ⬜
  - 做什么：改标题/标签/删除。
  - 产出：管理功能齐全。

---

## 阶段 6：端到端联调

- [ ] **31. 真机端到端** ⬜
  - 做什么：真机 + 后端局域网联调，对照 PRD §7 验收清单逐项打勾。
  - 产出：一条真实录音从导入到追问全流程走通。

---

## 阶段 7：作品打磨 + 证据

> 目标：这是秋招真正看的东西——作品门面 + AI 协作证据。

- [ ] **32. 根 README.md** ⬜
  - 做什么：架构图（可用 mermaid）+ 功能截图 + 技术栈 + 「AI 协作开发过程」章节。
  - 产出：面试官一眼看懂的门面。

- [ ] **33. devlog 整理** ⬜
  - 做什么：把开发过程中的 prompt/纠错/决策整理进 `docs/devlog.md`。
  - 产出：能讲出「我怎么用 AI 开发」的弹药。
  - 参考：tech-spec §7

- [ ] **34. 简历提炼** ⬜
  - 做什么：把项目拆成 3 个 bullet + 一个量化点，写进个人资料。
  - 产出：简历上能写、能讲满 2 分钟。

---

## 阶段 8：进阶优化（未来，非 MVP）

> 目标：MVP 稳定后按兴趣/时间逐步加。🟡 不阻塞主线，做完 31 再说。

- [ ] **35. App 内录音** 🟡 —— 复用 AudioRecord + Opus（小智AI 强项），作为第二录音来源。
- [ ] **36. 转写文本校对编辑** 🟡 —— 转写有错字可手动改。
- [ ] **37. AI 自动打标签** 🟡 —— 按内容自动归类（八股/项目/行为面/手撕）。
- [ ] **38. 跨录音聚合分析** 🟡 —— 「最近 5 场高频问题」「薄弱模块」。
- [ ] **39. 导出复盘报告** 🟡 —— 生成 Markdown/PDF 存档。
- [ ] **40. 转写完成通知** 🟡 —— 转写/总结完成推送提醒。
- [ ] **41. 多用户 + JWT** 🟡 —— 注册登录、数据隔离。
- [ ] **42. 云部署 + 对象存储** 🟡 —— 后端上云，录音存 OSS。
- [ ] **43. 本地 whisper 替换云 ASR** 🟡 —— 降成本、数据可控。
- [ ] **44. Redis 缓存 + 性能优化** 🟡 —— 高频接口缓存、慢查询优化。

---

## 当前焦点

**下一件事 = 第 3 项：git init + 首次提交。**
（第 1、2 项已完成；第 3 项起进入准备动作。）
