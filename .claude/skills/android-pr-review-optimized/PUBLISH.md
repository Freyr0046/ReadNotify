# 發布 Review（Step 5）

所有 review 內容**一律用繁體中文**。review 章節、問題分級、comment 呈現方式以 `android-reviewer` 為唯一準則。

## 目錄
- [5-1 重用 cache 內的 repo 與 SHA](#5-1)
- [5-2 驗證 inline comment 的 path 與 line](#5-2)
- [5-3 組裝 review payload](#5-3)
- [5-4 寫入 payload 暫存檔](#5-4)
- [5-5 發布 review（含重試）](#5-5)

---

## 5-1. 重用 Cache 內的 Repo 與 SHA {#5-1}

從 cache 讀以下資料，不要重跑 `gh repo view` / `gh api pulls/<PR>`：

- `repo.txt`
- `pr_meta.json`
- `changed_files.txt`

## 5-2. 驗證 Inline Comment 的 Path 與 Line {#5-2}

**使用 `Read` tool** 開啟目標檔案，直接對照行號確認目標程式碼位置。
估算行號幾乎必錯——diff 只顯示修改區塊，不含整個檔案的行計數。

驗證清單（每則 inline comment 都必須通過）：

1. `path` 必須存在於 `changed_files.txt`
2. `line` 必須是 `Read` tool 顯示的實際行號，且該行必須出現在 diff 的 `+` 區塊（右側）

若確認的行不在 `+` 區塊內，該 comment 降級寫入 summary，不發 inline。

## 5-3. 組裝 Review Payload {#5-3}

review summary、fix priority、positive observations、checklist、pre-merge requirements 與 inline comments 的格式，**全部直接使用 `android-reviewer` 的輸出結構**。

本 skill 只負責：

1. 把 review 結論整理成 GitHub Review API 可接受的 JSON
2. 決定 `event`（自己的 PR 固定 `COMMENT`）
3. 驗證每則 inline comment 的 `path` / `line`
4. 無法驗證的 observation 降級到 summary，不硬送 API

本 skill 補充的 orchestration 欄位：
- `Review Mode`: workspace / snapshot / full-diff / staged-escalation
- `Coverage Notes`: 深度審查 / 輕量審查 / 升級審查檔案

## 5-4. 寫入 Payload 暫存檔 {#5-4}

**使用 `Write` tool** 將 review payload JSON 寫入 `TMP_JSON="/tmp/pr_review_out_<PR_NUMBER>.json"`：

```json
{
  "commit_id": "<HEAD_SHA>",
  "event": "COMMENT",
  "body": "...",
  "comments": [
    {"path": "...", "line": 42, "side": "RIGHT", "body": "..."}
  ]
}
```

> **注意**：不要用 `mktemp /tmp/XXXXXX.json`（副檔名在 X 後面，randomize 失效），改用固定唯一路徑 `/tmp/pr_review_out_<PR_NUMBER>.json`。

## 5-5. 發布 Review（含重試）{#5-5}

5-4 與 5-5 合併在**同一個 bash call** 執行，送出後立即刪除暫存檔：

```bash
TMP_JSON="/tmp/pr_review_out_<PR_NUMBER>.json"
for attempt in 1 2 3; do
  if gh api repos/<owner>/<repo>/pulls/<PR_NUMBER>/reviews \
    --method POST \
    --input "$TMP_JSON"; then
    break
  fi
  if [ "$attempt" -eq 3 ]; then
    echo "ERROR: review publish failed after retries"
    rm -f "$TMP_JSON"
    exit 1
  fi
  sleep $((attempt * 2))
done
rm -f "$TMP_JSON"
```
