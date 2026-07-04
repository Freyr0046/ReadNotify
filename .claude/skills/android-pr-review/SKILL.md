---
name: android-pr-review
description: 一鍵完成「建立 Pull Request → Android Code Review → 發布繁體中文 Review Comments」的完整流程。觸發時機：使用者要求對當前分支發起 PR 並進行 code review，或說「幫我開 PR 並 review」、「發 PR 到 X 分支並審查」、「PR + code review」、「android-pr-review」等。
---

# Android PR & Review

對當前分支建立 PR，執行 Android production-grade code review，並以繁體中文將結果以 inline comments + summary 發布到 GitHub。

## 流程

### Step 0：確認必要資訊（執行任何動作前必須完成）

#### 0-1. Dependency Check（立即執行，若失敗立即停止）

執行以下檢查：
```bash
# 檢查 android-reviewer skill 可用
if [ -f .claude/skills/android-reviewer/SKILL.md ]; then
  echo "OK: project android-reviewer skill"
elif [ -f ~/.claude/skills/android-reviewer/SKILL.md ]; then
  echo "OK: global android-reviewer skill"
else
  echo "MISSING: android-reviewer/SKILL.md"
fi

# 檢查 GitHub CLI 登入狀態
gh auth status
```

若輸出 `MISSING:` 或 `gh auth status` 失敗，**立即停止**，回報缺失後不執行後續步驟。

#### 0-2. 收集資訊並一次詢問（僅一輪等待）

並行執行以下指令收集候選資訊：
```bash
git branch --show-current
git branch -r | grep -E 'develop|INT|UAT|master' | head -10
git log --oneline -10
```

使用 **AskUserQuestion 工具**在**同一輪**詢問三件事（來源分支、目標分支、PR 標題），再等待一次回覆：

- **問題 1 — 來源分支**（header: `來源分支`）  
  option 1：`<current_branch>`（Recommended）；其餘依偵測結果列出。可選 "Other"。
- **問題 2 — 目標分支**（header: `目標分支`）  
  最多列 4 個遠端候選分支（如 develop / INT / UAT / master）。可選 "Other"。
- **問題 3 — PR 標題**（header: `PR 標題`）  
  依 commit log 產生 2–4 個建議選項。可選 "Other"。

---

### Step 1：建立 Pull Request

並行執行以下指令收集資訊：
```bash
git log <target_branch>..HEAD --oneline
git diff <target_branch>...HEAD --stat
git remote -v
```

以 `gh pr create` 建立 PR，Body 包含 Summary、Commits、Test plan 三個區塊。記錄回傳的 PR number。

---

### Step 2：執行 Android Code Review

#### 2-1. 取得 PR Diff 與變更檔案清單

```bash
gh pr diff <PR_NUMBER>
gh pr diff <PR_NUMBER> --name-only
```

從 `--name-only` 取得：
- `CHANGED_FILES`：所有變更檔案
- `CHANGED_KT_FILES`：僅 `.kt` 檔

#### 2-2. 審查策略

若 `CHANGED_KT_FILES` 為空：
- 執行**輕量 Android review**（Manifest / Gradle / XML / ProGuard / 資源命名與配置）
- 跳過 Kotlin 專屬項目（如 coroutine scope、Flow 收集、ViewModel Kotlin 實作細節）

若 `CHANGED_KT_FILES` 非空：
- 依照 **android-reviewer** skill 的 Multi-Pass 方法執行完整審查：
- Pass 1：Triage & Risk Assessment
- Pass 2：Structural & Architectural Analysis（MVVM、ViewBinding、DI、跨模組依賴）
- Pass 3：Android Lifecycle、Coroutines、Flow、Navigation
- Pass 4：Performance & Memory Analysis

將問題分類為 `BLOCKER` / `IMPORTANT` / `OPTIONAL`。
並依 `android-reviewer` 規則給出 `Verdict`：
- 有未解決 `BLOCKER` 或 `IMPORTANT` → `REQUEST CHANGES`
- 其餘 → `APPROVE`

---

### Step 3：發布 Review（繁體中文）

#### 3-1. 取得 repo 與 commit SHA
```bash
gh repo view --json nameWithOwner -q '.nameWithOwner'
gh api repos/<owner>/<repo>/pulls/<PR_NUMBER> --jq '.head.sha'
```

第一行取得 `<owner>/<repo>`（例如 `spring-trees/aOS_InvoiceMaster`），代入後續 API 路徑。

#### 3-2. 建立 JSON（使用 mktemp）並驗證 comment 位置

**必須將 JSON 寫入暫存檔再用 `--input` 傳入**，原因：
1. 自己的 PR 無法使用 `REQUEST_CHANGES`，一律用 `"event": "COMMENT"`
2. Body 或 comment 含 `${...}` 時，直接在 shell 展開會觸發 zsh `bad substitution` 錯誤

```bash
TMP_JSON="$(mktemp /tmp/pr_review_XXXXXX.json)"
trap 'rm -f "$TMP_JSON"' EXIT

cat > "$TMP_JSON" << 'ENDJSON'
{
  "commit_id": "<HEAD_SHA>",
  "event": "COMMENT",
  "body": "<整體 review summary>",
  "comments": [
    {
      "path": "app/src/.../Foo.kt",
      "line": 42,
      "side": "RIGHT",
      "body": "<inline comment>"
    }
  ]
}
ENDJSON

# 驗證 inline comment 目標：
# 1) path 必須存在於 CHANGED_FILES
# 2) line 必須是 PR diff 右側可評論的新行號
# 驗證失敗的 comment：降級到 summary 的「其他觀察」，不要保留在 comments 陣列
```

Inline comment 必要欄位：`path`（repo root 相對路徑）、`line`（新檔案行號）、`side: "RIGHT"`。  
只允許對 `gh pr diff --name-only` 列出的檔案發 inline comments。

#### 3-3. 發布 Review（含重試）

```bash
for attempt in 1 2 3; do
  if gh api repos/<owner>/<repo>/pulls/<PR_NUMBER>/reviews \
    --method POST \
    --input "$TMP_JSON"; then
    break
  fi

  if [ "$attempt" -eq 3 ]; then
    echo "ERROR: review publish failed after retries"
    exit 1
  fi

  sleep $((attempt * 2))
done
```

---

## Review 輸出格式（繁體中文）

### 整體 Summary Body

```
## 🔍 PR Summary & Strategy
- **Verdict**: APPROVE / REQUEST CHANGES
- **Detected Type** / **Risk Level** 🔴🟡🟢 / **Review Focus**
- **Overall Assessment**: Strengths / Concerns / Code Quality X/10

## 🎯 Fix Priority
1. 🚨 [BLOCKER] ...（見 inline comment）
2. ⚠️ [IMPORTANT] ...
3. 💡 [OPTIONAL] ...

## ✅ Positive Observations
- `File.kt:行號` — 一行說明（只在有真正亮點時加入）

## 📋 Review Checklist
- [x] / [ ] 各項目

## ✅ Pre-Merge Requirements
- [ ] 所有 BLOCKER 解決
- [ ] IMPORTANT 解決或建立 tech debt ticket
```

### Inline Comment 格式

```
🚨/⚠️/💡 **[等級] 分類 — 標題**

問題描述（繁體中文）

❌ **目前**：
```kotlin
// 問題程式碼
```

✅ **建議**：
```kotlin
// 修正後程式碼
```

💡 **Why**: 說明原因
🎲 Risk: 🔴/🟡/🟢 風險說明
🧪 Test: 驗證方式（BLOCKER 才需要）
```

---

## 注意事項

- 所有 review 內容**一律用繁體中文**
- 參考 android-reviewer skill 的完整 checklist 確保不遺漏 Android 特有問題
- PR diff > 500 行時，優先聚焦架構層與生命週期問題
- 審查過程可讀取 diff 以外檔案補 context，但正式問題（BLOCKER/IMPORTANT/OPTIONAL）僅針對 diff 內修改檔案提出
- 若目標行無法建立 inline comment（path/line 驗證未通過），請降級寫入 summary「其他觀察」，不要強行送 API
