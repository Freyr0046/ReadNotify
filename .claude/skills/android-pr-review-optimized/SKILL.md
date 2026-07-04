---
name: android-pr-review-optimized
description: 低 token 成本版 Android PR review 流程。建立或更新當前分支的 Pull Request，優先重用本地 git 物件與 session cache，以分層 diff 策略執行 Android production-grade code review，最後以繁體中文發布 inline comments + summary。當使用者要「開 PR 並 review」、希望「省 token 審 PR」、PR 很大、或明確提到「android-pr-review-optimized」時，優先使用此 skill。
---

# Android PR Review Optimized

低 token 成本的 PR 建立 + Android code review 完整流程。詳細實作分散在子文件，按需載入。

## 工作流程清單

複製此清單追蹤進度：

```
- [ ] Step 0：前置檢查、載入 android-reviewer、詢問目標分支與 PR 標題
- [ ] Step 1：Push 來源分支、建立或沿用 PR
- [ ] Step 2-3：建立 session cache，決定內容來源模式 A/B → CACHE.md
- [ ] Step 4：執行分層審查、寫入 inventory 與 manifest → REVIEW.md
- [ ] Step 5：驗證行號、組裝 payload、發布繁體中文 review → PUBLISH.md
```

## 核心設計原則

1. **來源分支先 push**，之後盡量用本地 git 物件做 diff 與全文讀檔。
2. **GitHub 查詢結果落地成 session cache**，避免重複打 `gh`。
3. 小 PR 直接完整審查；中大型 PR 先分層掃描，再依風險升級。
4. 本地工作區和 PR head 不一致時，切到 **PR head snapshot**，不腦補忽略。
5. **Code review 前一定要先以 skill 載入 `android-reviewer`**。

## Bash Orchestration 原則

- 同一階段有依賴的指令，放在**同一個 bash call** 內執行
- 跨步驟重用的值，一律落地到檔案（`cache_dir.txt`、`head_sha.txt` 等）
- **不要假設 shell 變數會跨 bash 保留**

---

## Step 0：前置檢查與一次性詢問

### 0-1. Dependency Check

```bash
gh auth status 2>&1 | grep -q "Logged in" || { echo "ERROR: gh not authenticated. Run: gh auth login"; exit 1; }
```

失敗就停止。

### 0-1.5. 強制載入 `android-reviewer`

在進入任何 code review 判斷前，**必須先以 skill tool 載入 `android-reviewer`**。
若無法載入，**立即停止**，不要退化成手動模擬 reviewer 流程。

### 0-2. 詢問目標分支（Round 1）

用 `AskUserQuestion` 詢問目標分支：`develop`（Recommended）/ `INT` / `UAT` / `master`。

取得後立即確認實際新 commits：

```bash
git fetch --quiet origin <target_branch> 2>&1 || true
git log origin/<target_branch>..HEAD --oneline > /tmp/pr_review_new_commits_tmp.txt
cat /tmp/pr_review_new_commits_tmp.txt
```

**不能用 session context 的 Recent commits 代替**，那些 commits 可能已在目標分支上。

### 0-3. 詢問 PR 標題（Round 2）

依上一步結果產生 2–3 個標題建議，用 `AskUserQuestion` 確認：
- 多個 Issue 合併為 `fix: <主題> (QMON-XXXX/XXXX)`
- 只涵蓋 `new_commits.txt` 裡出現的 Issue

---

## Step 1：Push 並建立 / 沿用 PR

### 1-1. Push 來源分支

```bash
git push -u origin <source_branch>
```

Push 失敗就回報並停止。這是整個低 token 流程的前提。

### 1-2. 收集 PR 資訊

```bash
git fetch --quiet origin <source_branch>
git log origin/<target_branch>..HEAD --oneline > /tmp/pr_review_new_commits_tmp.txt
gh pr list --head <source_branch> --json number,title,state,url
```

### 1-3. 建立或沿用 PR

- 有 open PR → 直接取用其 `number`
- 沒有 → 用 `gh pr create` 建立

PR Body 必須以 `git log origin/<target_branch>..HEAD --oneline` 結果為準，至少包含：
- `## Summary`：每行對應一條實際新 commit，格式 `- <issue 編號>: <commit 訊息>`
- `## Test plan`：依 Summary 列出驗證步驟

記錄 `PR_NUMBER`，後續步驟全部依賴此值。

---

## 後續步驟

| 步驟 | 文件 | 說明 |
|------|------|------|
| Step 2–3 | [CACHE.md](CACHE.md) | Session cache 建立、diff 素材、內容來源模式 A/B |
| Step 4 | [REVIEW.md](REVIEW.md) | 分層審查策略、問題分級、inventory 與 manifest |
| Step 5 | [PUBLISH.md](PUBLISH.md) | 行號驗證、payload 組裝、發布 review |
| Evaluations | [evals/](evals/) | 三個代表性測試場景 |

---

## 注意事項

- 所有 review 內容**一律用繁體中文**
- 只要 `head_sha` 改變，就必須重建 cache
- 若建立了暫時 `worktree`，結束前務必清理
- 正式問題只針對 `changed_files.txt` 中的檔案提出
