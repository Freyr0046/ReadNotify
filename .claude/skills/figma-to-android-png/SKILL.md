---
name: figma-to-android-png
description: Export a Figma node as PNG at all 5 Android densities (mdpi/hdpi/xhdpi/xxhdpi/xxxhdpi) and save directly into the project's res/ drawable folders. Trigger when the user provides a Figma URL and wants to download PNG assets for Android, or says things like "幫我從 Figma 下載圖片", "export PNG to Android", "下載到對應資料夾", or "Figma 轉 Android PNG".
---

## Workflow

### 1. Gather inputs

**Figma URL** — must contain `node-id`. Parse:
- `fileKey`: path segment after `/design/`
- `nodeId`: `node-id` query param, convert `-` → `:` (e.g. `1427-6150` → `1427:6150`)

**Token** — resolve in this order:
1. Check env var: `echo $FIGMA_TOKEN`
2. Check `~/.zshrc` for `export FIGMA_TOKEN=...`
3. Only if both are empty, ask the user:
   > 請提供 Figma Personal Access Token（`figd_...`）。
   > 取得方式：figma.com/settings → Personal access tokens → 權限只需勾選 **File content: Read-only**

**Filename** — ask if not provided:
> 請輸入 Android 檔名（snake_case，不含 .png），例如 `bg_more_has_carrier`

### 2. Preview

Use `mcp__figma-remote-mcp__get_screenshot` with the parsed `fileKey` and `nodeId` to show a preview before downloading.

### 3. Find res/ path

Search the project for `app/src/main/res`:
```bash
find . -type d -name "res" -path "*/main/res" | head -1
```

### 4. Export & download

Run the bundled script:
```bash
python3 <skill_dir>/scripts/export.py <token> <fileKey> <nodeId> <filename> <res_path>
```

The script exports at 5 scales and saves:

| Scale | Folder |
|---|---|
| 1x | drawable-mdpi |
| 1.5x | drawable-hdpi |
| 2x | drawable-xhdpi |
| 3x | drawable-xxhdpi |
| 4x | drawable-xxxhdpi |

### 5. Confirm

Show the summary table printed by the script (folder / filename / file size).
