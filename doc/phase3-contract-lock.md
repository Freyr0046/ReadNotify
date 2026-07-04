# Phase 3: Contract Lock — ReadNotify Phase 1 MVP

- **文件狀態**：CONTRACT LOCKED（合約本身已鎖定並通過編譯/測試驗證），
  **進入 Phase 4 前仍待人工審閱**（PENDING HUMAN REVIEW）
- **對應 Spec / Plan**：`doc/phase1-spec.md`、`doc/phase2-plan.md`
- **產出階段**：`android-spec-driven-development` Skill / Phase 3

---

## Overview

本階段把 Spec 轉換為程式碼形式的行為合約，並凍結：Repository / UseCase
介面、`MainUiState` / `MainViewIntent`、以及對應 `MainViewModelTest` 測試樁。
Phase 5 實作**只能填入這些介面與測試樁的內容，不能修改它們的形狀**。若實作
過程中發現合約有問題，必須依 Phase 6 走 Escalation 流程，而不是直接改測試
或介面。

本次鎖定範圍**不包含** `NotifyForegroundService`（Step 1 決議 #3：本次不
重構）。

---

## Step 1: Gherkin 情境（對應 `MainUiState` 全部轉換）

```gherkin
Feature: Main Screen 狀態機（對應 PRD 第 4 節 UI 狀態機）

  Scenario: App 啟動時進入初始化檢查
    Given 使用者開啟 App
    When ViewModel 初始化
    Then UiState 為 InitChecking

  Scenario: 初始化檢查後偵測到未授權通知存取權限
    Given ViewModel 完成初始化檢查
    When CheckNotificationAccessUseCase 回傳 false
    Then UiState 轉為 PermissionDenied

  Scenario: 初始化檢查後偵測到權限已授權且 TTS 就緒
    Given ViewModel 完成初始化檢查
    When CheckNotificationAccessUseCase 回傳 true 且 InitializeTtsEngineUseCase 成功
    Then UiState 轉為 IdleConfig，並帶入目前已安裝 App 清單與白名單勾選狀態

  Scenario: TTS 引擎初始化失敗（未安裝引擎）
    Given ViewModel 執行 InitializeTtsEngineUseCase
    When 回傳 Result.failure 且原因為 TTS_ENGINE_NOT_INSTALLED
    Then UiState 轉為 EngineError(TTS_ENGINE_NOT_INSTALLED)

  Scenario: 使用者回到前景時重新檢查權限（異常矩陣：中途關閉權限）
    Given UiState 目前為 IdleConfig
    When 使用者送出 OnScreenResumed 意圖，且 CheckNotificationAccessUseCase 回傳 false
    Then UiState 立即轉為 PermissionDenied

  Scenario: 使用者勾選白名單 App
    Given UiState 為 IdleConfig
    When 使用者送出 OnAppWhitelistToggled(packageName, true)
    Then SetAppWhitelistedUseCase 被呼叫且傳入 (packageName, true)
    And UiState 中對應 App 的 isChecked 更新為 true

  Scenario: 背景開始播報時前景同步顯示 TtsPlaying
    Given UiState 為 IdleConfig
    When ObservePlaybackStateUseCase 發出 PlaybackState.Speaking("Line")
    Then UiState 轉為 TtsPlaying(speakingFromLabel 包含 "Line")

  Scenario: 播報結束後回到 IdleConfig
    Given UiState 為 TtsPlaying
    When ObservePlaybackStateUseCase 發出 PlaybackState.Idle
    Then UiState 轉為 IdleConfig

  Scenario: 使用者按下發送測試通知
    Given UiState 為 IdleConfig
    When 使用者送出 OnSendTestNotificationClicked
    Then SendTestNotificationUseCase 被呼叫恰好一次

  Scenario: 使用者於 EngineError 狀態按下重試並成功恢復
    Given UiState 為 EngineError(LANGUAGE_UNSUPPORTED)
    When 使用者送出 OnRetryEngineInitClicked 且 InitializeTtsEngineUseCase 這次回傳成功
    Then UiState 轉為 IdleConfig
```

> 說明：本專案未導入 Cucumber/BDD 執行框架（Step 2 假設清單未包含此項，
> 屬新增依賴需另外確認），故以上 Gherkin 情境為**設計文件**，不是可執行的
> `.feature` 測試檔。其可執行對應版本為下方鎖定的 `MainViewModelTest`。

---

## Step 2: 鎖定的 Repository / UseCase 介面

以下介面已寫入原始碼並標註 `// LOCKED — do not modify during Phase 5`：

**Domain Models**（`app/src/main/java/com/freyr/readmynotify/domain/model/`）
- `InstalledApp.kt`、`Announcement.kt`、`PlaybackState.kt`、
  `EngineErrorReason.kt`、`TtsEngineState.kt`、`IncomingNotification.kt`

**Repository 介面**（`app/src/main/java/com/freyr/readmynotify/domain/repository/`）
- `InstalledAppRepository.kt`
- `NotificationWhitelistRepository.kt`
- `SpeechQueueRepository.kt`
- `TtsEngineRepository.kt`
- `NotificationAccessRepository.kt`

**UseCase 介面**（`app/src/main/java/com/freyr/readmynotify/domain/usecase/`）
- `GetInstalledAppsUseCase.kt`
- `ObserveWhitelistUseCase.kt`
- `SetAppWhitelistedUseCase.kt`
- `CheckNotificationAccessUseCase.kt`
- `BuildAnnouncementUseCase.kt`
- `EnqueueAnnouncementUseCase.kt`
- `SendTestNotificationUseCase.kt`
- `InitializeTtsEngineUseCase.kt` **（Contract Lock 階段新增，已回填至
  `doc/phase1-spec.md` 的 Layer Map）**
- `ObserveEngineStateUseCase.kt` **（同上，新增）**
- `ObservePlaybackStateUseCase.kt` **（同上，新增）**

**UI 契約**（`app/src/main/java/com/freyr/readmynotify/ui/main/`）
- `MainUiState.kt`（含 `AppWhitelistItem`）
- `MainViewIntent.kt`
- `MainViewModel.kt`：**建構子參數清單（8 個 UseCase 依賴）為鎖定內容**，
  方法內部為 `TODO()`，Phase 5 只能填入實作、不能改變建構子形狀

新增為何未在原 Spec 出現的三個 UseCase：`MainViewModel` 在 `InitChecking`
與 `OnRetryEngineInitClicked` 情境都需要觸發/觀察 TTS 引擎狀態，且需要觀察
播放狀態才能切換 `TtsPlaying`；若讓 ViewModel 直接依賴
`TtsEngineRepository` / `SpeechQueueRepository`，會違反 CLAUDE.md 規定的
`UI → ViewModel → UseCase → Repository` 嚴格分層。這是撰寫鎖定介面時常見的
「發現遺漏」情況，已同步回寫 Spec，不視為需要重新走 Phase 1 的變更。

---

## Step 3: 鎖定的 ViewModel 測試樁

檔案：`app/src/test/java/com/freyr/readmynotify/ui/main/MainViewModelTest.kt`

10 個測試方法與上方 10 個 Gherkin 情境一一對應，全數使用 `TODO()` 作為
方法本體（依規範「必須失敗」——`TODO()` 拋出 `NotImplementedError`，執行
時會確實失敗，而非空方法體的假通過）。測試已可正確編譯並執行失敗，已於
下方 Step 4 驗證。

Mock 對象：`MainViewModel` 建構子依賴的全部 8 個 UseCase 皆以 Mockk
`mockk()` 建立，尚未 stub 任何回傳值（將於 Phase 5 各測試內個別 stub）。

---

## Step 4: 建置驗證

為了讓上述鎖定檔案可以編譯與執行（而不只是文字文件），本階段一併對
`app/build.gradle` 做了最小必要的工具鏈調整（已列入 Step 2 假設 #5/#10
已核准範圍內，非新決策）：
- 新增 `kotlinx-coroutines-core` / `kotlinx-coroutines-android`
  （Domain 介面使用 Flow/StateFlow 所需）
- 移除 `testImplementation 'junit:junit:4.13.2'`，改為
  `org.junit.jupiter:junit-jupiter` + `junit-jupiter-engine`
- 新增 `io.mockk:mockk`、`app.cash.turbine:turbine`、
  `kotlinx-coroutines-test`
- `android.testOptions.unitTests.all { useJUnitPlatform() }`

驗證結果（已於 2026-07-04 實際執行）：

- `sh gradlew :app:compileDebugKotlin` → **BUILD SUCCESSFUL**，僅既有檔案
  的既存警告（`NotificationService.kt` 未使用變數、deprecated override）
  以及 `MainViewModel.onIntent()` 參數未使用的預期警告
- `sh gradlew :app:testDebugUnitTest --tests "...MainViewModelTest"` →
  **10 個測試、10 個失敗**，全部失敗原因皆為
  `kotlin.NotImplementedError`（對應 `TODO()`），無任何編譯錯誤或非預期
  例外——符合「鎖定測試樁必須失敗」的要求
- 順帶修正：專案原有的 Android 樣板測試 `ExampleUnitTest.kt` 使用 JUnit4
  `org.junit.Test`／`org.junit.Assert`，因 JUnit4 依賴已移除而編譯失敗，
  已改為 JUnit5 `org.junit.jupiter.api.Test`／`Assertions`（非本次合約
  範圍，僅為讓工具鏈可用所做的必要修正）

⚠ 已知環境警告（不影響本階段結果，留待 Phase 4/5 處理）：AGP 8.5.0 尚未
正式支援 `compileSdk 36`（僅測試至 34），`gradlew` 檔案權限遺失可執行位元
（本次以 `sh gradlew ...` 繞過執行）。

---

## Lock Declaration

```
CONTRACT LOCKED.
以下項目在本 Feature 剩餘開發期間視為不可變：
- domain/repository/ 下全部 5 個 Repository 介面
- domain/usecase/ 下全部 10 個 UseCase 介面
- ui/main/MainUiState.kt、MainViewIntent.kt
- ui/main/MainViewModel.kt 的建構子參數形狀
- ui/main/MainViewModelTest.kt 的 10 個測試樁（方法簽名與情境對應關係）

實作（Phase 5）與上述合約之間的任何衝突，必須依 Phase 6 走 Escalation
流程，不可透過修改合約本身解決。
```

---

## 下一步

進入 **Phase 4（Tasks）**：依 `doc/phase2-plan.md` 的里程碑，把每個里程碑
拆解為可獨立驗證的細部任務（Task 1..N，含驗收標準與檔案清單），產出
`doc/phase4-tasks.md`。
