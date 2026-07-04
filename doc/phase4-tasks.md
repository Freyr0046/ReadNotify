# Phase 4: Tasks — ReadNotify Phase 1 MVP

- **文件狀態**：待人工審閱（PENDING HUMAN REVIEW）
- **建立日期**：2026-07-04
- **對應文件**：`doc/phase1-spec.md`（Spec）、`doc/phase2-plan.md`（Plan/依賴圖）、
  `doc/phase3-contract-lock.md`（已鎖定的介面與測試樁）
- **產出階段**：`android-spec-driven-development` Skill / Phase 4 (Tasks)
  （依 `planning-and-task-breakdown` skill 的任務格式）

---

## Overview

把 `doc/phase2-plan.md` 的 7 個里程碑（M0–M7）拆解為 18 個可獨立驗證的任務。
每個任務皆有明確驗收標準、驗證指令、依賴關係與涉及檔案，任務大小控制在
S/M（多數）、少數 L（ViewModel 整合任務，已刻意再切分為兩個以避免過大）。

Repository / UseCase 介面與 `MainUiState`/`MainViewIntent`/`MainViewModel`
建構子已於 Phase 3 **鎖定**，以下任務僅**填入實作**，不得更動這些介面與
`MainViewModelTest.kt` 的測試樁。

---

## Task 0：建置環境設定

**Description：** 新增 Hilt、Jetpack Compose、Navigation Compose、
DataStore (Preferences)、`core-splashscreen`、detekt/ktlint 等 Gradle
依賴與 plugin，並建立 `@HiltAndroidApp` Application class。
（JUnit5/Mockk/Turbine/Coroutines 已於 Phase 3 加入，本任務不重複。）

**Acceptance criteria：**
- [ ] `ReadNotifyApplication`（`@HiltAndroidApp`）建立並註冊於
      `AndroidManifest.xml` 的 `android:name`
- [ ] `buildFeatures { compose true }` 啟用，Compose BOM 已加入
- [ ] detekt / ktlint plugin 加入且可執行（初次執行允許有既有檔案的違規，
      作為後續 baseline）

**Verification：**
- [ ] `sh gradlew assembleDebug` 成功
- [ ] `sh gradlew detekt ktlintCheck` 可執行完畢（不要求零違規，只要求可跑）

**Dependencies：** None

**Files likely touched：**
- `app/build.gradle`、`build.gradle`（root，detekt classpath）
- 新增 `app/src/main/java/com/freyr/readmynotify/ReadNotifyApplication.kt`
- `app/src/main/AndroidManifest.xml`

**Estimated scope：** M

---

### Checkpoint：Task 0
- [ ] `assembleDebug` 成功、既有功能未被破壞（現有 Fragment/Firestore 邏輯
      仍可執行，尚未動它們）
- [ ] 與人工確認建置工具鏈無誤後才繼續

---

## Task 1：`BuildAnnouncementUseCase` 實作

**Description：** 填入 Phase 3 鎖定介面 `BuildAnnouncementUseCase` 的實作：
套用模板、50 字截斷、URL 正則取代（取代需在截斷之前執行）。

**Acceptance criteria：**
- [ ] `title`/`content` 任一為 null 或空字串時回傳 `null`
- [ ] 內容 ≤ 50 字：完整播報，不加尾碼
- [ ] 內容 > 50 字：截斷前 50 字 + `「...等省略內容」`
- [ ] URL（`https?://\S+`）取代為「網址」，且取代後才計算截斷長度

**Verification：**
- [ ] `sh gradlew :app:testDebugUnitTest --tests "*BuildAnnouncementUseCaseImplTest"`
      涵蓋 49/50/51 字邊界與 URL 取代案例

**Dependencies：** None（純函式，僅依賴 Phase 3 已鎖定的 Domain Model）

**Files likely touched：**
- `domain/usecase/impl/BuildAnnouncementUseCaseImpl.kt`
- `app/src/test/.../BuildAnnouncementUseCaseImplTest.kt`

**Estimated scope：** S

---

## Task 2：白名單 Repository + UseCase 實作（DataStore）

**Description：** 實作 `NotificationWhitelistRepository`（DataStore
Preferences，預設空集合）與其對應的 `ObserveWhitelistUseCase`、
`SetAppWhitelistedUseCase` 薄封裝實作。

**Acceptance criteria：**
- [ ] 未設定過時，`observeWhitelist()` 發出空集合
- [ ] `setAppEnabled(pkg, true/false)` 正確增/刪 DataStore 中的 package name
- [ ] 寫入具持久性（跨 Repository 實例仍讀得到）

**Verification：**
- [ ] `sh gradlew :app:testDebugUnitTest --tests "*NotificationWhitelistRepositoryImplTest"`

**Dependencies：** Task 0（DataStore 依賴）

**Files likely touched：**
- `data/datasource/WhitelistPreferencesDataSource.kt`
- `data/repository/NotificationWhitelistRepositoryImpl.kt`
- `domain/usecase/impl/ObserveWhitelistUseCaseImpl.kt`
- `domain/usecase/impl/SetAppWhitelistedUseCaseImpl.kt`
- 對應測試檔

**Estimated scope：** M

---

## Task 3：已安裝 App Repository + UseCase 實作（PackageManager）

**Description：** 實作 `InstalledAppRepository`（`PackageManager` 查詢已安裝
App）與 `GetInstalledAppsUseCase` 實作。於 Manifest 宣告
`QUERY_ALL_PACKAGES`。

**Acceptance criteria：**
- [ ] 回傳 `Result.success(list)`，依 `label` 排序
- [ ] `AndroidManifest.xml` 已宣告 `QUERY_ALL_PACKAGES`

**Verification：**
- [ ] `sh gradlew :app:testDebugUnitTest --tests "*InstalledAppRepositoryImplTest"`

**Dependencies：** Task 0

**Files likely touched：**
- `data/repository/InstalledAppRepositoryImpl.kt`
- `domain/usecase/impl/GetInstalledAppsUseCaseImpl.kt`
- `app/src/main/AndroidManifest.xml`
- 對應測試檔

**Estimated scope：** S

---

## Task 4：通知存取權限 Repository + UseCase 實作

**Description：** 將現有 `NotifyFragment.checkNotifyPermission()` 邏輯搬遷至
`NotificationAccessRepositoryImpl`（比對
`Settings.Secure` `enabled_notification_listeners`），並實作
`CheckNotificationAccessUseCase`。

**Acceptance criteria：**
- [ ] 權限已授權/未授權兩種情況皆正確判斷（沿用現有邏輯，補上單元測試）

**Verification：**
- [ ] `sh gradlew :app:testDebugUnitTest --tests "*NotificationAccessRepositoryImplTest"`

**Dependencies：** None

**Files likely touched：**
- `data/repository/NotificationAccessRepositoryImpl.kt`
- `domain/usecase/impl/CheckNotificationAccessUseCaseImpl.kt`
- 對應測試檔

**Estimated scope：** S

---

## Task 5：TTS 引擎 Repository + UseCase 實作

**Description：** 封裝 `android.speech.tts.TextToSpeech`，實作
`TtsEngineRepository`（含 `engineState: StateFlow`）、
`InitializeTtsEngineUseCase`、`ObserveEngineStateUseCase`。

**Acceptance criteria：**
- [ ] 無 TTS 引擎 → `initialize()` 回傳 `Result.failure`，
      `engineState` 為 `Error(TTS_ENGINE_NOT_INSTALLED)`
- [ ] 語系不支援（`LANG_MISSING_DATA`/`LANG_NOT_SUPPORTED`）→
      `Error(LANGUAGE_UNSUPPORTED)`
- [ ] 初始化成功 → `engineState` 為 `Ready`
- [ ] `speak()` 於語系不支援時回傳 `Result.failure`，不崩潰

**Verification：**
- [ ] `sh gradlew :app:testDebugUnitTest --tests "*TtsEngineRepositoryImplTest"`

**Dependencies：** Task 0

**Files likely touched：**
- `data/repository/TtsEngineRepositoryImpl.kt`
- `domain/usecase/impl/InitializeTtsEngineUseCaseImpl.kt`
- `domain/usecase/impl/ObserveEngineStateUseCaseImpl.kt`
- 對應測試檔

**Estimated scope：** M

---

### Checkpoint：Task 1–5（對應 Plan M1+M2）
- [ ] `sh gradlew :app:testDebugUnitTest` 全綠（不含尚未實作的
      `MainViewModelTest`，該檔案此時仍應全部失敗，這是預期狀態）
- [ ] Task 1–5 彼此獨立，**已平行完成**
- [ ] 與人工確認 Data 層行為無誤後才進入播報佇列

---

## Task 6：播報佇列 Repository + UseCase 實作

**Description：** 實作 `SpeechQueueRepositoryImpl`：記憶體佇列、上限 5 條
（超過直接捨棄）、透過 `UtteranceProgressListener` 更新
`playbackState: StateFlow<PlaybackState>`。實作 `EnqueueAnnouncementUseCase`
（呼叫 `SpeechQueueRepository.enqueue`）與 `ObservePlaybackStateUseCase`。

**Acceptance criteria：**
- [ ] 連續 enqueue 6 條時，第 6 條被靜默捨棄（不拋錯、不阻塞）
- [ ] `speak()` 開始時 `playbackState` 變為 `Speaking(appLabel)`，結束時變回
      `Idle`
- [ ] `StateFlow` 更新在正確 Dispatcher 上執行（`UtteranceProgressListener`
      回呼為背景執行緒，需注意執行緒安全）

**Verification：**
- [ ] `sh gradlew :app:testDebugUnitTest --tests "*SpeechQueueRepositoryImplTest"`
      模擬連續 6 條通知驗證第 6 條被捨棄

**Dependencies：** Task 5（`TtsEngineRepository`）

**Files likely touched：**
- `data/repository/SpeechQueueRepositoryImpl.kt`
- `domain/usecase/impl/EnqueueAnnouncementUseCaseImpl.kt`
- `domain/usecase/impl/ObservePlaybackStateUseCaseImpl.kt`
- 對應測試檔

**Estimated scope：** M

---

## Task 7：自我測試通知 UseCase 實作

**Description：** 實作 `SendTestNotificationUseCase`：延遲 2 秒後發出一條
帶有 `Constants.SELF_TEST_NOTIFICATION_TAG` 標記的模擬通知。

⚠ 實作備註：發送通知需要 `Context`/`NotificationManagerCompat`，依
CLAUDE.md 規則 Domain 層不可持有 `Context`。因此需新增一個 Data 層小型
介面（例如 `TestNotificationSender`），`SendTestNotificationUseCaseImpl`
只依賴此介面，實作類別放在 `data` package 並注入 `@ApplicationContext`。
這是 Task 7 執行時才會具體定形的新介面，不在 Phase 3 鎖定範圍內，屬於
實作細節而非跨層公開合約，完成後**不需要**回頭修改 Phase 3 文件。

**Acceptance criteria：**
- [ ] 呼叫後延遲 2 秒發出通知，`tag == Constants.SELF_TEST_NOTIFICATION_TAG`
- [ ] `NotificationService` 收到此標記時應放行（驗證邏輯在 Task 15）

**Verification：**
- [ ] `sh gradlew :app:testDebugUnitTest --tests "*SendTestNotificationUseCaseImplTest"`

**Dependencies：** Task 0

**Files likely touched：**
- `data/notification/TestNotificationSender.kt`（新介面+實作）
- `domain/usecase/impl/SendTestNotificationUseCaseImpl.kt`
- `common/Constants.kt`（新增 `SELF_TEST_NOTIFICATION_TAG`）
- 對應測試檔

**Estimated scope：** S

---

## Task 8：`MainViewModel` 實作（一）— Init / Permission / EngineError

**Description：** 填入 `MainViewModel` 的初始化流程：呼叫
`CheckNotificationAccessUseCase` → 若未授權轉 `PermissionDenied`；若授權則
呼叫 `InitializeTtsEngineUseCase` → 失敗轉 `EngineError`、成功轉
`IdleConfig`（合併 `GetInstalledAppsUseCase` 與 `ObserveWhitelistUseCase`）。
同時實作 `OnScreenResumed`、`OnRetryEngineInitClicked` 意圖處理。

**Acceptance criteria（對應 `MainViewModelTest.kt` 內 5 個測試由 TODO 轉為
真正實作並通過）：**
- [ ] `initial state is InitChecking`
- [ ] `permission denied transitions to PermissionDenied`
- [ ] `permission granted and tts ready transitions to IdleConfig`
- [ ] `tts engine not installed transitions to EngineError`
- [ ] `OnScreenResumed with revoked permission force-switches to PermissionDenied`

**Verification：**
- [ ] `sh gradlew :app:testDebugUnitTest --tests "*MainViewModelTest"`
      上述 5 個測試通過（其餘 5 個仍會失敗，屬 Task 9 範圍，此時可接受）

**Dependencies：** Task 2, 3, 4, 5（全部 Init 流程需要的 UseCase）

**Files likely touched：**
- `ui/main/MainViewModel.kt`（僅填入方法本體，不得更動建構子）

**Estimated scope：** M

---

## Task 9：`MainViewModel` 實作（二）— Whitelist / Playback / Test / Retry

**Description：** 補完 `MainViewModel` 剩餘意圖處理：白名單勾選、觀察播放
狀態切換 `TtsPlaying`/`IdleConfig`、測試通知按鈕、`EngineError` 重試恢復。

**Acceptance criteria（`MainViewModelTest.kt` 剩餘 5 個測試通過）：**
- [ ] `OnAppWhitelistToggled invokes SetAppWhitelistedUseCase and updates isChecked`
- [ ] `playback Speaking state transitions to TtsPlaying`
- [ ] `playback Idle state returns to IdleConfig`
- [ ] `OnSendTestNotificationClicked invokes SendTestNotificationUseCase exactly once`
- [ ] `OnRetryEngineInitClicked recovers from EngineError to IdleConfig`

**Verification：**
- [ ] `sh gradlew :app:testDebugUnitTest --tests "*MainViewModelTest"` **10/10 全通過**

**Dependencies：** Task 8（同一檔案的延續）、Task 6、Task 7

**Files likely touched：**
- `ui/main/MainViewModel.kt`

**Estimated scope：** M

---

### Checkpoint：Task 6–9（對應 Plan M3+M4）
- [ ] `MainViewModelTest` 10/10 通過，鎖定測試樁**未被修改或刪除**
- [ ] 此時已具備完整狀態機邏輯，尚無 UI，可用測試完整驗證行為
- [ ] 與人工確認狀態機行為無誤後才進入 UI 開發

---

## Task 10：Compose 元件（一）— AppWhitelistRow / PermissionDeniedOverlay

**Description：** 實作兩個獨立、可預覽（`@Preview`）的 Composable 元件。

**Acceptance criteria：**
- [ ] `AppWhitelistRow` 依 `AppWhitelistItem` 渲染 checkbox + label + icon，
      勾選時觸發回呼（事件上拋，不持有 ViewModel）
- [ ] `PermissionDeniedOverlay` 全螢幕阻擋，僅一顆按鈕觸發回呼

**Verification：**
- [ ] Compose Preview 手動檢視（Android Studio）
- [ ] `sh gradlew assembleDebug` 成功

**Dependencies：** Task 0

**Files likely touched：**
- `ui/main/components/AppWhitelistRow.kt`
- `ui/main/components/PermissionDeniedOverlay.kt`

**Estimated scope：** S

---

## Task 11：Compose 元件（二）— EngineErrorDialog / TtsPlayingBanner

**Description：** 同 Task 10，另兩個獨立元件，可與 Task 10 平行開發。

**Acceptance criteria：**
- [ ] `EngineErrorDialog` 依 `EngineErrorReason` 顯示對應文案，
      `TTS_ENGINE_NOT_INSTALLED` 顯示前往下載按鈕
- [ ] `TtsPlayingBanner` 顯示 `speakingFromLabel`

**Verification：** 同 Task 10

**Dependencies：** Task 0

**Files likely touched：**
- `ui/main/components/EngineErrorDialog.kt`
- `ui/main/components/TtsPlayingBanner.kt`

**Estimated scope：** S

---

## Task 12：`MainScreen` 組裝 + ViewModel 綁定

**Description：** 組裝 Task 10–11 元件，依 `MainUiState` 各分支渲染對應
畫面，透過 `hiltViewModel()` 取得 `MainViewModel`，`DisposableEffect` 監聽
`ON_RESUME` 送出 `OnScreenResumed`。

**Acceptance criteria：**
- [ ] 五種 `MainUiState` 皆有對應渲染分支，無遺漏
- [ ] ViewModel 不向下傳遞給子 Composable（僅在 `MainScreen` 頂層取得，
      事件以 lambda 下傳）
- [ ] `collectAsStateWithLifecycle()` 收集狀態

**Verification：**
- [ ] `sh gradlew assembleDebug` 成功
- [ ] Compose UI 測試（見 Task 14）涵蓋主要分支

**Dependencies：** Task 8, 9（ViewModel 邏輯完成）、Task 10, 11（元件完成）

**Files likely touched：**
- `ui/main/MainScreen.kt`

**Estimated scope：** M

---

## Task 13：`MainActivity` Single-Activity Host + Navigation Compose

**Description：** 將 `MainActivity` 改為 Compose host，掛上
`core-splashscreen`，以 `NavHost`（Navigation Compose）承載 `MainScreen`。
移除 `SplashActivity`。

**Acceptance criteria：**
- [ ] `SplashActivity`、`activity_splash.xml`、舊 `mobile_navigation.xml`
      移除
- [ ] `AndroidManifest.xml` 只剩 `MainActivity` 作為 LAUNCHER
- [ ] 冷啟動視覺（icon/主色）與移除前一致，無白屏閃爍

**Verification：**
- [ ] 實機/模擬器手動測試冷啟動流程（Success Criteria 對應項）
- [ ] `sh gradlew assembleDebug` 成功

**Dependencies：** Task 12

**Files likely touched：**
- `MainActivity.kt`
- 刪除：`SplashActivity.kt`、`activity_splash.xml`、
  `res/navigation/mobile_navigation.xml`
- `AndroidManifest.xml`

**Estimated scope：** M

---

## Task 14：Compose UI 測試（happy path）

**Description：** 撰寫關鍵路徑的 Compose UI 測試。

**Acceptance criteria：**
- [ ] `PermissionDenied` 狀態下阻擋其餘操作
- [ ] 勾選白名單 App 後清單即時反映勾選狀態
- [ ] 按下測試通知後畫面切換為 `TtsPlaying`（可用假的 UseCase/ViewModel
      注入模擬）

**Verification：**
- [ ] `sh gradlew :app:connectedDebugAndroidTest`（或依專案選用的 Compose UI
      測試執行方式）

**Dependencies：** Task 12

**Files likely touched：**
- `app/src/androidTest/.../MainScreenTest.kt`

**Estimated scope：** M

---

### Checkpoint：Task 10–14（對應 Plan M5）
- [ ] 實機手動走過五種 `MainUiState` 畫面皆正確渲染
- [ ] 與人工確認 UI 呈現與互動符合預期後才進入 Service 重構

---

## Task 15：`NotificationService` 重構

**Description：** 精簡 `NotificationService`：空值檢查 → 自我黑名單檢查
（`packageName == BuildConfig.APPLICATION_ID`，`tag ==
SELF_TEST_NOTIFICATION_TAG` 為唯一例外）→ 透過 `@AndroidEntryPoint` 注入
`BuildAnnouncementUseCase` + `EnqueueAnnouncementUseCase`。移除舊
`line`/`facebook`/`instagram` 寫死判斷與全部 Firebase Firestore 呼叫。

**Acceptance criteria：**
- [ ] `grep -r FirebaseFirestore app/src/main/java/com/freyr/readmynotify/NotificationService.kt`
      無結果
- [ ] 自我通知（`packageName == BuildConfig.APPLICATION_ID`）除測試標記外
      一律不進入播報流程
- [ ] 白名單內 App 的合法通知能成功呼叫
      `EnqueueAnnouncementUseCase`

**Verification：**
- [ ] 端對端手動測試：白名單 App 送出真實通知 → 背景播報 → 前景 UI 同步
      `TtsPlaying`（Success Criteria 對應項）
- [ ] 單元測試（抽出純函式部分，如自我黑名單判斷邏輯）

**Dependencies：** Task 1（`BuildAnnouncementUseCase`）、Task 6
（`EnqueueAnnouncementUseCase`）、Task 7（測試標記常數）
**可與 Task 10–14 平行**（僅共同依賴 Domain 層，UI 與 Service 互不依賴）

**Files likely touched：**
- `NotificationService.kt`

**Estimated scope：** M

---

## Task 16：清理現已淘汰的舊檔案

**Description：** 確認 Compose 版本運作正常後，移除被取代的舊 View 系統
檔案。

**Acceptance criteria：**
- [ ] `NotifyFragment.kt`、`NotifyAdapter.kt`、`NotifyViewHolder.kt`、
      `NotifyViewModel.kt`、`fragment_notify.xml`、`row_notify.xml` 移除
- [ ] 專案中無殘留引用（`sh gradlew assembleDebug` 成功即可佐證）
- [ ] `NotifyForegroundService.kt`、`layout_notify.xml`
      **不動**（Step 1 決議 #3，`RemoteViews` 需要原生 XML）

**Verification：**
- [ ] `sh gradlew assembleDebug` 成功
- [ ] `sh gradlew :app:testDebugUnitTest` 全綠

**Dependencies：** Task 13（Compose 版本已全面取代舊畫面）

**Files likely touched：**
- 刪除上述舊檔案

**Estimated scope：** S

---

## Task 17：detekt/ktlint 規則調整 + Full Gate

**Description：** 針對 Task 0 建立的 detekt/ktlint baseline，處理過程中新增
程式碼的違規（不得對新程式碼使用 `@Suppress` 消音，只能修正程式碼本身）。

**Acceptance criteria：**
- [ ] `sh gradlew detekt ktlintCheck :app:testDebugUnitTest` 全數通過
- [ ] 無任何新增的 `@Suppress`

**Verification：**
- [ ] 同上指令即為驗證

**Dependencies：** Task 1–16 全部完成

**Files likely touched：** 視違規項目而定

**Estimated scope：** S–M（視 detekt 預設規則集嚴格程度而定）

---

## Task 18：Success Criteria 全項驗證 + 上架準備

**Description：** 手動逐項驗證 `doc/phase1-spec.md` 的 Success Criteria
清單，並準備 Google Play `QUERY_ALL_PACKAGES` 權限使用聲明表單草稿。

**Acceptance criteria：**
- [ ] `doc/phase1-spec.md` Success Criteria 全數打勾
- [ ] `QUERY_ALL_PACKAGES` 聲明表單草稿完成（文字檔即可，供上架時使用）

**Verification：**
- [ ] 逐項於實機（建議 Android 12+ 裝置）手動驗證並記錄結果

**Dependencies：** Task 17

**Files likely touched：** 無程式碼異動（驗證與文件）

**Estimated scope：** S

---

### Checkpoint：Task 15–18（對應 Plan M6+M7，Phase 1 MVP 完成）
- [ ] 全部 Success Criteria 打勾
- [ ] Full gate 通過
- [ ] 準備進入 `code-review-and-quality` 自我審查，再開 PR

---

## 任務相依總覽（簡表）

| Task | 依賴 | 可平行對象 |
|------|------|-----------|
| 0 | 無 | — |
| 1 | 無 | 2,3,4,5 |
| 2 | 0 | 1,3,4,5 |
| 3 | 0 | 1,2,4,5 |
| 4 | 無 | 1,2,3,5 |
| 5 | 0 | 1,2,3,4 |
| 6 | 5 | 7 |
| 7 | 0 | 6 |
| 8 | 2,3,4,5 | — |
| 9 | 8,6,7 | — |
| 10 | 0 | 11 |
| 11 | 0 | 10 |
| 12 | 8,9,10,11 | 15 |
| 13 | 12 | 15 |
| 14 | 12 | 15 |
| 15 | 1,6,7 | 10,11,12,13,14 |
| 16 | 13 | — |
| 17 | 1–16 | — |
| 18 | 17 | — |

---

## Risks and Mitigations

沿用 `doc/phase2-plan.md` 的風險清單，額外補充：

| Risk | Impact | Mitigation |
|------|--------|------------|
| Task 7 的 `TestNotificationSender` 是實作時才具體定形的新介面，可能與 Task 15 的自我黑名單判斷邏輯耦合不清 | Medium | Task 7 完成後，於 Task 15 開始前先確認 `SELF_TEST_NOTIFICATION_TAG` 常數與判斷邏輯介面一致，必要時在 Task 15 描述中補充 |
| Task 8/9 拆分 `MainViewModel` 為兩個任務，但兩者編輯同一檔案，若平行執行會衝突 | Low | 明確標示 Task 9 依賴 Task 8，**必須序列**，不得平行 |

---

## Open Questions

無。

---

## 下一步

進入 **Phase 5（Implement + Remediation Loop）**：依上述任務順序逐一實作，
每個任務完成後執行對應 Verification 指令，並依
`incremental-implementation` / `test-driven-development` /
`git-workflow-and-versioning` skill 指引進行小步提交。每次實作後跑
Remediation Loop（`detekt ktlintCheck :app:testDebugUnitTest`），失敗 3 次
未解決則觸發 Phase 6 Escalation。
