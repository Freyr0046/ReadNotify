# Implementation Plan: ReadNotify Phase 1 MVP

- **文件狀態**：待人工審閱（PENDING HUMAN REVIEW）
- **建立日期**：2026-07-04
- **對應 Spec**：`doc/phase1-spec.md`（已審閱確認，Open Questions 為零）
- **產出階段**：`android-spec-driven-development` Skill / Phase 2 (Plan)
  （依 `planning-and-task-breakdown` skill 的 Step 2「識別依賴圖」為主，
  細部任務拆解留待 Phase 4）

---

## Overview

依照 `doc/phase1-spec.md` 的 Clean Architecture Layer Map，由下而上
（Data → Domain → ViewModel → UI → System 轉接層）規劃 Phase 1 MVP 的
實作依賴圖與里程碑順序。Android Clean Architecture 的分層特性決定了
本次採**分層由下而上**而非通用規劃 Skill 建議的「垂直切片」，但每個里程碑
內部仍盡量以獨立、可平行開發的最小單位切分，並在每個里程碑後安排
Checkpoint。

---

## Architecture Decisions（延續 Spec，不重複展開）

- Hilt 為新導入 DI 框架，`@Singleton` scope 用於 `SpeechQueueRepositoryImpl`
  與 `TtsEngineRepositoryImpl`（兩者需要跨 Service/ViewModel 共享同一實例
  才能達成狀態同步）
- `BuildAnnouncementUseCase` 為純函式（無 Repository 依賴），可與 Data 層
  完全平行開發，優先安排以盡早驗證核心文字處理規則
- `SpeechQueueRepositoryImpl` 依賴 `TtsEngineRepositoryImpl`（需呼叫
  `speak()` 並監聽播放完成回呼），兩者不可平行、需先完成 TTS 封裝

---

## 依賴圖（Dependency Graph）

```
Task 0：Gradle 環境建置
（Hilt / Compose / Navigation Compose / DataStore / core-splashscreen /
 JUnit5+Mockk+Turbine / detekt+ktlint）
    │
    ├── Domain 層：Repository 介面 + Domain Models（純 Kotlin，無外部依賴）
    │       │
    │       ├──▶ Data：NotificationWhitelistRepositoryImpl（DataStore）
    │       ├──▶ Data：InstalledAppRepositoryImpl（PackageManager）
    │       ├──▶ Data：NotificationAccessRepositoryImpl（Settings.Secure）
    │       ├──▶ Data：TtsEngineRepositoryImpl（封裝 TextToSpeech）
    │       │        │
    │       │        └──▶ Data：SpeechQueueRepositoryImpl
    │       │                 （依賴 TtsEngineRepository，記憶體佇列 + 上限5）
    │       │
    │       └──▶ Hilt Module（介面 → 實作綁定，@Singleton scope 設定）
    │                │
    │                ├──▶ UseCase：GetInstalledAppsUseCase
    │                ├──▶ UseCase：ObserveWhitelistUseCase
    │                ├──▶ UseCase：SetAppWhitelistedUseCase
    │                ├──▶ UseCase：CheckNotificationAccessUseCase
    │                │        （以上四者為薄封裝，各自僅依賴單一 Repository，
    │                │         彼此完全獨立、可平行開發）
    │                │
    │                ├──▶ UseCase：BuildAnnouncementUseCase
    │                │        （純函式：模板/截斷/URL 取代，無 Repository 依賴，
    │                │         可與 Task 0 之後的任何時間點平行開發，優先完成）
    │                │
    │                ├──▶ UseCase：EnqueueAnnouncementUseCase
    │                │        （依賴 SpeechQueueRepository + BuildAnnouncementUseCase）
    │                │
    │                └──▶ UseCase：SendTestNotificationUseCase
    │                         （依賴 NotificationAccessRepository +
    │                          自我測試標記機制 Constants.SELF_TEST_NOTIFICATION_TAG）
    │                              │
    │                              └──▶ MainViewModel (@HiltViewModel)
    │                                     聚合全部 UseCase → StateFlow<MainUiState>
    │                                          │
    │                                          ├──▶ Compose components
    │                                          │     （AppWhitelistRow /
    │                                          │      PermissionDeniedOverlay /
    │                                          │      EngineErrorDialog /
    │                                          │      TtsPlayingBanner —
    │                                          │      四者互相獨立、可平行開發）
    │                                          │
    │                                          ├──▶ MainScreen（組裝上述元件）
    │                                          │        │
    │                                          │        └──▶ MainActivity
    │                                          │             （Single-Activity host +
    │                                          │              core-splashscreen +
    │                                          │              Navigation Compose，
    │                                          │              取代 SplashActivity）
    │                                          │
    │                                          └──▶ NotificationService 重構
    │                                                （依賴 EnqueueAnnouncementUseCase +
    │                                                 BuildAnnouncementUseCase，經由
    │                                                 @AndroidEntryPoint 注入；移除
    │                                                 舊 Firestore 呼叫與寫死判斷）
    │
    └── 測試任務（與對應層平行撰寫，TDD 風格）：
            Repository 單元測試 ── 隨 Data 層任務同步完成
            UseCase 單元測試   ── 隨 Domain 層任務同步完成
            ViewModel 單元測試 ── 隨 MainViewModel 任務同步完成
            Compose UI 測試   ── 隨 UI 任務同步完成（僅 happy path）
```

`NotifyForegroundService` 不在此依賴圖內（Step 1 決議：本次不重構，維持
現狀，無任何任務觸碰此檔案）。

---

## 里程碑劃分（高階，細部任務拆解於 Phase 4）

### M0：建置環境
- 新增 Hilt、Compose BOM、Navigation Compose、DataStore、
  `core-splashscreen`、JUnit5/Mockk/Turbine、detekt/ktlint 等 Gradle 依賴
- 新增 `@HiltAndroidApp` Application class 並註冊於 Manifest
- **Checkpoint**：`./gradlew assembleDebug` 成功、`./gradlew detekt ktlintCheck`
  可執行（即使規則尚未針對舊檔案調整，至少工具鏈可跑）

### M1：Domain 契約 + 純函式業務規則
- 定義全部 Repository 介面與 Domain Models（`InstalledApp`、
  `Announcement`、`PlaybackState`、`TtsEngineState`、`EngineErrorReason` 等）
- 實作 `BuildAnnouncementUseCase`（模板／50字截斷／URL 正則取代）
- 對應單元測試（邊界值 49/50/51 字、URL 取代、空內容防禦）
- **可與 M2 完全平行**，因為此里程碑不依賴任何 Android Framework API
- **Checkpoint**：`BuildAnnouncementUseCase` 測試覆蓋率涵蓋 PRD 全部截斷/
  URL 案例並全數通過

### M2：Data 層四個 Repository 實作
- `NotificationWhitelistRepositoryImpl`（DataStore，預設空集合）
- `InstalledAppRepositoryImpl`（PackageManager 查詢，處理
  `QUERY_ALL_PACKAGES` 情境）
- `NotificationAccessRepositoryImpl`（沿用並搬遷現有
  `NotifyFragment.checkNotifyPermission()` 邏輯）
- `TtsEngineRepositoryImpl`（封裝 `TextToSpeech` 初始化/語系檢查/錯誤映射）
- 四者互相獨立，**可平行開發**；各自的單元測試同步完成
- **Checkpoint**：四個 Repository 皆有測試覆蓋，`Result<T>` 錯誤路徑（無
  TTS 引擎、語系不支援、DataStore 初始為空）皆已驗證

### M3：播報佇列鏈路
- `SpeechQueueRepositoryImpl`（依賴 M2 的 `TtsEngineRepositoryImpl`，
  記憶體佇列、上限 5 條捨棄邏輯、`UtteranceProgressListener` 橋接
  `StateFlow<PlaybackState>`）
- `EnqueueAnnouncementUseCase`、`SendTestNotificationUseCase`
- **依賴 M1 + M2 完成**，為序列任務（此里程碑不可平行於 M1/M2 開始，
  但佇列上限邏輯與測試通知標記邏輯兩者之間可平行）
- **Checkpoint**：可用測試腳本模擬「連續 6 條通知」驗證第 6 條被捨棄；
  `SendTestNotificationUseCase` 產生的標記可被辨識

### M4：MainViewModel 狀態機
- 聚合 M1–M3 全部 UseCase，輸出 `StateFlow<MainUiState>`，處理全部
  `MainViewIntent`（含 `OnScreenResumed` 觸發權限重檢）
- ViewModel 單元測試（JUnit5+Mockk+Turbine）覆蓋五態轉換
- **依賴 M1–M3 完成**
- **Checkpoint**：五態轉換測試全數通過，且無 UI 程式碼的情況下即可用
  測試驗證完整狀態機邏輯

### M5：Compose UI
- 四個獨立元件（`AppWhitelistRow`、`PermissionDeniedOverlay`、
  `EngineErrorDialog`、`TtsPlayingBanner`）**可平行開發**
- `MainScreen` 組裝元件、`MainActivity`（Single-Activity + 
  `core-splashscreen` + Navigation Compose，取代 `SplashActivity`）
- Compose UI 測試（happy path：權限阻擋、白名單勾選、測試通知觸發播報）
- **依賴 M4 完成**（ViewModel 狀態機需先就緒）
- **Checkpoint**：實機／模擬器手動走過五種狀態畫面皆正確渲染；
  `SplashActivity` 移除後冷啟動流程無閃爍或崩潰

### M6：NotificationService 重構
- 精簡為轉接層：空值檢查 → 自我黑名單（含測試標記例外）→ 呼叫
  `EnqueueAnnouncementUseCase`
- 移除舊 `line`/`facebook`/`instagram` 寫死判斷、移除 Firebase Firestore
  上傳呼叫
- **依賴 M1、M3 完成**（需要 `BuildAnnouncementUseCase` 與
  `EnqueueAnnouncementUseCase` 就緒），**與 M4/M5 可平行**（UI 與 Service
  重構互不依賴，僅共同依賴 Domain 層）
- **Checkpoint**：端對端手動測試——白名單 App 送出真實通知後，背景
  确實觸發播報且前景 UI 同步更新為 `TtsPlaying`

### M7：收尾與上架準備
- 手動實機驗證 Success Criteria 全部項目（`doc/phase1-spec.md` 清單）
- 準備 Google Play `QUERY_ALL_PACKAGES` 權限使用聲明表單草稿
- Full gate：`./gradlew detekt ktlintCheck :app:testDebugUnitTest`
- **Checkpoint**：`doc/phase1-spec.md` Success Criteria 逐項打勾，
  準備進入 PR 審查

---

## 平行化建議（Parallelization）

**可平行：**
- M1（純函式 UseCase）與 M2（Data 層四個 Repository）
- M2 內部四個 Repository 彼此獨立
- M5 內部四個 Compose 元件彼此獨立
- M6（NotificationService 重構）與 M4/M5（ViewModel/UI）—— 三者共同
  依賴 M1+M3，但彼此互不依賴
- 每層的單元測試與該層實作同步撰寫

**必須序列：**
- M0 → 其餘所有里程碑（工具鏈與依賴必須先就緒）
- M3 依賴 M2 的 `TtsEngineRepositoryImpl`（佇列需要先有可呼叫的 TTS 封裝）
- M4 依賴 M1–M3 全部 UseCase 完成
- M5 依賴 M4（UI 需綁定已完成的 ViewModel 狀態機）

---

## Risks and Mitigations

| Risk | Impact | Mitigation |
|------|--------|------------|
| Hilt 為專案首次導入，`@HiltAndroidApp`/Manifest 設定遺漏會導致編譯期或執行期崩潰 | High | M0 完成後立即以最小可行注入（如注入一個假 UseCase 到 Activity）驗證 DI 圖可解析，再繼續後續里程碑 |
| 移除 `SplashActivity`、改用 `core-splashscreen` 可能影響冷啟動視覺（現有 `LaunchTheme`/`activity_splash.xml` 邏輯）| Medium | M5 Checkpoint 明確要求實機驗證冷啟動無閃爍，並保留現有品牌視覺（icon/主色）於新 SplashScreen 設定 |
| `QUERY_ALL_PACKAGES` 上架審查可能延誤發布時程 | Medium | M7 及早準備聲明表單草稿，不等到最後才處理；若審查延遲不影響內部測試/側載安裝 |
| `TextToSpeech` 的 `UtteranceProgressListener` 回呼在背景執行緒觸發，`SpeechQueueRepositoryImpl` 更新 `StateFlow` 需注意執行緒安全 | Medium | M3 實作時明確使用 `MutableStateFlow.update {}` 或切換至安全 Dispatcher 更新，並於單元測試中驗證併發呼叫下佇列計數正確 |
| Firebase Firestore 依賴保留但呼叫移除，若遺漏移除會違反 Spec 隱私要求 | Low | M6 Checkpoint 明確要求 grep 確認 `NotificationService.kt` 內無任何 `FirebaseFirestore` 呼叫殘留 |

---

## Open Questions

無。若審閱過程中發現里程碑劃分需調整，將回頭更新本文件後再繼續。

---

## 下一步

請人工審閱本計畫（依賴圖順序是否合理、里程碑劃分是否可執行、風險緩解
是否足夠）。確認無誤後，將進入：

- **Phase 3（Contract Lock）**：撰寫 Gherkin 情境、鎖定 Repository/UseCase
  介面與 `MainViewModel` 測試樁
- 產出文件：`doc/phase3-contract-lock.md`
