# Phase 1 - Specify / Step 1: PRD Blind-Spot Detection

- **文件狀態**：已解決（RESOLVED — all 8 items answered 2026-07-04）
- **建立日期**：2026-07-04
- **回覆日期**：2026-07-04
- **對應 PRD**：`doc/SPEC.md`（Phase 1 MVP 詳細需求規格）
- **產出階段**：`android-spec-driven-development` Skill / Phase 1 (Specify) / Step 1

---

## 背景

在動手撰寫 Android Engineering Spec 之前，先比對 `doc/SPEC.md` 與現有程式碼庫
（`NotificationService.kt`、`NotifyForegroundService.kt`、`NotifyViewModel.kt`、
`Constants.kt`、`app/build.gradle`、`AndroidManifest.xml`），找出 PRD 未定義、
或 PRD 與現有實作互相衝突之處。

依 Skill 規範，發現盲點時必須「停止並列出清單」，待人工確認後才能進入
Step 2（確認技術假設）與 Step 3（撰寫 Spec）。

---

## 盲點清單

### 【架構落差】

**1. 白名單清單的資料來源未定義**
現有 `NotificationService.kt` 完全沒有 Clean Architecture 分層（無 Repository /
UseCase / ViewModel 邏輯），且直接在 Service 內寫死 `line` / `facebook` /
`instagram` 套件名稱判斷，沒有任何白名單 UI 可勾選。
PRD 3.1.2 要求「使用者手動勾選」——白名單清單的資料來源是？
- (a) 呼叫 `PackageManager` 列出裝置所有已安裝 App（Android 11+ 需要
  `QUERY_ALL_PACKAGES` 套件可見性權限）
- (b) 僅提供預先定義的常見通訊 App 清單（Line / WeChat / FB / IG / Telegram...）

**2. Firebase Firestore 上傳邏輯與 PRD「隱私保護」原則衝突**
現有程式碼在 `onNotificationPosted()` 中直接呼叫 Firebase Firestore，把每一則
通知的完整標題、內文、Bundle 內容上傳雲端——這與 PRD 1.1「隱私保護
(Privacy First)」核心原則直接衝突，且 PRD 全文未提及任何雲端上傳需求。
Phase 1 MVP 是否要保留此 Firestore 上傳邏輯？
若保留，是否需要先做隱私聲明；若移除，是否用 Phase 2 提到的「本地通知
歷史日誌」取代？

**3. Foreground Service 是否納入 Phase 1 範圍**
`NotifyForegroundService` 目前已存在暫停/繼續/關閉播報的常駐通知列按鈕，
但 PRD 完全沒提到 Foreground Service 或常駐通知列這塊功能。這個既有功能
是否算 Phase 1 範圍內、需要一併重構進新架構？還是視為既有基礎設施、
暫不處理？

### 【PRD 內部矛盾】

**4. 自我測試工具如何繞過自我無限循環防護**
3.1.1 自我無限循環防護要求「本 App 自身通知永不攔截、永不播報」，但
3.1.5 自我測試工具需要「發送一條模擬第三方通知」並驗證 TTS 真的有出聲。
這條測試通知要如何繞過自我黑名單又不造成邏輯衝突？
- (a) 測試流程直接呼叫 TTS 讀出文字，不透過 `NotificationListenerService`
  管線
- (b) 測試通知使用特殊 tag/extra 標記，讓 Service 判斷為「允許的測試訊號」

**5. 播報佇列上限需要 App 自管佇列**
異常矩陣「佇列中最多積壓 5 條」——Android `TextToSpeech` 引擎本身不提供
查詢目前佇列長度的 API。這代表播報佇列需要 App 自行維護（例如在
Repository/Manager 層用一個 `Queue<String>` 搭配 `UtteranceProgressListener`
回呼追蹤），而不是單純呼叫 `tts.speak(QUEUE_ADD)`。請確認採用此自管佇列
的方向。

### 【背景服務 ↔ UI 同步機制】

**6. 跨元件狀態同步機制未定義**
UI 狀態機要求 `TTS_PLAYING` 狀態要與背景 `NotificationListenerService`
即時同步，但兩者是不同元件、無 ViewModel 共享關係。需要一個跨元件狀態
傳遞機制。
建議：Application-scoped Singleton Repository 用 StateFlow/SharedFlow
廣播，Service 寫入、ViewModel 訂閱。是否同意這個方向？
（若要用 Hilt 做 `@Singleton` scope 需要先導入 Hilt——見技術假設清單。）

### 【資料儲存】

**7. 白名單本地儲存機制未指定**
白名單 `Set<String>` PRD 說「本地維護」，但沒指定儲存機制。現有專案沒有
Room 或 DataStore 依賴。建議採用 Jetpack DataStore (Preferences)，取代
明文 SharedPreferences。是否同意？

### 【範圍邊界】

**8. 使用者主動暫停播報是否屬於 Phase 1**
Phase 1 完全沒提及「取消播報／清空佇列」的使用者主動操作，但現有
`NotifyForegroundService` 已有暫停/繼續按鈕。Phase 1 UI 狀態機
（`IDLE_CONFIG` / `TTS_PLAYING`）是否也要包含使用者暫停播報的能力，還是
完全交由 Phase 2（搖一搖/實體鍵中斷）處理？

---

## 決議結果

| # | 主題 | 決議 |
|---|------|------|
| 1 | 白名單清單來源 | 採用 **PackageManager 列出裝置所有已安裝 App**（非預先定義清單）。需在 Manifest 宣告 `QUERY_ALL_PACKAGES`，Spec 中需列為 Android 11+ 套件可見性風險項目。 |
| 2 | Firestore 上傳邏輯 | **移除**。Phase 1 不做任何雲端記錄，改為純本地運作，符合 PRD 隱私保護原則。 |
| 3 | Foreground Service 重構範圍 | **暫不處理**。維持現有 `NotifyForegroundService` 現狀，不納入本次 Clean Architecture 重構。 |
| 4 | 自我測試工具繞過機制 | 採用**特殊 tag/extra 標記**方式：測試發出的通知帶有專屬標記，`NotificationService` 判斷為「允許的測試訊號」而略過自我黑名單阻擋，其餘情況仍嚴格阻擋自身通知。 |
| 5 | 播報佇列上限 | 採用 **App 自管佇列**：Manager/Repository 層自行維護 `Queue`，搭配 `UtteranceProgressListener` 追蹤播放進度與佇列長度，滿 5 條時捨棄新通知。 |
| 6 | 背景 Service ↔ 前景 UI 狀態同步 | 採用 **Hilt `@Singleton` Repository + StateFlow**：Service 寫入狀態、ViewModel 透過 `collectAsStateWithLifecycle` / `repeatOnLifecycle` 訂閱。需在專案中新增 Hilt 依賴與 `@HiltAndroidApp`。 |
| 7 | 白名單本地儲存機制 | 採用 **Jetpack DataStore (Preferences)**，取代 SharedPreferences。 |
| 8 | 使用者主動暫停/繼續播報 | **延到 Phase 2**。Phase 1 UI 狀態機（`IDLE_CONFIG` / `TTS_PLAYING`）不包含暫停操作，現有 Foreground Service 暫停按鈕維持現狀、不整合進新狀態機。 |

## 待辦

- [x] 人工逐項回覆或核准建議選項
- [ ] 進入 Phase 1 / Step 2：確認技術假設（Assumptions）
- [ ] Step 2 通過後進入 Phase 1 / Step 3：撰寫正式 Android Engineering Spec
      （`doc/phase1-spec.md`）
