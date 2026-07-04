---
name: smart-commit
description: 智能 commit：自動從 git log 推斷 ticket 號碼與 fix/feat 類型，不確定時才詢問使用者。觸發時機：使用者說「commit」、「幫我 commit」、「smart-commit」。
---

# Smart Commit

自動分析目前的修改，推斷 ticket 號碼與 commit 類型，提出 commit message 讓使用者確認或調整。

---

## Step 1：收集資訊

並行執行：

```bash
# 已 staged 的檔案
git diff --cached --name-only

# 尚未 staged 的修改（已追蹤檔案）
git diff --name-only

# 完整 diff（含 staged + unstaged）
git diff HEAD
```

若 staged 為空且有 unstaged 修改，將所有修改的已追蹤檔案視為本次 commit 範圍。

---

## Step 2：推斷 Ticket 號碼

對 Step 1 取得的每個修改檔案，執行：

```bash
git log --oneline -5 -- <檔案路徑>
```

從 log 中提取格式為 `[QMON-XXXXX]` 的 ticket 號碼，按以下規則決定：

- **所有檔案指向同一個 ticket** → 直接採用，不詢問
- **檔案分屬不同 ticket** → 詢問使用者選擇哪個 ticket
- **找不到任何 ticket** → 詢問使用者提供 ticket 號碼

---

## Step 3：推斷 Commit 類型（fix vs feat）

依照以下規則判斷：

| 類型 | 條件 |
|------|------|
| `fix:` | 修正已發布或已存在功能的 **bug**（行為錯誤、crash、資料錯誤） |
| `feat:` | 開發中的新功能、架構改善、重構、新增 API、補上遺漏的設計（即使是修正 code review 問題，開發階段一律用 `feat:`） |

**判斷依據**：
1. 閱讀 diff 內容判斷變更性質
2. 參考 Step 2 取得的近期 commit message 風格與同一 ticket 的歷史提交

若無法確定，在 Step 4 的 AskUserQuestion 中一併詢問。

---

## Step 4：提出 Commit Message 並確認

使用 **AskUserQuestion 工具**，一次詢問：

**問題 1 — Commit Message**（header: `Commit Message`）

列出 1–2 個建議選項，格式為：
```
feat/fix: <繁體中文描述>。[QMON-XXXXX]
```

- option 1：根據 diff 自動生成的建議（標示 Recommended）
- option 2（若有）：另一個角度的描述
- 使用者可選 "Other" 自行輸入完整 message

**若 ticket 或類型不確定**，在同一輪 AskUserQuestion 中一起詢問，不分多輪。

---

## Step 5：執行 Commit 並 Push

依使用者確認的 message 執行：

```bash
# 若有未 staged 的修改，先 stage
git add <修改的已追蹤檔案>

git commit -m "$(cat <<'EOF'
<確認的 commit message>

Co-Authored-By: Claude Sonnet 4.6 <noreply@anthropic.com>
EOF
)"

git push origin <當前分支名稱>
```

Commit + push 完成後輸出 commit hash。

---

## 注意事項

- 不 stage `.env`、credentials、大型 binary 等敏感或無關檔案
- Commit message 一律**繁體中文描述**，ticket 格式 `[QMON-XXXXX]`
- 描述句末加全形句號（。），ticket 接在後面：`feat: 說明。[QMON-11881]`
- 不使用 `--no-verify` 或跳過 hooks
