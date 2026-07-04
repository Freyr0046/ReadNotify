# 分層審查策略（Step 4）

> 進入本步驟前，必須已成功以 skill 載入 `android-reviewer`。若尚未載入，先返回執行 Step 0-1.5。

## 目錄
- [4-1 以 PR 大小決定起手式](#4-1)
- [4-2 中大型 PR：檔案盤點](#4-2)
- [4-3 小 PR：完整審查](#4-3)
- [4-4 中大型 PR：分層掃描＋升級](#4-4)
- [4-5 升級條件](#4-5)
- [4-6 問題分級與 Verdict](#4-6)
- [4-7 審查邊界](#4-7)
- [4-7.5 寫入 review_inventory.json](#4-75)
- [4-8 寫入 manifest.json](#4-8)

---

## 4-1. 以 PR 大小決定起手式 {#4-1}

從 `pr_meta.json` 讀取 `changed_files`、`additions`、`deletions`：

| 條件 | 路徑 |
|------|------|
| `changed_files <= 8` 且 `additions + deletions <= 300` | 跳過 4-2，直接進 4-3 完整審查 |
| 超過上述任一條件 | 執行 4-2 盤點，再依風險升級到 4-4 |

## 4-2. 中大型 PR 專用：檔案盤點 {#4-2}

根據 `changed_files.txt` 將每個檔案標記成 `high_risk` / `low_risk` / `needs_escalation`。

高風險包含：
- `*ViewModel.kt` / `*Fragment.kt` / `*Activity.kt`
- `*Repository.kt` / `*UseCase.kt`
- DI / navigation / state / coroutine / Flow 相關 Kotlin 檔
- `AndroidManifest.xml` / `build.gradle*` / `settings.gradle*` / `gradle.properties` / `proguard*`

## 4-3. 小 PR：直接完整審查 {#4-3}

```bash
CACHE_DIR="$(cat /tmp/android_pr_review_state_<PR_NUMBER>/cache_dir.txt)"
HEAD_SHA="$(cat "$CACHE_DIR/head_sha.txt")"
BASE_REF="$(cat "$CACHE_DIR/base_ref.txt")"
git diff --unified=80 "origin/$BASE_REF...$HEAD_SHA" > "$CACHE_DIR/full_diff.patch"
```

依已載入的 `android-reviewer` 執行完整 review，直接沿用該 skill 定義的：
- Multi-Pass review strategy
- BLOCKER / IMPORTANT / OPTIONAL 分級
- review output format 與 checklist

## 4-4. 中大型 PR：分層掃描＋升級 {#4-4}

1. 對所有 `high_risk` 檔案讀**完整檔案內容**
2. 為每個高風險檔建立檔案級 patch：

```bash
CACHE_DIR="$(cat /tmp/android_pr_review_state_<PR_NUMBER>/cache_dir.txt)"
HEAD_SHA="$(cat "$CACHE_DIR/head_sha.txt")"
BASE_REF="$(cat "$CACHE_DIR/base_ref.txt")"
git diff --unified=80 "origin/$BASE_REF...$HEAD_SHA" -- <path> > "$CACHE_DIR/patches/<sanitized_path>.patch"
```

3. 對 `low_risk` 檔案做 lightweight review（命名、資源、配置、XML 結構）
4. 對所有 changed files 至少標記一次：`reviewed_in_depth` / `reviewed_lightweight` / `needs_escalation`

## 4-5. 升級條件 {#4-5}

符合任一條件，就升級成完整 diff / 關聯檔閱讀：

- 同一檔案有多個修改區塊
- 涉及 public API / method signature / state model
- 涉及 lifecycle / coroutine / Flow / navigation
- 涉及跨模組依賴
- 僅靠輕量檢視無法判斷風險

## 4-6. 問題分級與 Verdict {#4-6}

問題分級與 review 文字結構一律沿用 `android-reviewer`。

- 有未解決 `BLOCKER` 或 `IMPORTANT` → `REQUEST_CHANGES`
- 否則 → `APPROVE`

## 4-7. 審查邊界 {#4-7}

- 正式問題只針對 `changed_files.txt` 中的檔案提出
- 可讀 diff 外檔案補 context，但不對未修改檔案發 inline comment
- 只做 lightweight review 的檔案，未升級前不下 `BLOCKER`

## 4-7.5. 寫入 `review_inventory.json` {#4-75}

review 分析完成後，**在寫 manifest 之前**必須先寫此檔案，這是 `android-pr-fixer` 重用 review 結果的依據。

```bash
CACHE_DIR="$(cat /tmp/android_pr_review_state_<PR_NUMBER>/cache_dir.txt)"
```

取得 `CACHE_DIR` 後，**使用 `Write` tool** 將以下格式的 JSON 寫入 `$CACHE_DIR/review_inventory.json`：

```json
{
  "schema_version": 1,
  "review_mode": "full_diff",
  "files_reviewed": [
    {"path": "app/src/.../Foo.kt", "depth": "full", "risk": "high"},
    {"path": "app/src/main/res/values-v26/styles.xml", "depth": "lightweight", "risk": "low"}
  ],
  "issues": [
    {"severity": "OPTIONAL", "file": "app/src/.../Foo.kt", "line": 65, "summary": "一行描述"}
  ],
  "verdict": "APPROVE"
}
```

`files_reviewed` 必須涵蓋 `changed_files.txt` 中的所有檔案；`review_mode` 填 `"full_diff"` 或 `"staged_escalation"`；`depth` 填 `"full"` 或 `"lightweight"`；`verdict` 填 `"APPROVE"` 或 `"REQUEST_CHANGES"`。

## 4-8. 寫入 `manifest.json` {#4-8}

當所有 artifacts 完成後才寫 `manifest.json`，它是後續 skill 判斷 shared state 是否完整的唯一依據。

先用 bash 取得現有檔案清單：

```bash
CACHE_DIR="$(cat /tmp/android_pr_review_state_<PR_NUMBER>/cache_dir.txt)"
HEAD_SHA="$(cat "$CACHE_DIR/head_sha.txt")"
ls "$CACHE_DIR" | grep -v '^manifest\.json$' | sort
```

再**使用 `Write` tool** 將以下格式的 JSON 寫入 `$CACHE_DIR/manifest.json`（`available_files` 填入上一步 `ls` 的結果）：

```json
{
  "schema_version": 1,
  "producer": "android-pr-review-optimized",
  "pr_number": "<PR_NUMBER>",
  "head_sha": "<HEAD_SHA>",
  "available_files": ["base_ref.txt", "cache_dir.txt", "changed_files.txt", "..."]
}
```
