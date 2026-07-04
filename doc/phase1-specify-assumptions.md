# Phase 1 - Specify / Step 2: Android-Specific Assumptions

- **文件狀態**：已確認（CONFIRMED — 2026-07-04，含 3 項修正見下方）
- **建立日期**：2026-07-04
- **確認日期**：2026-07-04
- **前置文件**：`doc/phase1-specify-blind-spots.md`（已解決，8 項決議完成）
- **產出階段**：`android-spec-driven-development` Skill / Phase 1 (Specify) / Step 2

---

## 技術假設清單

以下假設基於 Step 1 決議與現有專案實際狀態（而非 Skill 模板預設值）整理，
請逐項確認或修正，全部確認後才會進入 Step 3（撰寫正式 Spec）。

1. **Min SDK：26**（沿用 `app/build.gradle` 現值，非 Skill 模板預設的 21）

2. ~~UI 層：Fragment + ViewBinding~~ → **【修正】全面改用 Jetpack Compose**。
   人工決議：整個 App 改為 Single-Activity + Compose 架構。
   - 現有 `NotifyFragment` / `NotifyAdapter` / `NotifyViewHolder`
     （通知歷史列表，透過 RecyclerView + ViewBinding 實作）將改寫為
     Composable + `LazyColumn`，`row_notify.xml` / `fragment_notify.xml`
     等 layout XML 屆時可移除。
   - `SplashActivity` 之後直接導入 Compose 畫面（不再經由
     Fragment + Navigation Component 的 XML 導覽圖）。
   - 現有 `layout_notify.xml`（`NotifyForegroundService` 常駐通知列用的
     `RemoteViews`）**不受影響**——`RemoteViews` 不支援 Compose，本來就
     必須維持原生 View XML，且依 Step 1 決議 #3 該 Service 本次不重構。
   - 此為既有畫面的重構工作，會使 Phase 1 工作量增加，將反映在
     Phase 4 Tasks 拆分中。

3. **DI：新導入 Hilt**。現有專案完全沒有 DI 框架。因 Step 1 決議 #6
   （背景 Service ↔ 前景 UI 狀態同步採 Hilt Singleton + StateFlow），
   本次需要：
   - 新增 Hilt Gradle 依賴與 plugin
   - 新增 `@HiltAndroidApp` Application class（目前 Manifest 未指定
     `android:name`，需一併新增並註冊）
   - ViewModel 改為 `@HiltViewModel`，於 Composable 中透過
     `hiltViewModel()` 取得實例（不得將 ViewModel 往下傳遞給深層
     Composable，遵循 UDF 規範）

4. ~~Navigation：Jetpack Navigation Component（XML）+ Safe Args~~ →
   **【修正】改用 Navigation Compose (`androidx.navigation:navigation-compose`)**，
   因應全面 Compose 化決議。既有 `res/navigation/mobile_navigation.xml`
   將被 Compose 的 `NavHost` + route 定義取代並可移除。白名單設定 UI
   規劃為 PRD `IDLE_CONFIG` 狀態對應的 Composable 畫面
   （沿用既有「通知列表」畫面的路由位置，非開全新獨立流程）。

5. **Async：Kotlin Coroutines + StateFlow**，取代現有 `NotifyViewModel`
   中的 `LiveData`。

6. **本地儲存：Jetpack DataStore (Preferences)**（Step 1 決議 #7），
   新增對應 Gradle 依賴。

7. **無遠端 API（No Retrofit / 無 REST 網路合約）**。本 App 為純本地裝置端
   應用（NotificationListenerService + TTS + DataStore），Skill 模板中
   「API Contracts」章節不適用傳統 REST 定義，Spec 中將改寫為「內部合約
   (Internal Contracts)」，定義：
   - `PackageManager` 已安裝 App 查詢結果的 Domain Model
   - DataStore 白名單 Schema
   - `NotificationListenerService → Repository` 的資料傳遞合約
   - TTS Manager（播報佇列）對外合約
   現有的 Firebase Firestore 依賴（`firebase-firestore-ktx`）將於 Phase 1
   移除相關呼叫程式碼（Step 1 決議 #2），但 Gradle 依賴本身是否整個移除，
   或保留給未來其他用途，請一併確認（預設：**保留依賴、移除呼叫程式碼**，
   避免影響專案中其他可能使用 Firebase 的部分——目前掃描未發現其他用途，
   如確認無用可整個移除）。

8. **Module：單一 `:app` module**
   （`./gradlew :app:testDebugUnitTest`）。

9. **錯誤處理合約：`Result<T>` 貫穿所有跨層邊界**
   （Repository → UseCase → ViewModel）。

10. **測試框架：採用 Skill 建議組合 JUnit5 + Mockk + Turbine**【已確認】。
    現有 `build.gradle` 僅有 `junit:junit:4.13.2`，需新增：
    - `org.junit.jupiter:junit-jupiter`（JUnit5，需搭配
      `useJUnitPlatform()` 設定，並移除/取代 JUnit4 runner）
    - `io.mockk:mockk`
    - `app.cash.turbine:turbine`（測試 StateFlow/Flow 發射序列）
    Compose UI 測試另外採 `androidx.compose.ui:ui-test-junit4`
    （僅用於關鍵 happy path，依 Skill 測試策略）。

11. **自我黑名單判斷依據：`BuildConfig.APPLICATION_ID`**
    （即 `com.freyr.readnotify`，注意這與程式碼 namespace
    `com.freyr.readmynotify` 不同——`sbn.packageName` 取得的是
    `applicationId`，需確保黑名單比對用對值，避免因誤用 namespace
    導致自我黑名單失效）。

12. **`QUERY_ALL_PACKAGES` 上架風險**【已確認接受】：Step 1 決議 #1 採用
    列出所有已安裝 App 的方案，需在 Manifest 宣告此權限。人工已確認接受
    Google Play 上架時的權限聲明審查流程成本，Spec 中將列為
    Success Criteria 之一（需完成 Play Console 權限使用聲明表單）。

13. **現有 `NotificationService.kt` 中寫死的 line/facebook/instagram
    判斷邏輯將被完全取代**，改為讀取使用者透過 DataStore 設定的白名單
    Package Name 集合進行比對，不再有任何寫死的 App 判斷分支。

## 確認狀態

全部 13 點已於 2026-07-04 確認完成，其中 #2、#4、#10、#12 依人工指示修正
如上。**Step 2 結束，進入 Step 3：撰寫正式 Android Engineering Spec**
（`doc/phase1-spec.md`）。
