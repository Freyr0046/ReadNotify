# Session Cache 與內容來源模式（Step 2–3）

## 目錄
- [Step 2-1：建立 shared state 根目錄](#step-2-1)
- [Step 2-2：以 PR_NUMBER + HEAD_SHA 建 cache key](#step-2-2)
- [Step 2-3：Cache 使用規則](#step-2-3)
- [Step 2-4：提供給 android-pr-fixer 的 shared state 契約](#step-2-4)
- [Step 3-1：建立本地 diff 素材](#step-3-1)
- [Step 3-2：選擇內容來源模式 A/B](#step-3-2)

---

## Step 2-1. 建立 Shared State 根目錄 {#step-2-1}

只查一次 repo / PR metadata，reusable state 放到固定 canonical 路徑。
`android-pr-fixer` 重用前一次 review 成果靠這組檔案，不靠對話記憶。

```bash
STATE_DIR="/tmp/android_pr_review_state_<PR_NUMBER>"
mkdir -p "$STATE_DIR"

REPO="$(gh repo view --json nameWithOwner -q '.nameWithOwner')"
printf '%s\n' "$REPO" > "$STATE_DIR/repo.txt"

REPO_SAFE="${REPO/\//__}"
SHARED_PR_DIR="/tmp/copilot-pr-state/${USER:-unknown}/$REPO_SAFE/pr-<PR_NUMBER>"
mkdir -p "$SHARED_PR_DIR"
printf '%s\n' "$SHARED_PR_DIR" > "$STATE_DIR/shared_pr_dir.txt"

gh api "repos/$REPO/pulls/<PR_NUMBER>" \
  --jq '{number, head_sha: .head.sha, head_ref: .head.ref, head_label: .head.label, base_ref: .base.ref, changed_files: .changed_files, additions: .additions, deletions: .deletions}' \
  > "$STATE_DIR/pr_meta.json"
```

## Step 2-2. 以 PR_NUMBER + HEAD_SHA 建 Cache Key {#step-2-2}

在**同一個 bash call** 內從 `pr_meta.json` 取值，建 canonical cache 後把路徑寫回檔案：

```bash
STATE_DIR="/tmp/android_pr_review_state_<PR_NUMBER>"
HEAD_SHA="$(jq -r '.head_sha' "$STATE_DIR/pr_meta.json")"
BASE_REF="$(jq -r '.base_ref' "$STATE_DIR/pr_meta.json")"
SHARED_PR_DIR="$(cat "$STATE_DIR/shared_pr_dir.txt")"
CACHE_DIR="$SHARED_PR_DIR/$HEAD_SHA"
mkdir -p "$CACHE_DIR"
printf '%s\n' "$CACHE_DIR" > "$STATE_DIR/cache_dir.txt"
printf '%s\n' "$CACHE_DIR" > "$SHARED_PR_DIR/current_cache_dir.txt"
printf '%s\n' "$CACHE_DIR" > "$CACHE_DIR/cache_dir.txt"
printf '%s\n' "$HEAD_SHA" > "$CACHE_DIR/head_sha.txt"
printf '%s\n' "$BASE_REF" > "$CACHE_DIR/base_ref.txt"
cp "$STATE_DIR/repo.txt" "$CACHE_DIR/repo.txt"
cp "$STATE_DIR/pr_meta.json" "$CACHE_DIR/pr_meta.json"
```

Cache 必須包含以下檔案（`manifest.json` 最後落地）：

| 檔案 | 說明 |
|------|------|
| `repo.txt` | `owner/repo` |
| `pr_meta.json` | PR 基本資訊 |
| `head_sha.txt` | PR head commit SHA |
| `base_ref.txt` | 目標分支名稱 |
| `cache_dir.txt` | cache 根路徑 |
| `new_commits.txt` | PR body / 標題建議的唯一來源 |
| `changed_files.txt` | diff 變更檔案清單 |
| `changed_kt_files.txt` | Kotlin 檔清單 |
| `diff_stat.txt` | diff 統計 |
| `review_inventory.json` | review 結果快照 |
| `manifest.json` | **最後寫入**，作為 shared state 完成標記 |

## Step 2-3. Cache 使用規則 {#step-2-3}

- `head_sha` 相同時，**優先讀 cache，不重跑同一個 `gh` 查詢**
- `head_sha` 改變或使用者明確要求重抓時，才失效重建
- 不要把整份 full diff 原文長期存在 cache；只保存必要 patch 與分類結果
- 後續 bash 需要 `CACHE_DIR` / `HEAD_SHA` / `BASE_REF`，**從檔案讀回，不假設前一個 shell 的變數還在**
- `manifest.json` 必須在所有 artifacts 完成後最後落地；沒有 manifest 視同 shared state 未完成

## Step 2-4. 提供給 `android-pr-fixer` 的 Shared State 契約 {#step-2-4}

`android-pr-fixer` 可安全重用的只限**穩定 PR context**：

- `repo.txt` / `pr_meta.json` / `head_sha.txt` / `base_ref.txt`
- `changed_files.txt` / `changed_kt_files.txt` / `diff_stat.txt`
- `full_diff.patch` 或 `patches/*.patch`（若本輪已產生）

以下只代表**當下快照**，不可取代 live GitHub 查詢：

- `review_inventory.json`
- 任何 review comments / threads 的 snapshot 檔
- `snapshot_dir.txt`

---

## Step 3-1. 建立本地 Diff 素材 {#step-3-1}

在已 push 且已 fetch 的前提下，優先使用本地 git：

```bash
CACHE_DIR="$(cat /tmp/android_pr_review_state_<PR_NUMBER>/cache_dir.txt)"
HEAD_SHA="$(cat "$CACHE_DIR/head_sha.txt")"
BASE_REF="$(cat "$CACHE_DIR/base_ref.txt")"
git diff --name-only "origin/$BASE_REF...$HEAD_SHA" > "$CACHE_DIR/changed_files.txt"
git diff --stat "origin/$BASE_REF...$HEAD_SHA" > "$CACHE_DIR/diff_stat.txt"
grep -E '\.kt$' "$CACHE_DIR/changed_files.txt" > "$CACHE_DIR/changed_kt_files.txt" || true
```

只有本地 git 無法取得完整比較基準時，才回退到：

```bash
gh pr diff <PR_NUMBER> --name-only
```

## Step 3-2. 選擇內容來源模式 {#step-3-2}

**必須明確執行以下 bash，輸出 MODE 後才能繼續**：

```bash
CACHE_DIR="$(cat /tmp/android_pr_review_state_<PR_NUMBER>/cache_dir.txt)"
LOCAL_HEAD="$(git rev-parse HEAD)"
PR_HEAD_SHA="$(cat "$CACHE_DIR/head_sha.txt")"
TRACKED_CHANGES="$(git status --short | grep -v '^??')"

if [ "$LOCAL_HEAD" = "$PR_HEAD_SHA" ] && [ -z "$TRACKED_CHANGES" ]; then
    echo "CONTENT_MODE=A (Workspace Mode)"
    printf 'A\n' > "$CACHE_DIR/content_mode.txt"
else
    echo "CONTENT_MODE=B (Pinned Snapshot Mode)"
    echo "  LOCAL_HEAD : $LOCAL_HEAD"
    echo "  PR_HEAD_SHA: $PR_HEAD_SHA"
    [ -n "$TRACKED_CHANGES" ] && echo "  Tracked changes: $TRACKED_CHANGES"
    printf 'B\n' > "$CACHE_DIR/content_mode.txt"
fi
```

### Mode A：Workspace Mode（首選）

條件：`LOCAL_HEAD == PR_HEAD_SHA` 且無已追蹤檔案的修改。

可忽略的 untracked files 須同時滿足：
1. `git status --short` 只出現 `?? <file>`
2. 不在 `changed_files.txt` 裡
3. 不是高風險全域檔（`settings.gradle*`、`gradle.properties`、`build.gradle*`、`AndroidManifest.xml`、`proguard*`）

符合時，直接從工作目錄讀完整檔案。

### Mode B：Pinned Snapshot Mode（不一致時使用）

若 `LOCAL_HEAD != PR_HEAD_SHA` 或有已追蹤變更，改用 PR head 固定內容：

```bash
PR_HEAD_SHA="$(cat "$CACHE_DIR/head_sha.txt")"
git show "$PR_HEAD_SHA":<path>
```

需要大量全文閱讀時，建立暫時唯讀 worktree：

```bash
PR_HEAD_SHA="$(cat "$CACHE_DIR/head_sha.txt")"
SNAPSHOT_DIR="$(mktemp -d /tmp/pr_head_snapshot_XXXXXX)"
git worktree add --detach "$SNAPSHOT_DIR" "$PR_HEAD_SHA"
printf '%s\n' "$SNAPSHOT_DIR" > "$CACHE_DIR/snapshot_dir.txt"
```

完成後務必移除：

```bash
SNAPSHOT_DIR="$(cat "$CACHE_DIR/snapshot_dir.txt")"
git worktree remove "$SNAPSHOT_DIR" --force
rm -f "$CACHE_DIR/snapshot_dir.txt"
```
