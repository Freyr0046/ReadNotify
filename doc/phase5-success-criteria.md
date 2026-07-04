# Phase 5 — Success Criteria Walkthrough (Task 18)

- **文件狀態**：已完成人工/實機驗證
- **建立日期**：2026-07-07
- **對應文件**：`doc/phase1-spec.md`（Success Criteria 清單來源）
- **測試裝置**：Pixel 7（實體機，USB 連線），Android 16（API 36）

---

## 逐項驗證結果

| # | Success Criteria | 結果 | 驗證方式 |
|---|---|---|---|
| 1 | 白名單清單以 PackageManager 動態列出裝置所有已安裝 App，皆可勾選/取消，勾選狀態即時寫入 DataStore 並持久化 | ✅ | 實機：勾選「Android Auto」後重啟 App，勾選狀態仍保留 |
| 2 | 白名單為空（初始狀態）時，所有傳入通知靜音跳過，不觸發 TTS | ✅ | 單元測試（`NotificationWhitelistRepositoryImplTest` 預設空集合）+ 程式碼審查（`NotificationService` 白名單 guard clause） |
| 3 | 白名單內 App 送出通知後，2 秒內於背景觸發 TTS 播報 | ⚠️ 部分驗證 | 完整管線（Service→Domain→Data→ViewModel→UI）已透過**自我測試通知**於實機驗證成功（見 #12）。**未**額外用真正第三方 App 驗證這條路徑，因為 (a) 無法透過 `adb shell cmd notification post` 偽造成任意第三方套件身分（該指令固定以 `com.android.shell` 身分發送），(b) 不希望為了測試去騷擾使用者手機上其他真實 App 的通知。白名單判斷邏輯（`packageName !in whitelist`）本身極簡單且已被 `NotificationWhitelistRepositoryImplTest` 覆蓋，殘餘風險低，但**建議在真正的 Line/FB 等 App 上手動補測一次**再上線 |
| 4 | 通知標題或內文任一為 null／空字串時，該通知靜音跳過 | ✅ | 單元測試（`BuildAnnouncementUseCaseImplTest`） |
| 5 | App 自身通知永不被攔截播報，僅 `SELF_TEST_NOTIFICATION_TAG` 例外 | ✅ | 實機：自我測試通知（帶標記）成功通過；程式碼審查確認一般自身通知會被 `packageName == applicationContext.packageName && !isSelfTestNotification` 擋下 |
| 6 | 播報內容 > 50 字截斷 + 語音尾碼 | ✅ | 單元測試（49/50/51 字邊界） |
| 7 | URL 正則取代為「網址」 | ✅ | 單元測試 |
| 8 | 播報佇列積壓達 5 條時捨棄第 6 條起 | ✅ | 單元測試（`SpeechQueueRepositoryImplTest`，模擬 7 條攻頂驗證僅 6 條被處理） |
| 9 | `OnScreenResumed` 偵測權限撤銷並切換 `PermissionDenied` | ✅ | 單元測試 + **實機**：關閉/開啟系統通知存取權限後回到 App，畫面正確切換 `PermissionDenied` ↔ `IdleConfig` |
| 10 | 無 TTS 引擎時 `EngineError(TTS_ENGINE_NOT_INSTALLED)` + 安裝 Intent | ⚠️ 未實機驗證 | 單元測試（`MainViewModelTest`）+ 程式碼審查（`ACTION_INSTALL_TTS_DATA` Intent 已接上）。測試機本身已預裝 Google TTS，**無法在不解除安裝系統元件的情況下**於這台機器重現此狀態，屬於合理的環境限制，非程式碼風險 |
| 11 | TTS 語系不支援時靜音跳過 | ✅ | 單元測試（`TtsEngineRepositoryImplTest` 映射邏輯） |
| 12 | 「發送測試通知」延遲 2 秒觸發並成功 TTS 朗讀 | ✅ | **實機完整端對端驗證**：按鈕 → 2 秒延遲 → 系統通知 (id=9999, tag=SELF_TEST) → `NotificationService` 接收 → `TtsPlaying` 畫面顯示「正在播報：來自 ReadNotify 的通知...」 |
| 13 | `MainUiState` 五態皆有 Compose UI，Hilt Singleton + StateFlow 即時反映 | ✅ | 實機驗證 `PermissionDenied`/`IdleConfig`/`TtsPlaying` 三態；`InitChecking`/`EngineError` 經單元測試 + Compose UI 測試（`MainScreenTest`）驗證 |
| 14 | `QUERY_ALL_PACKAGES` Play Console 權限聲明表單草稿 | ✅ | 見下方「Play Store 上架準備」 |
| 15 | Full gate 通過 | ✅ | `./gradlew detekt ktlintCheck :app:testDebugUnitTest` — BUILD SUCCESSFUL |
| 16 | 手動檢查無明顯記憶體洩漏 | ⚠️ 非工具驗證 | 未整合 LeakCanary（Spec 中為「若後續導入」的可選項，本次未導入）。實機測試過程中多次重啟 App、切換權限狀態、反覆點擊，未觀察到明顯延遲累積或 OOM，但這只是人工觀察，不是量化的洩漏偵測工具結果 |

**總結：16 項中 13 項完整驗證通過，3 項為有明確理由的部分驗證/環境限制**（#3 白名單第三方 App 建議上線前補測、#10 受測試機環境限制、#16 未導入專門工具）。均非程式碼邏輯缺陷，皆已在上表逐一寫明原因。

---

## Play Store 上架準備：`QUERY_ALL_PACKAGES` 權限使用聲明（草稿）

Google Play Console 的「應用程式內容」→「權限」章節，`QUERY_ALL_PACKAGES` 屬於
需要額外聲明使用理由的敏感權限，草擬回覆內容如下：

> **權限用途說明（草稿，實際送出前請 Tech Lead / PM 覆核用詞）：**
>
> ReadNotify 是一款語音播報 App，核心功能是讓使用者選擇想要被朗讀通知的
> App（例如通訊軟體）。為了讓使用者能從「裝置上所有已安裝的 App」中挑選，
> 而不是侷限在一份預先定義的清單，App 需要透過
> `PackageManager.getInstalledApplications()` 取得完整的已安裝應用程式
> 清單，供使用者在設定畫面中勾選白名單。
>
> 此清單僅用於畫面顯示與使用者勾選，**不會**上傳、分享或用於廣告/分析目的，
> 使用者勾選的白名單套件名稱僅保存在裝置本機的 DataStore 中。

**待辦（非本次程式碼變更範圍，需人工在 Play Console 後台操作）：**
- [ ] 於 Play Console「App content」→「Permissions」提交上述聲明
- [ ] 準備對應的隱私權政策頁面（若尚未有，需說明本機儲存白名單、不上傳資料）
- [ ] 確認 `POST_NOTIFICATIONS`（Android 13+）於商店頁面的目標 SDK 聲明中已涵蓋

---

## 事後補充：Edge-to-Edge 處理（使用者發現，2026-07-07）

本文件初版完成後，使用者檢視實機截圖時發現 `TtsPlayingBanner` 文字與狀態列
圖示重疊，指出「目前看起來沒做 edge to edge 的處理」。確認後為真：
`MainActivity` 未呼叫 `enableEdgeToEdge()`，且沒有任何 Composable 套用
系統列 insets padding。Android 15+（targetSdk 35+）強制 edge-to-edge、
無法退出，導致內容預設畫到狀態列/導覽列底下——這也是 Task 13/15 實機測試
過程中，多次點擊畫面下緣按鈕卻誤觸「返回主畫面」手勢的根本原因（按鈕的
可點擊區域一路延伸到手勢導覽列的保留區）。

修正：`MainActivity` 呼叫 `enableEdgeToEdge()` 並讓根 `Surface` 填滿視窗
（背景維持真正 edge-to-edge）；`MainScreenContent` 的外層 `Box` 加上
`Modifier.windowInsetsPadding(WindowInsets.safeDrawing)`，把實際內容
（文字、按鈕）從系統列往內推。已於 Pixel 7 實機重新驗證：狀態列不再遮擋
文字、按鈕與導覽列之間恢復安全間距、full gate 與 3 個 `MainScreenTest`
測試皆維持綠燈。

---

## 已知後續建議（非本次任務範圍，記錄供未來規劃參考）

- **白名單清單雜訊過多**：實機測試時發現 `PackageManager.getInstalledApplications()`
  回傳大量系統內部套件（如 `com.android.providers.media`、大量
  `*.auto_generated_rro_*` overlay 套件），且這類套件沒有對應的人類可讀
  `applicationLabel`（退化顯示為套件名稱本身）。PRD 字面上要求「列出裝置
  所有已安裝 App」，本次照字面實作，但實際使用體驗上清單會被大量使用者
  不會想選的系統元件淹沒。建議 Phase 2 評估是否改為只顯示「有 Launcher
  Icon（可被使用者主動開啟）」的 App，這是大多數同類 App 的慣例做法。
- **白名單第三方 App 上線前建議補測**：見上方 #3。

---

## 下一步

Phase 1 MVP 的 18 個實作任務（`doc/phase4-tasks.md`）與 Success Criteria
（`doc/phase1-spec.md`）皆已完成/驗證。建議：
1. 人工審閱本文件與 `feature/phase1-mvp` 分支的完整 commit 歷史
2. 依 `code-review-and-quality` skill 做一次自我審查（PR 前檢查）
3. 確認無誤後開 PR 合併回 `main`，並處理 Play Console 上架聲明
