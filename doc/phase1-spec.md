# Android Spec: ReadNotify — Phase 1 MVP 核心骨幹

- **文件狀態**：待人工審閱（PENDING HUMAN REVIEW）
- **建立日期**：2026-07-04
- **對應 PRD**：`doc/SPEC.md`
- **前置文件**：
  - `doc/phase1-specify-blind-spots.md`（Step 1，已解決）
  - `doc/phase1-specify-assumptions.md`（Step 2，已確認）
- **產出階段**：`android-spec-driven-development` Skill / Phase 1 (Specify) / Step 3

---

## Objective

打造一款背景常駐的「語音播報 App」。目標使用者是在開車、運動、做家事等雙手/
視線不便的情境下，需要即時得知手機通知內容、但無法低頭滑手機查看的使用者。

Phase 1 MVP 的成功定義：使用者可以手動勾選想聽的 App（如 Line、FB、IG 或任何
已安裝的 App），系統會在收到這些 App 的通知時自動朗讀標題與內容；同時 App
必須對「通知權限被關閉」「TTS 引擎異常」「內容為空」「自我通知迴圈」等極端
狀況做到 100% 防禦，不崩潰、不噪音轟炸、不洩漏隱私（不再上傳任何通知內容到
雲端）。

---

## Assumptions（摘要，完整內容見 `phase1-specify-assumptions.md`）

- Min SDK 26、單一 `:app` module、Kotlin Coroutines + StateFlow
- **UI：全面改用 Jetpack Compose + Navigation Compose，Single-Activity**
  （`MainActivity` 為唯一 Activity，`SplashActivity` 移除，改用
  `androidx.core:core-splashscreen` 掛在 `MainActivity` 上）
- **DI：新導入 Hilt**（`@HiltAndroidApp` + `@HiltViewModel` + `hiltViewModel()`）
- **本地儲存：Jetpack DataStore (Preferences)**
- **白名單清單：`PackageManager` 動態列出所有已安裝 App**（需
  `QUERY_ALL_PACKAGES`，已確認接受 Google Play 上架審查成本）
- **無遠端 API**：本 Spec 的「API Contracts」章節改為「Internal Contracts」
- **測試框架：JUnit5 + Mockk + Turbine**（取代現有 JUnit4）
- 現有 Firebase Firestore 上傳通知內容之呼叫程式碼**移除**（Gradle 依賴暫保留）
- 現有 `NotifyForegroundService`（常駐通知列暫停/繼續）**本次不重構**，維持原狀
- 自我黑名單比對依據 `BuildConfig.APPLICATION_ID`（`com.freyr.readnotify`）
- 使用者主動暫停/繼續播報功能**延到 Phase 2**

---

## Screen Inventory

Phase 1 UI 狀態機（PRD 第 4 節）描述的是「同一個畫面」隨背景服務狀態變化而
呈現不同內容，並非五個可互相導覽的獨立頁面（使用者不會在這五者之間「返回/
前進」）。因此建模為**單一畫面 `MainScreen`**，由一個 `MainUiState` sealed
interface 涵蓋全部五種狀態。

```kotlin
sealed interface MainUiState {

    /** App 啟動時，檢查通知存取權限與 TTS 引擎可用性 */
    data object InitChecking : MainUiState

    /** 無通知監聽權限：全螢幕阻擋，僅顯示前往設定按鈕 */
    data object PermissionDenied : MainUiState

    /** 權限正常且目前無語音播放中：顯示白名單清單、字數限制、測試按鈕 */
    data class IdleConfig(
        val installedApps: List<AppWhitelistItem>,
        val maxCharLimit: Int = 50,
    ) : MainUiState

    /** 背景正在播報 */
    data class TtsPlaying(
        val speakingFromLabel: String,   // 例："正在播報：來自 Line 的通知..."
        val installedApps: List<AppWhitelistItem>,
    ) : MainUiState

    /** TTS 初始化失敗或遺失語音元件 */
    data class EngineError(val reason: EngineErrorReason) : MainUiState
}

/** 精簡、穩定、可比較的資料模型，避免不必要的 recomposition。
 *  App 圖示不放進 UiState（Drawable 不可比較/不穩定），由 UI 層依 packageName
 *  自行以 remember{} 快取載入。 */
data class AppWhitelistItem(
    val packageName: String,
    val appLabel: String,
    val isChecked: Boolean,
)

enum class EngineErrorReason {
    TTS_ENGINE_NOT_INSTALLED,   // 系統未安裝任何 TTS 引擎
    LANGUAGE_UNSUPPORTED,       // OnInitListener 回傳語系不支援/缺資料
    INIT_FAILED,                // 其他初始化失敗
}

sealed interface MainViewIntent {
    /** 前往系統通知存取權限設定頁 */
    data object OnPermissionSettingsClicked : MainViewIntent

    /** 畫面回到前景時觸發（DisposableEffect + Lifecycle.Event.ON_RESUME），
     *  對應異常矩陣「使用者中途關閉通知權限」防禦規則 */
    data object OnScreenResumed : MainViewIntent

    data class OnAppWhitelistToggled(val packageName: String, val checked: Boolean) : MainViewIntent

    /** 按下「發送測試通知」，2 秒後觸發模擬通知 */
    data object OnSendTestNotificationClicked : MainViewIntent

    /** ENGINE_ERROR 狀態下，若原因為 TTS_ENGINE_NOT_INSTALLED，導向 Play 商店下載頁 */
    data object OnInstallTtsEngineClicked : MainViewIntent

    /** 使用者切換至系統設定修復 TTS 語系後，手動重試初始化 */
    data object OnRetryEngineInitClicked : MainViewIntent
}
```

**設計備註（避免審查時被標記為缺漏 Empty 狀態）**：`IdleConfig.installedApps`
理論上可能為空清單（極端情況：裝置沒有任何第三方 App）。此情況不建模為獨立
的頂層 UiState，而是在 `IdleConfig` 畫面內以清單區塊顯示 empty-hint 文案
「尚未偵測到可勾選的應用程式」，因為這是設定頁面清單的區塊層級狀態，而非
整頁 Loading/Success/Error 的轉換。此決策已於 Spec 中明確記錄。

---

## Internal Contracts（取代 API Contracts）

本 App 無任何遠端 API 呼叫，故不定義 REST 合約，改為定義下列跨層資料合約
（皆以 `Result<T>` 作為錯誤傳遞邊界）：

### 1. 通知擷取合約（NotificationListenerService → Domain）

```kotlin
data class IncomingNotification(
    val packageName: String,
    val title: String?,
    val content: String?,
    val groupName: String?,      // Notification.EXTRA_SUB_TEXT（Line 群組名稱）
    val category: String?,       // Notification.category
    val tag: String?,            // StatusBarNotification.tag
)
```
⚠ `title` / `content` 皆為 nullable —— 依 PRD 3.1.1，任一為 null 或空字串
即整條「靜音跳過」，不進入後續 TTS 流程。
⚠ `tag == Constants.SELF_TEST_NOTIFICATION_TAG` 是自我測試通知的唯一合法
例外標記，其餘 `packageName == BuildConfig.APPLICATION_ID` 一律阻擋。

### 2. 白名單清單合約

```kotlin
interface InstalledAppRepository {
    suspend fun getInstalledApps(): Result<List<InstalledApp>>
}
data class InstalledApp(val packageName: String, val label: String)

interface NotificationWhitelistRepository {
    fun observeWhitelist(): Flow<Set<String>>
    suspend fun setAppEnabled(packageName: String, enabled: Boolean): Result<Unit>
}
```
DataStore Schema（Preferences）：`stringSetPreferencesKey("notification_whitelist")`
——儲存已勾選的 package name 集合。預設值為空集合（對應 PRD「預設全域關閉」）。

### 3. 播報佇列合約（App 自管，因 `TextToSpeech` 無佇列查詢 API）

```kotlin
interface SpeechQueueRepository {
    val playbackState: StateFlow<PlaybackState>
    suspend fun enqueue(announcement: Announcement): Result<Unit>
}
sealed interface PlaybackState {
    data object Idle : PlaybackState
    data class Speaking(val appLabel: String) : PlaybackState
}
data class Announcement(val appLabel: String, val message: String)
```
⚠ 佇列上限 5 條：`enqueue()` 於內部佇列已有 5 筆待播時，回傳
`Result.success(Unit)` 但**不加入佇列**（依 PRD 定義為靜默捨棄，非錯誤，不
可視為失敗阻塞呼叫端）。

### 4. TTS 引擎合約

```kotlin
interface TtsEngineRepository {
    val engineState: StateFlow<TtsEngineState>
    suspend fun initialize(): Result<Unit>
    suspend fun speak(text: String, utteranceId: String): Result<Unit>
}
sealed interface TtsEngineState {
    data object NotReady : TtsEngineState
    data object Ready : TtsEngineState
    data class Error(val reason: EngineErrorReason) : TtsEngineState
}
```
⚠ 語系不支援（`LANG_MISSING_DATA` / `LANG_NOT_SUPPORTED`）：`speak()` 回傳
`Result.failure`，呼叫端（`BuildAnnouncementUseCase`/Service 層）必須靜音
跳過，絕不可讓錯誤碼externalize 成 crash 或亂碼朗讀。

### 5. 通知存取權限檢查合約

```kotlin
interface NotificationAccessRepository {
    fun isNotificationAccessGranted(): Boolean
}
```
實作沿用現有 `NotifyFragment.checkNotifyPermission()` 邏輯（比對
`Settings.Secure.getString(..., "enabled_notification_listeners")`），移至
Data 層並補上單元測試。

### 6. 內容處理規則（Domain 純函式，`BuildAnnouncementUseCase`）

- 模板：`"{AppName}傳來新訊息，內容是：{Content}"`
- 截斷：`content.length > 50` 時取前 50 字 + 固定尾碼 `「...等省略內容」`
- URL 取代：正則 `Regex("https?://\\S+")`，取代為 `「網址」`（在截斷之前執行，
  避免網址被從中間截斷產生亂碼片段）

---

## Clean Architecture Layer Map

```
UI Layer（Jetpack Compose, Single-Activity）
  └─ MainActivity                         ← Compose host + AndroidX Core SplashScreen
  └─ MainScreen (Composable)
  └─ MainViewModel (@HiltViewModel)        ← 輸出 StateFlow<MainUiState>，接收 MainViewIntent
  └─ components/
       AppWhitelistRow, PermissionDeniedOverlay,
       EngineErrorDialog, TtsPlayingBanner

Domain Layer（無 Context）
  └─ GetInstalledAppsUseCase
  └─ ObserveWhitelistUseCase
  └─ SetAppWhitelistedUseCase
  └─ SendTestNotificationUseCase           ← 觸發特殊標記測試通知（不經黑名單）
  └─ BuildAnnouncementUseCase              ← 模板/截斷/URL 取代
  └─ CheckNotificationAccessUseCase
  └─ EnqueueAnnouncementUseCase            ← 呼叫 SpeechQueueRepository
  └─ InitializeTtsEngineUseCase            ← 【Phase 3 Contract Lock 新增】
  └─ ObserveEngineStateUseCase             ← 【Phase 3 Contract Lock 新增】
  └─ ObservePlaybackStateUseCase           ← 【Phase 3 Contract Lock 新增】
       （三者為鎖定介面階段補上：MainViewModel 需要初始化/觀察 TTS 引擎與
        播放狀態，若直接依賴 Repository 會違反 UI→ViewModel→UseCase→
        Repository 分層規則，詳見 doc/phase3-contract-lock.md）
  └─ Repository interfaces：
       NotificationWhitelistRepository, InstalledAppRepository,
       SpeechQueueRepository, TtsEngineRepository, NotificationAccessRepository

Data Layer
  └─ NotificationWhitelistRepositoryImpl   ← DataStore<Preferences>
  └─ InstalledAppRepositoryImpl            ← PackageManager
  └─ SpeechQueueRepositoryImpl (@Singleton) ← 記憶體內 Queue + UtteranceProgressListener
  └─ TtsEngineRepositoryImpl (@Singleton)  ← 封裝 android.speech.tts.TextToSpeech
  └─ NotificationAccessRepositoryImpl      ← Settings.Secure 查詢
  └─ WhitelistPreferencesDataSource        ← DataStore 實體

System Layer（Android Framework 進入點，僅做轉接，不含業務邏輯）
  └─ NotificationService (NotificationListenerService)
       onNotificationPosted() 只做：
       1. 空值檢查 → 靜音跳過
       2. 自我黑名單檢查（BuildConfig.APPLICATION_ID，
          tag == SELF_TEST_NOTIFICATION_TAG 為唯一例外）→ 阻擋或放行
       3. 交給 Domain layer（透過 Hilt `@AndroidEntryPoint` 注入 UseCase）
          執行白名單過濾 → BuildAnnouncement → Enqueue
       移除：現有寫死的 line/facebook/instagram 判斷、Firebase Firestore
       上傳呼叫
  └─ NotifyForegroundService                ← 不變動（Step 1 決議 #3）
```

規則：Domain 層不得 import Context／Android Framework 類別（`TextToSpeech`、
`PackageManager` 等一律封裝在 Data 層 Impl，透過建構子注入介面）。Data 層
Impl 透過 `@ApplicationContext` 取得 Context，禁止持有 Activity/Fragment 參考。

---

## Build & Test Commands

```
Build debug APK:      ./gradlew assembleDebug
Run unit tests:       ./gradlew :app:testDebugUnitTest
Static analysis:      ./gradlew detekt ktlintCheck
Full gate:            ./gradlew detekt ktlintCheck :app:testDebugUnitTest
```

⚠ 現況專案未設定 detekt / ktlint plugin，將於 Phase 4 Task 0（建置環境設定）
一併加入預設規則集，做為 Remediation Loop 的把關工具。

---

## Testing Strategy

| Layer | Framework | What to test |
|-------|-----------|--------------|
| ViewModel | JUnit5 + Mockk + Turbine | `MainUiState` 五態轉換、`OnScreenResumed` 觸發權限重檢、白名單勾選即時反映、`EngineError` 各 reason 對應 |
| UseCase | JUnit5 + Mockk | `BuildAnnouncementUseCase`（模板/截斷/URL 取代邊界值：49/50/51 字）、`SendTestNotificationUseCase` 特殊標記產生、`EnqueueAnnouncementUseCase` 滿 5 條捨棄邏輯 |
| Repository | JUnit5 + Mockk | DataStore 讀寫與預設空集合、PackageManager 查詢例外處理、TTS `OnInitListener` 錯誤碼 → `EngineErrorReason` 映射 |
| System (NotificationService) | JUnit5 + Mockk | 自我黑名單阻擋、特殊標記例外放行、空值靜音跳過（以 Robolectric 或直接抽出純函式測試，避免依賴真實 Service 生命週期） |
| UI | Compose UI Test（`ui-test-junit4`） | 僅涵蓋關鍵 happy path：`PermissionDenied` 阻擋操作、白名單勾選即時更新清單、按下測試通知後畫面切換為 `TtsPlaying` |

Coverage 期望：ViewModel、UseCase 層 ≥ 80%。UI 測試僅覆蓋 happy path。

---

## Boundaries

**Always**
- Null-safe Kotlin，`!!` 需附註解說明「數學上不可能為 null」的理由
- Compose 中 ViewModel 一律透過 `hiltViewModel()` 取得，不向下層 Composable
  傳遞 ViewModel 實例；狀態下放 (State Hoisting)、事件上拋
- Flow 收集皆用 `collectAsStateWithLifecycle()`
- ViewModel 內協程僅在 `viewModelScope` 啟動
- 所有跨層錯誤以 `Result<T>` 包裝，禁止裸 `throw` 或空 `catch`
- 通知標題/內文等敏感內容不得寫入 Log 或任何持久化儲存（DataStore 只存
  package name，不存通知內容）

**Ask first**
- 變更 Navigation Compose 的 route 結構（本 Spec 已定義初版，後續增減需再確認）
- 新增其他 Gradle 依賴（本 Spec 已列出全部新依賴：Hilt、Compose、Navigation
  Compose、DataStore、core-splashscreen、JUnit5/Mockk/Turbine、detekt/ktlint）
- 變更 Hilt `@Singleton` scope 範圍
- 移除 Firebase Firestore Gradle 依賴本身（目前僅移除呼叫程式碼，依賴保留）

**Never**
- `Context` / `Activity` 出現在 ViewModel、UseCase 或 Repository 介面中
- 使用 `GlobalScope`
- 為了消除 detekt/ktlint 警告加上 `@Suppress`
- 修改或刪除 Phase 3 鎖定的測試樁（locked test stubs）
- 在 Repository/UseCase 中 `throw Exception(...)`（一律 `Result.failure`）
- 上傳任何通知標題/內文到 Firebase 或任何雲端服務

---

## Success Criteria

驗證結果詳見 `doc/phase5-success-criteria.md`（Task 18，2026-07-07 於
Pixel 7 / Android 16 實機驗證）。16 項中 13 項完整驗證通過，3 項
（標註 ⚠️）為有明確理由的部分驗證，非程式碼缺陷。

```
- [x] 白名單清單以 PackageManager 動態列出裝置所有已安裝 App，皆可勾選/取消，
      勾選狀態即時寫入 DataStore 並持久化
- [x] 白名單為空（初始狀態）時，所有傳入通知靜音跳過，不觸發 TTS
- [ ] ⚠️ 白名單內 App 送出通知後，2 秒內於背景觸發 TTS 播報（實機測試，
      Pixel 5 或同等裝置，Android 12+）——管線已透過自我測試通知驗證，
      未另外用真正第三方 App 驗證，建議上線前補測
- [x] 通知標題或內文任一為 null／空字串時，該通知靜音跳過，不進入播報佇列
- [x] App 自身通知（BuildConfig.APPLICATION_ID）永不被攔截播報，僅
      SELF_TEST_NOTIFICATION_TAG 標記的測試通知為例外
- [x] 播報內容 > 50 字時，截斷為前 50 字並附加語音尾碼「...等省略內容」
- [x] 內文中的 URL 以正則 `https?://\S+` 取代為「網址」朗讀，不逐字母唸出網址
- [x] 播報佇列積壓達 5 條時，第 6 條起新通知直接捨棄，不崩潰、不阻塞既有播報
- [x] 使用者於系統設定關閉通知存取權限後，App 回到前景（`onResume`／
      `OnScreenResumed`）立即偵測並切換為 `PermissionDenied` 全螢幕阻擋，
      無法進入 `IdleConfig` 主介面
- [ ] ⚠️ 裝置未安裝任何 TTS 引擎時，`EngineError(TTS_ENGINE_NOT_INSTALLED)`
      彈出提示並提供前往 Google TTS 下載頁的 Intent——測試機已預裝 TTS
      引擎，無法重現此狀態，僅單元測試+程式碼審查驗證
- [x] TTS 引擎不支援目前設定語系時，靜音跳過，不朗讀亂碼或破音
- [x] 按下「發送測試通知」按鈕後延遲 2 秒觸發模擬通知，並成功透過 TTS 朗讀，
      藉此驗證權限與 TTS 引擎皆正常
- [x] `MainUiState` 五態（`InitChecking`/`PermissionDenied`/`IdleConfig`/
      `TtsPlaying`/`EngineError`）皆有對應 Compose UI，且背景 Service 狀態
      變化能透過 Hilt Singleton + StateFlow 即時反映到前景畫面
- [x] Google Play 上架用的 `QUERY_ALL_PACKAGES` 權限使用聲明表單草稿已準備
- [x] Full gate 通過：`./gradlew detekt ktlintCheck :app:testDebugUnitTest`
- [ ] ⚠️ 手動檢查（或 LeakCanary，若後續導入）未發現 Service／ViewModel 相關
      記憶體洩漏——未整合專門工具，僅人工觀察未發現異常
```

---

## Open Questions

無（Zero remaining）。Step 1、Step 2 提出的全部盲點與假設已於前置文件中
確認完畢，並已反映於本 Spec 各章節。

---

## 下一步

請 Tech Lead / 架構師審閱本 Spec。確認無誤後，將進入：
- **Phase 2（Plan）**：依 Clean Architecture Layer Map 建立依賴圖與任務順序
- 產出文件：`doc/phase2-plan.md`
