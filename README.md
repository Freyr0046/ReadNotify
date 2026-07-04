# ReadNotify — AI Agent Workflow Showcase

> 這份 repo 記錄的不只是一個「唸通知」的 Android App，而是一套用 [Claude Code](https://claude.com/claude-code) 自訂 skills 打造的**受控（gated）AI 工程流程**：從一份中文 PRD 開始，經過 Spec → Plan → Contract → Tasks → Implement，逐 commit 落地成一支通過品質關卡的 Android App。所有過程都留下了可追溯的文件與 commit 紀錄，而不是「丟給 AI 一個 prompt，生出一坨程式碼」。

---

## 這個 repo 想證明什麼

一般對「AI 寫 App」的疑慮是：需求會被腦補、架構會走鐘、Review 會流於形式。這個專案用 `.claude/` 底下的一組自訂 skills，把這些疑慮逐一做成**可驗證的關卡**：

| 疑慮 | 對應機制 | 證據 |
|---|---|---|
| 需求被腦補 | Spec 前先掃描 PRD 盲點，人工逐項確認才繼續 | [`doc/phase1-specify-blind-spots.md`](doc/phase1-specify-blind-spots.md)、[`doc/phase1-specify-assumptions.md`](doc/phase1-specify-assumptions.md) |
| 架構邊界模糊 | Contract Lock：介面/測試樁先鎖定，後續任務不得更動 | [`doc/phase3-contract-lock.md`](doc/phase3-contract-lock.md) |
| 大爆炸式實作難以驗證 | 拆成 18 個獨立任務，每個任務都有驗收標準與驗證指令 | [`doc/phase4-tasks.md`](doc/phase4-tasks.md)、git log 的 `Task 1`–`Task 18` |
| Review 抓不到真的 bug | Code review skill 實際擋下 BLOCKER，事後自我 review 揪出記憶體洩漏 | `fe59c98`、`69b8dcd`、`bea0f99`、`97a50d6`（見下方） |
| 完成度自己說了算 | Success Criteria 逐項驗證，未通過項目誠實記錄原因，不美化 | [`doc/phase5-success-criteria.md`](doc/phase5-success-criteria.md)（16 項中 13 項完整通過，3 項標明具體限制） |

---

## 核心流程：Gated Spec-Driven Development

由 [`.claude/skills/android-spec-driven-development`](.claude/skills/android-spec-driven-development/SKILL.md) 驅動，**每個 Phase 之間都有人工確認關卡**，AI 不能自己決定跳過：

```
SPECIFY ──→ PLAN ──→ CONTRACT ──→ TASKS ──→ IMPLEMENT ──→ [ESCALATE]
   │          │          │          │          │                │
   ▼          ▼          ▼          ▼          ▼                ▼
 Human      Human      Human      Human     Auto loop        Human
 reviews    reviews    reviews    reviews   (max 3x)         decides
```

流程內建「Skip Guard」防呆機制：只要偵測到使用者訊息暗示跳過尚未完成的步驟（例如「直接做」「跳過」），就必須先列出風險並要求使用者明確回覆「確定跳過」才會放行，否則一律照順序把剩下的步驟走完。

這套流程在本專案的真實產出：

| Phase | 產出文件 | 內容 |
|---|---|---|
| 0. PRD | [`doc/SPEC.md`](doc/SPEC.md) | 三階段產品藍圖（本次僅實作 Phase 1 MVP） |
| 1. Specify | [`doc/phase1-spec.md`](doc/phase1-spec.md) | 逐模組功能規格、極端情況與防禦行為 |
| 2. Plan | [`doc/phase2-plan.md`](doc/phase2-plan.md) | M0–M7 里程碑與依賴圖 |
| 3. Contract Lock | [`doc/phase3-contract-lock.md`](doc/phase3-contract-lock.md) | 鎖定 `MainUiState` / `MainViewIntent` / Repository 介面與測試樁 |
| 4. Tasks | [`doc/phase4-tasks.md`](doc/phase4-tasks.md) | 拆解為 18 個任務，各自附驗收標準、驗證指令、涉及檔案 |
| 5. Implement | git log `Task 1`–`Task 18` | 每個任務對應一個獨立 commit，逐步落地 |
| 5.5 Success Criteria | [`doc/phase5-success-criteria.md`](doc/phase5-success-criteria.md) | 16 項驗收標準逐一實機/單元測試驗證 |

任務與 commit 的一一對應（節錄）：

```
61c492f feat: implement BuildAnnouncementUseCase (Task 1)
2b5864d feat: implement whitelist DataStore repository + use cases (Task 2)
3f9c7a4 feat: implement installed-app repository + use case (Task 3)
       ...
6cde246 feat: convert MainActivity to Single-Activity Compose host (Task 13)
2f61da3 test: add Compose UI happy-path tests for MainScreen (Task 14)
429df5c feat: refactor NotificationService into a thin transport layer (Task 15)
b577846 chore: remove obsolete View-system files replaced by Compose (Task 16)
91d6627 chore: pass the full detekt/ktlint/test gate (Task 17)
332a65f docs: Success Criteria walkthrough and Play Store prep (Task 18)
```

---

## 品質關卡：AI 抓到的真實問題

流程不是走過場——`android-reviewer` / PR review skills 與自我 review 環節，在這個專案裡實際擋下並修正了會上線的 bug：

- **`fe59c98`** — `fix: remove dead NotifyForegroundService (PR review BLOCKER/IMPORTANT)`：PR review 擋下一個未串接卻殘留、會誤導後續維護者的 Service。
- **`69b8dcd`** — `fix: self-review findings — TTS leak, whitelist race, icon-load jank`：實作完成後主動自我 review，抓出 TTS 資源洩漏、白名單 race condition、圖示載入卡頓三個問題。
- **`97a50d6`** / **`bea0f99`** — 針對 `TextToSpeech` 在 coroutine 取消時未正確關閉引擎、停止播放的生命週期修正，符合 `.claude/CLAUDE.md` 訂的「協程安全」與「記憶體洩漏防範」規則。
- **`593add7`** — `fix: skip group-summary notifications to stop double-speaking messages`：Success Criteria 走查時發現的實際行為缺陷並修正。

---

## Skills 一覽

`.claude/skills/` 下共 14 個自訂 skill，依用途分四類：

**規劃與拆解**
- `android-spec-driven-development` — PRD → Spec → Plan → Contract → Tasks 的受控流程（本專案主幹）
- `planning-and-task-breakdown` — 把 spec 拆解為可獨立驗證的任務
- `incremental-implementation` — 強制以小步快跑方式實作，每個增量都要能跑、能測、能驗證

**版本控制與提交**
- `git-workflow-and-versioning` — Trunk-based 開發、原子化 commit 紀律
- `smart-commit` — 自動從 git log 推斷 ticket 編號與 commit 類型

**Code Review 自動化**
- `android-reviewer` — 依 Clean Architecture、生命週期、並發安全、Compose 效能等面向做生產級審查
- `android-pr-review` / `android-pr-review-optimized` — 一鍵建立 PR、執行審查、以繁體中文發布 review comments（optimized 版本針對大型 PR 做分層 diff 與 token 成本優化）
- `android-pr-fixer` — 讀取 PR review comments，依 BLOCKER/IMPORTANT/OPTIONAL 分類後自動修正

**設計轉程式碼 / 文件**
- `android-layout-creator` — 將 Figma 設計轉為 ViewBinding + XML 的 Fragment
- `figma-to-android-png` / `figma-to-avd` — 將 Figma 節點匯出為五種密度 PNG 或 Vector Drawable，直接落地到 `res/`
- `android-api-scaffold` — 依 API Spec 產生 Model / ApiService / Repository
- `documentation-and-adrs` — 記錄架構決策與文件，供後續工程師與 AI agent 使用

搭配 [`.claude/CLAUDE.md`](.claude/CLAUDE.md) 作為全域護欄：強制 Clean Architecture 分層、ViewBinding 記憶體安全模式、ViewModel 不得持有 Context、協程只能在對應 scope 啟動、`!!` 禁用等規則，並且**要求每次 code review 輸出都遵循固定格式**（Summary / Detected Issues / Suggested Code / Positive Observations）。

---

## App 本身：ReadNotify

上述流程落地出來的產品，是一支 Phase 1 MVP：在特定情境（如開車、運動、雙手不便）下，透過 `NotificationListenerService` 攔截白名單 App 的通知，經 `TextToSpeech` 唸出。

核心設計原則：
- **高防禦性**：權限被撤銷時強制導回設定頁、TTS 引擎缺失或不支援語系時靜音跳過、自身通知永遠鎖進黑名單避免無限迴圈
- **低干擾性**：預設白名單全關、佇列積壓超過上限即捨棄、群組摘要通知不重複播報
- **隱私優先**：僅播報使用者主動勾選的 App

### 架構

Clean Architecture + Unidirectional Data Flow：

```
UI (Jetpack Compose)
   ↓ Intent          ↑ StateFlow<MainUiState>
ViewModel (Hilt, viewModelScope)
   ↓
UseCase (Domain layer，不含 Android 依賴)
   ↓
Repository (interface 在 domain，實作在 data)
   ↓
DataSource (NotificationListenerService / TextToSpeech / DataStore)
```

對應目錄：

```
app/src/main/java/com/freyr/readmynotify/
├── data/
│   ├── datasource/       # NotificationListenerService、TTS engine 等系統邊界
│   └── repository/       # Repository 實作
├── domain/
│   ├── model/
│   ├── repository/       # Repository 介面（Contract Lock 產物）
│   └── usecase/
├── di/                   # Hilt modules
└── ui/main/              # Compose UiState / ViewModel / Screen
```

### 技術棧

Kotlin · Jetpack Compose · Hilt · Coroutines/Flow · Navigation Compose · DataStore (Preferences) · JUnit5 / MockK / Turbine · detekt / ktlint

### 建置

```bash
./gradlew assembleDebug
./gradlew detekt ktlintCheck :app:testDebugUnitTest   # 對應 Task 17 的 full gate
```
