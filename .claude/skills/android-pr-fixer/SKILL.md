---
name: android-pr-fixer
description: 讀取 PR 現有審查 comments，自動分類後直接修改原始碼並回覆通知。觸發時機：使用者說「android-pr-fixer」、「幫我修 PR #X 的 issues」、「處理 PR review」、「fix PR」、「修 PR 的 review comments」，或在看完 PR review 後要求批量處理。只需提供 PR 號碼，分類偏好可選填（預設：BLOCKER/IMPORTANT 全修，OPTIONAL 略過）。若同 session 已跑過 android-pr-review-optimized，必須優先重用它留下的 shared state；只有找不到或驗證失敗時，才補跑必要 gh/bash。
---

# Android PR Fixer

你是資深 Android 工程師，負責在 PR 審查後自動處理 issue 分類、程式碼修正與 reviewer 回覆。

---

## 輸入格式

呼叫者只需提供 PR 號碼，其餘為選填：

```text
PR_NUMBER: <PR 號碼，例如 1025>

# 以下皆為選填，未提供時使用預設值
SKIP_OPTIONAL: true/false   # 預設 true（OPTIONAL 全部略過）

FALSE_POSITIVES:
  - <issue 描述或 comment 關鍵字> — 原因：<誤報理由>

SKIP:
  - <issue 描述>

FIX:
  - <issue 描述，含檔案名/行號>
```

**預設行為**（未提供 FALSE_POSITIVES / SKIP / FIX 時）：
- `BLOCKER` / `IMPORTANT` → 全部修正
- `OPTIONAL` → 全部略過（不留言）
- 無誤報

---

## Step 0：載入 reviewer 知識（選用）

嘗試讀取審查員 Reference Patterns，若不存在則略過：

```bash
for agent in android-lifecycle-reviewer android-arch-reviewer android-coroutines-reviewer android-security-reviewer android-performance-reviewer; do
  [ -f ~/.claude/agents/${agent}.md ] && echo "EXISTS: ${agent}.md"
done
```

---

## Step 1：reuse-first bootstrap

### 1-1. 一次取得 live PR identity（必要，不可跳過）

即使同 session 已跑過 `android-pr-review-optimized`，這一步也**仍然要做一次 live 查詢**。  
原因：`android-pr-fixer` 不能只靠快取假設目前 `head_sha` 沒變。

把彼此相依的查詢放在**同一個 bash call**：

```bash
STATE_DIR="/tmp/android_pr_fixer_state_<PR_NUMBER>"
mkdir -p "$STATE_DIR"

REPO="$(gh repo view --json nameWithOwner -q '.nameWithOwner')"
printf '%s\n' "$REPO" > "$STATE_DIR/repo.txt"

gh api "repos/$REPO/pulls/<PR_NUMBER>" \
  --jq '{number, head_sha: .head.sha, head_ref: .head.ref, base_ref: .base.ref, changed_files: .changed_files, additions: .additions, deletions: .deletions}' \
  > "$STATE_DIR/pr_meta.json"
```

### 1-2. 檢查 `android-pr-review-optimized` shared state

shared state canonical 路徑：

```text
/tmp/copilot-pr-state/${USER:-unknown}/<owner>__<repo>/pr-<PR_NUMBER>/<HEAD_SHA>/
```

檢查規則：

1. `manifest.json` 必須存在，且為最後寫入的 commit marker
2. `manifest.json.head_sha` 必須等於 live PR 的 `head_sha`
3. manifest 宣告的必要檔案都必須存在
4. 建議加上 staleness guard；若超過 6 小時且使用者未要求強制重用，視為過期

可直接重用的穩定檔案只有：

- `repo.txt`
- `pr_meta.json`
- `head_sha.txt`
- `base_ref.txt`
- `changed_files.txt`
- `changed_kt_files.txt`
- `diff_stat.txt`
- `full_diff.patch` 或 `patches/*.patch`（若存在）

**不要把以下資料當成 authoritative cache input：**

- `review_inventory.json`
- 舊的 comments snapshot
- 舊的 thread map

這些都只能當參考快照，不能取代 live GitHub 查詢。

### 1-3. cache hit 時怎麼做

若 shared state 驗證通過：

1. 記錄 `CACHE_DIR` 到 fixer 自己的 state 檔案
2. 直接重用穩定 PR context
3. **不要**重跑那些 review-optimized 已經產生且可直接重用的本地 diff / changed files bash

可接受的 cache-hit 驗證 / 落地方式：

```bash
STATE_DIR="/tmp/android_pr_fixer_state_<PR_NUMBER>"
REPO="$(cat "$STATE_DIR/repo.txt")"
HEAD_SHA="$(python - <<'PY'
import json
from pathlib import Path
state_dir = Path("/tmp/android_pr_fixer_state_<PR_NUMBER>")
print(json.loads((state_dir / "pr_meta.json").read_text())["head_sha"])
PY
)"
REPO_SAFE="${REPO/\//__}"
CACHE_DIR="/tmp/copilot-pr-state/${USER:-unknown}/$REPO_SAFE/pr-<PR_NUMBER>/$HEAD_SHA"

CACHE_DIR="$CACHE_DIR" HEAD_SHA="$HEAD_SHA" python - <<'PY'
import json
import os
from pathlib import Path

cache_dir = Path(os.environ["CACHE_DIR"])
manifest = json.loads((cache_dir / "manifest.json").read_text())
required = ["repo.txt", "pr_meta.json", "head_sha.txt", "base_ref.txt", "changed_files.txt", "diff_stat.txt"]
missing = [name for name in required if not (cache_dir / name).exists()]
assert manifest["head_sha"] == os.environ["HEAD_SHA"], "stale head sha"
assert not missing, f"missing files: {missing}"
PY

printf '%s\n' "$CACHE_DIR" > "$STATE_DIR/cache_dir.txt"
```

### 1-4. cache miss 時怎麼做

若 shared state 不存在、manifest 不完整、`head_sha` 不一致，或已過期：

1. 明確記錄 miss 原因
2. 只補抓 fixer 真正需要的最小 stable context
3. 不要假裝已命中 cache

可接受的 cold bootstrap 例子：

```bash
HEAD_SHA="$(python - <<'PY'
import json
from pathlib import Path
state_dir = Path("/tmp/android_pr_fixer_state_<PR_NUMBER>")
print(json.loads((state_dir / "pr_meta.json").read_text())["head_sha"])
PY
)"
BASE_REF="$(python - <<'PY'
import json
from pathlib import Path
state_dir = Path("/tmp/android_pr_fixer_state_<PR_NUMBER>")
print(json.loads((state_dir / "pr_meta.json").read_text())["base_ref"])
PY
)"
REPO="$(cat /tmp/android_pr_fixer_state_<PR_NUMBER>/repo.txt)"
REPO_SAFE="${REPO/\//__}"
CACHE_DIR="/tmp/copilot-pr-state/${USER:-unknown}/$REPO_SAFE/pr-<PR_NUMBER>/$HEAD_SHA"
mkdir -p "$CACHE_DIR"

git fetch --quiet origin "$BASE_REF" "$(python - <<'PY'
import json
from pathlib import Path
state_dir = Path('/tmp/android_pr_fixer_state_<PR_NUMBER>')
print(json.loads((state_dir / 'pr_meta.json').read_text())['head_ref'])
PY
)"
git diff --name-only "origin/$BASE_REF...$HEAD_SHA" > "$CACHE_DIR/changed_files.txt"
git diff --stat "origin/$BASE_REF...$HEAD_SHA" > "$CACHE_DIR/diff_stat.txt"
grep -E '\.kt$' "$CACHE_DIR/changed_files.txt" > "$CACHE_DIR/changed_kt_files.txt" || true

printf '%s\n' "$CACHE_DIR" > /tmp/android_pr_fixer_state_<PR_NUMBER>/cache_dir.txt
printf '%s\n' "$HEAD_SHA" > "$CACHE_DIR/head_sha.txt"
printf '%s\n' "$BASE_REF" > "$CACHE_DIR/base_ref.txt"
cp /tmp/android_pr_fixer_state_<PR_NUMBER>/repo.txt "$CACHE_DIR/repo.txt"
cp /tmp/android_pr_fixer_state_<PR_NUMBER>/pr_meta.json "$CACHE_DIR/pr_meta.json"
```

### 1-5. 無論 cache hit / miss，都要 refresh 動態 review 資料

`comments` 與 `thread map` 是動態資料；這一步不可省略。

同一個 bash call 內順序執行：

```bash
STATE_DIR="/tmp/android_pr_fixer_state_<PR_NUMBER>"
REPO="$(cat "$STATE_DIR/repo.txt")"
OWNER="${REPO%/*}"
REPO_NAME="${REPO#*/}"

gh api "repos/$REPO/pulls/<PR_NUMBER>/comments" \
  > "$STATE_DIR/comments.json"

gh api graphql --input - <<'EOF' > "$STATE_DIR/review_threads.json"
{
  "query": "query($owner:String!, $repo:String!, $number:Int!) { repository(owner:$owner, name:$repo) { pullRequest(number:$number) { reviewThreads(first:100) { nodes { id isResolved comments(first:20) { nodes { databaseId body url } } } } } } }",
  "variables": {
    "owner": "'"$OWNER"'",
    "repo": "'"$REPO_NAME"'",
    "number": <PR_NUMBER>
  }
}
EOF
```

之後再從 `comments.json` 與 `review_threads.json` 產生本輪使用的 `thread_map.json`。  
`thread_map.json` 只保證對**這一輪 fixer** 有效，不保證下輪還能重用。

---

## Step 2：分類 PR comments

從 `comments.json` 讀取每條 comment body，依以下規則分類：

| 關鍵字（body 內） | 分類 |
|---|---|
| `[BLOCKER]` | → FIX |
| `[IMPORTANT]` | → FIX |
| `[OPTIONAL]` | → SKIP（若 `SKIP_OPTIONAL=true`）或 FIX（若 `false`） |

**若呼叫者已提供 FALSE_POSITIVES / SKIP / FIX 清單**，以呼叫者清單優先，自動分類只補足未明確指定的 comment。

分類完成後，用 **`ask_user` 工具**展示處理計畫並詢問確認：

- 問題：`以下是本次 PR #<PR_NUMBER> 的處理計畫，確認後開始執行？`
- 選項：`確認，開始修正` / `調整分類`

若使用者選擇「調整分類」，依補充內容重新分類後再確認一次。

---

## Step 2.5：建立 SQL todo 清單（使用者確認後立即執行）

> 這是強制步驟，不可略過。目的是用資料庫狀態機強制「一次只處理一個 issue」，避免批次修改後統一 commit。

每個 FIX item 對應一筆 todo，`description` 至少包含：

- `path`
- `line`
- `comment_id`
- `thread_id`
- 原始 issue 摘要

範例：

```sql
INSERT INTO todos (id, title, description, status) VALUES
  ('fix-<file>-<line>', '[IMPORTANT] <問題標題>', 'path=<path>; line=<line>; comment_id=<id>; thread_id=<PRRT_xxx>', 'pending');
```

建立後先確認：

```sql
SELECT id, title, status FROM todos ORDER BY id;
```

---

## Step 3：處理誤報（FALSE_POSITIVES）

對每個誤報，使用 GraphQL thread reply；**不要回退到 REST replies endpoint**。

把 body 與 GraphQL payload 都放在檔案，並在**同一個 bash call** 內建立、送出、清理：

```bash
BODY_FILE="$(mktemp /tmp/pr_fixer_body_XXXXXX.txt)"
PAYLOAD_FILE="$(mktemp /tmp/pr_fixer_graphql_XXXXXX.json)"

cat > "$BODY_FILE" <<'EOF'
✅ **已確認為誤報 — 不需修正**

**原因**：<誤報原因>
EOF

BODY_FILE="$BODY_FILE" python - <<'PY' > "$PAYLOAD_FILE"
import json
import os
from pathlib import Path

body = Path(os.environ["BODY_FILE"]).read_text()
payload = {
    "query": "mutation($threadId:ID!, $body:String!) { addPullRequestReviewThreadReply(input:{pullRequestReviewThreadId:$threadId, body:$body}) { comment { id } } }",
    "variables": {
        "threadId": "<THREAD_ID>",
        "body": body,
    },
}
print(json.dumps(payload, ensure_ascii=False))
PY

gh api graphql --input "$PAYLOAD_FILE"
rm -f "$BODY_FILE" "$PAYLOAD_FILE"
```

---

## Step 4：逐項修正（FIX）

> 絕對禁止批次修改：不得同時修改多個 issue 後統一 commit。

每個 issue 的完整循環如下，缺一不可：

```text
SELECT 一個 pending todo
→ UPDATE in_progress
→ 讀檔
→ 修改
→ format
→ 編譯 / 測試必要範圍
→ commit
→ push
→ GraphQL 留言
→ UPDATE done
```

### 4-1. 取得下一個待處理 issue

```sql
SELECT id, title FROM todos WHERE status = 'in_progress';
SELECT id, title, description FROM todos WHERE status = 'pending' LIMIT 1;
UPDATE todos SET status = 'in_progress' WHERE id = '<todo_id>';
```

若有殘留 `in_progress`，先完成它，不要跳到下一筆。

### 4-2. 讀取原始碼

用 `view` 工具讀完整目標檔案；必要時再一起讀關聯檔案（Fragment、ViewModel、Repository、navigation、DI）。

### 4-3. 執行修正

用 `apply_patch` 做精準修改。

規則：

- 修正範圍最小化，只改對應 issue 所需區塊
- 不得順手重構無關邏輯
- 不得整行刪除原有中文或英文註解；若需要調整，改內容即可
- 若業務邏輯不足以安全判斷，停止自動修改並改走「留言說明需人工確認」

### 4-4. reformat 修改檔案

commit 前必須執行**專案已存在**的 formatter task；不要用 `|| true` 吞掉失敗。

可接受的順序：

1. 若專案已有 `ktlintFormat`，執行它
2. 否則若已有 `spotlessApply`，執行它
3. 若兩者都沒有，至少人工維持周邊格式一致，並在最終摘要說明未找到 formatter task

範例：

```bash
./gradlew ktlintFormat -PinternalKtlint=true
```

或：

```bash
./gradlew spotlessApply
```

### 4-5. 編譯 / 測試驗證

不得用 `tail -20` 之類的截斷輸出來判斷成功與否；要以命令 exit code 為準。

優先跑**專案已存在且最貼近修改範圍**的 task；若無法判斷，才回退：

```bash
./gradlew assembleDebug
```

若命令失敗：

1. 不可 commit
2. 先修正編譯錯誤
3. 重新執行同一驗證步驟

### 4-6. Smart commit

若 `smart-commit` skill 可用，依其流程產生 commit message。  
若不可用，至少做到：

1. 先看最近相關 git log 推斷 ticket / 類型
2. 用 `ask_user` 確認 commit message
3. `git add <相關檔案>` → `git commit` → `git push`

### 4-7. GraphQL 回覆 reviewer

GraphQL reply 必須使用 variables / file-backed payload，不可把 body 直接內插進 query 字串。

回覆格式：

````text
✅ **已修正並 commit**

**修正說明**：<一行說明改了什麼>

```kotlin
// 修正後的關鍵片段（5 行內）
```

Commit: `<git log --oneline -1 的輸出>`
````

若找不到對應 `thread_id`，可以完成 code change / commit，但必須在最終摘要明確列出「未成功留言」。

### 4-8. 標記完成後再進下一筆

```sql
UPDATE todos SET status = 'done' WHERE id = '<todo_id>';
SELECT status, COUNT(*) FROM todos GROUP BY status;
```

只有當前 todo 完整完成後，才能 SELECT 下一個 pending。

---

## Step 5：處理略過項目（SKIP）

若使用者決定略過，仍需留言告知 reviewer。  
同樣使用 GraphQL thread reply，不可用 REST replies。

回覆格式：

```text
📌 **暫時略過**

原因：<略過原因>
```

這一步與 Step 3 / Step 4-7 相同：body 與 GraphQL payload 都放檔案，且在同一個 bash call 內建立與清理。

---

## Step 6：輸出執行摘要

```text
## 🔧 PR Fixer 執行摘要（PR #<PR_NUMBER>）

### ✅ 已修正並 commit + push（<N> 個）
- `<File.kt:行號>` — 修正說明（commit hash）

### 🚫 誤報（<N> 個）
- `<File.kt:行號>` — 已留言說明

### 📌 略過（<N> 個）
- `<File.kt:行號>` — 略過原因

### ⚠️ 未完成 / 需人工處理
- `<File.kt:行號>` — 原因

### ♻️ Shared state 使用情況
- `cache hit` / `cache miss`
- 若 miss，列出原因（manifest 缺失 / head_sha 不一致 / 已過期）
```

---

## 一般注意事項

- 這個 skill 的 reuse-first 原則是：**先做一次 live PR identity 查詢，再決定能否重用 shared state**
- 不要假設 shell 變數會跨 bash 保留；跨步驟資料都要寫檔
- 所有 GitHub 留言一律用繁體中文
- 對 GitHub API body / GraphQL variables，一律用 `--input` 傳 payload 檔，不要直接把多行 body 內插進 shell 指令
- 若單一 comment 留言失敗，要把錯誤記進摘要；不要假裝已通知成功
- 若本輪曾讀到 `android-pr-review-optimized` 留下的 `review_inventory.json`，只能把它當作 prior analysis snapshot，不得當成 live review state
